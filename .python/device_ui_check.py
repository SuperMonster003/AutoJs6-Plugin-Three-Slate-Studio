"""3-Slate Studio screens on a device (roadmap P4.1).

Installs the debug plugin, then for each appearance (``light`` and ``dark`` in Simplified Chinese, ``ar`` for Arabic
right to left; set through the plugin's own appearance preferences with ``run-as``, so debug builds only) starts the
Studio home from the launcher and walks its pages: library, toolbox, the trim wizard, tasks and settings. A page is
opened by its test tag from ``uiautomator dump`` and only when the tag belongs to the plugin; nothing outside the
plugin is tapped (system dialogs stay with the maintainer). Each page is checked for a running process and no crash,
and with ``--shots`` saved as ``<device>-<appearance>-<page>.png``. A 3 s sample clip (ffmpeg) is put into
``Movies/3-Slate Studio`` and scanned so the library and detail pages have content; the plugin gets all files access
through appops for the run (before Android 11 the storage permissions). Afterwards the sample, its MediaStore row and
the appearance preferences are deleted, the appops mode is restored, and the plugin found before the run is installed
again and checked by MD5 (a plugin that was not installed is uninstalled; ``--keep-plugin`` keeps the debug build).

zh-CN: 3-Slate Studio 真机页面检查 (路线图 P4.1). 安装 debug 插件, 然后按每种外观 (简体中文的 ``light`` 与 ``dark``, 阿拉伯语从右到左
的 ``ar``; 经 ``run-as`` 写插件自己的外观偏好, 仅限 debug 包) 从启动器打开 Studio 首页并依次打开各页: 媒体库, 工具箱, 修剪向导,
任务, 设置. 页面按 ``uiautomator dump`` 中的测试标签打开, 且只点按属于插件的标签; 不点按插件以外的任何内容 (系统对话框由维护者
操作). 每页检查进程在运行且无崩溃, 给出 ``--shots`` 时保存为 ``<设备>-<外观>-<页面>.png``. 用 ffmpeg 生成 3 秒样片放进
``Movies/3-Slate Studio`` 并扫描, 使媒体库与详情页有内容; 运行期间经 appops 授予插件全部文件访问权限 (Android 11 之前为存储权限).
结束后删除样片, 其 MediaStore 记录与外观偏好, 还原 appops 模式, 并重新安装运行前的插件且按 MD5 核对 (运行前未安装时卸载;
``--keep-plugin`` 保留 debug 包).

Usage: py -3.12 .python/device_ui_check.py --serial emulator-5554 [--shots <folder>] [--appearances light,dark,ar]
"""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
import time
from pathlib import Path

from device_regression import DEFAULT_FFMPEG, PLUGIN, adb, ffmpeg, sdk
from device_wizard_check import PLUGIN_APK, backup, restore

LIBRARY = "/sdcard/Movies/3-Slate Studio"
SAMPLE = f"{LIBRARY}/p4-ui-sample.mp4"
STAGING = "/data/local/tmp/slate-ui-check"
PREFS = "shared_prefs/app-appearance.xml"
APPEARANCES = {"light": ("zh-Hans", "light"), "dark": ("zh-Hans", "dark"), "ar": ("ar", "light")}
TAG_NODE = re.compile(r'resource-id="(slate\.[^"]*)"[^>]*?package="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


def sample(binary: str, directory: Path) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / "p4-ui-sample.mp4"
    if not path.exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "testsrc2=size=640x360:rate=30:duration=3", "-f", "lavfi", "-i", "sine=frequency=440:duration=3",
               "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "30", "-c:a", "aac", "-shortest", str(path))
    return path


def tags(serial: str) -> dict[str, tuple[str, int, int]]:
    """The plugin's test tags on screen with their centers. zh-CN: 屏幕上插件的测试标签及其中心点."""
    adb(serial, "shell", "uiautomator", "dump", "/data/local/tmp/slate-ui-check.xml", check=False)
    dump = adb(serial, "shell", "cat", "/data/local/tmp/slate-ui-check.xml", check=False)
    found = {}
    for tag, package, x1, y1, x2, y2 in TAG_NODE.findall(dump):
        found.setdefault(tag, (package, (int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2))
    return found


def tap(serial: str, tag: str, expect: str, attempts: int = 5) -> bool:
    """Taps the plugin's own element ``tag`` until a tag starting with ``expect`` shows the page opened; never taps
    anything of another package.
    zh-CN: 点按插件自己的元素 ``tag``, 直到出现以 ``expect`` 开头的标签 (页面已打开); 不点按其他包的任何内容."""
    for _ in range(attempts):
        found = tags(serial).get(tag)
        if found and found[0] == PLUGIN:
            adb(serial, "shell", "input", "tap", str(found[1]), str(found[2]))
            for _ in range(4):
                time.sleep(1.5)
                if any(name.startswith(expect) and package == PLUGIN for name, (package, _, _) in tags(serial).items()):
                    return True
        else:
            time.sleep(1)
    return False


def back(serial: str, expect: str = "slate.home.navigation", attempts: int = 3) -> bool:
    """Presses BACK until ``expect`` shows (a first BACK may only close the keyboard); stops as soon as it shows, so the
    plugin is never left.
    zh-CN: 按返回键直到出现 ``expect`` (第一次可能只收起键盘); 一出现就停, 不会退出插件."""
    for _ in range(attempts):
        found = tags(serial).get(expect)
        if found and found[0] == PLUGIN:
            return True
        adb(serial, "shell", "input", "keyevent", "KEYCODE_BACK", check=False)
        time.sleep(1.5)
    found = tags(serial).get(expect)
    return bool(found and found[0] == PLUGIN)


def locked(serial: str) -> bool:
    """Whether the keyguard is showing; only the maintainer unlocks a phone. zh-CN: 是否显示锁屏; 手机只由维护者解锁."""
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    time.sleep(1)
    window = adb(serial, "shell", "dumpsys", "window", check=False)
    return "isKeyguardShowing=true" in window or "mDreamingLockscreen=true" in window or "mShowingLockscreen=true" in window


def running(serial: str) -> bool:
    return bool(adb(serial, "shell", "pidof", PLUGIN, check=False).strip())


def foreground(serial: str) -> bool:
    activities = adb(serial, "shell", "dumpsys", "activity", "activities", check=False)
    resumed = [line for line in activities.splitlines() if "mResumedActivity" in line or "ResumedActivity:" in line or "topResumedActivity" in line]
    return any(PLUGIN in line for line in resumed)


def crashes(serial: str, since: str) -> list[str]:
    log = adb(serial, "shell", "logcat", "-b", "crash", "-d", "-t", since, check=False)
    return [line for line in log.splitlines() if f"Process: {PLUGIN}" in line or ("FATAL" in line and PLUGIN in line)]


def appearance(serial: str, language: str, dark_mode: str) -> None:
    # Single-quoted attributes, so the whole document fits in one double-quoted shell word.
    # zh-CN: 属性用单引号, 整个文档放进一个双引号括起的 shell 参数.
    xml = (f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><string name='language'>{language}</string>"
           f"<string name='darkMode'>{dark_mode}</string></map>")
    adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
    adb(serial, "shell", f"run-as {PLUGIN} mkdir -p shared_prefs", check=False)
    adb(serial, "shell", f"echo \"{xml}\" | run-as {PLUGIN} sh -c 'cat > {PREFS}'", check=False)


def launch(serial: str, timeout: float = 40) -> bool:
    """Starts the home from the launcher and waits until its navigation is drawn (a cold start on a software-rendered
    emulator takes several seconds).
    zh-CN: 从启动器打开首页并等到导航栏绘制完成 (软件渲染的模拟器冷启动要好几秒)."""
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "monkey", "-p", PLUGIN, "-c", "android.intent.category.LAUNCHER", "1", check=False)
    deadline = time.time() + timeout
    while time.time() < deadline:
        found = tags(serial).get("slate.home.navigation")
        if found and found[0] == PLUGIN:
            time.sleep(1.5)
            return True
        time.sleep(1.5)
    return False


def shot(serial: str, folder: Path | None, name: str) -> str:
    if folder is None:
        return ""
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / f"{name}.png"
    data = adb_bytes(serial, "exec-out", "screencap", "-p")
    path.write_bytes(data)
    return path.name


def adb_bytes(serial: str, *args: str) -> bytes:
    import subprocess
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, timeout=60).stdout


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--plugin-apk", type=Path, default=PLUGIN_APK)
    parser.add_argument("--appearances", default="light,dark,ar")
    parser.add_argument("--pages", default="library,detail,toolbox,wizard,tasks,settings",
                        help="pages to open after the home (library is the home itself)")
    parser.add_argument("--shots", type=Path, help="save screenshots into this folder")
    parser.add_argument("--name", help="device name in screenshot names (default: the model)")
    parser.add_argument("--ffmpeg", default=DEFAULT_FFMPEG)
    parser.add_argument("--keep-plugin", action="store_true", help="leave the debug plugin installed")
    parser.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "slate-ui-check-sources")
    args = parser.parse_args(argv)

    serial = args.serial
    model = adb(serial, "shell", "getprop", "ro.product.model", check=False).strip()
    if locked(serial):
        print(f"BLOCKED {model} ({serial}): the screen is locked; ask the maintainer to unlock it, nothing was installed")
        return 2
    api = sdk(serial)
    name = args.name or re.sub(r"[^A-Za-z0-9_-]+", "-", model) or serial
    pages = [page for page in args.pages.split(",") if page]
    since = adb(serial, "shell", "date", "+%m-%d %H:%M:%S.000", check=False).strip()
    results: list[tuple[bool, str]] = []
    status: list[str] = []
    with tempfile.TemporaryDirectory() as temporary:
        saved = backup(serial, PLUGIN, Path(temporary))
        mode = adb(serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
        try:
            adb(serial, "install", "-r", "-d", "-t", str(args.plugin_apk))
            adb(serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", "allow", check=False)
            if api < 30:
                for permission in ("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
                    adb(serial, "shell", "pm", "grant", PLUGIN, f"android.permission.{permission}", check=False)
            if "detail" in pages or "library" in pages:
                local = sample(args.ffmpeg, args.cache)
                adb(serial, "shell", f"mkdir -p '{STAGING}' '{LIBRARY}'", check=False)
                adb(serial, "push", str(local), f"{STAGING}/{local.name}")
                adb(serial, "shell", f"cp '{STAGING}/{local.name}' '{SAMPLE}'; rm -rf '{STAGING}'", check=False)
                # Android 10 and later scan through the media provider's scan_file call, older versions through the broadcast.
                # zh-CN: Android 10 起经媒体提供器的 scan_file 调用扫描, 更早的版本经广播.
                adb(serial, "shell", f"content call --uri content://media --method scan_file --arg '{SAMPLE}'", check=False)
                adb(serial, "shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", f"'file://{SAMPLE}'", check=False)
                time.sleep(2)
            for key in [item for item in args.appearances.split(",") if item]:
                language, dark_mode = APPEARANCES[key]
                appearance(serial, language, dark_mode)
                ok = launch(serial) and running(serial) and foreground(serial)
                results.append((ok, f"{key} home {shot(serial, args.shots, f'{name}-{key}-library')}".rstrip()))
                if not ok:
                    continue
                if "detail" in pages:
                    opened = tap(serial, "slate.library.item", "slate.detail")
                    results.append((opened and running(serial), f"{key} detail {shot(serial, args.shots, f'{name}-{key}-detail') if opened else 'no library item'}"))
                    if opened:
                        back(serial)
                if "toolbox" in pages or "wizard" in pages:
                    opened = tap(serial, "slate.home.tab.toolbox", "slate.toolbox")
                    results.append((opened and running(serial), f"{key} toolbox {shot(serial, args.shots, f'{name}-{key}-toolbox') if opened else 'tab not found'}"))
                    if opened and "wizard" in pages:
                        wizard = tap(serial, "slate.toolbox.trim", "slate.wizard")
                        results.append((wizard and running(serial), f"{key} wizard {shot(serial, args.shots, f'{name}-{key}-wizard') if wizard else 'trim not found'}"))
                        if wizard:
                            back(serial)
                if "tasks" in pages:
                    opened = tap(serial, "slate.home.tab.tasks", "slate.tasks")
                    results.append((opened and running(serial), f"{key} tasks {shot(serial, args.shots, f'{name}-{key}-tasks') if opened else 'tab not found, screen ' + shot(serial, args.shots, f'{name}-{key}-tasks-missing')}"))
                if "settings" in pages:
                    opened = tap(serial, "slate.home.settings", "slate.settings")
                    results.append((opened and running(serial), f"{key} settings {shot(serial, args.shots, f'{name}-{key}-settings') if opened else 'action not found'}"))
                adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
        finally:
            adb(serial, "shell", "am", "force-stop", PLUGIN, check=False)
            adb(serial, "shell", f"run-as {PLUGIN} rm -f {PREFS}", check=False)
            adb(serial, "shell", f"rm -f '{SAMPLE}' /data/local/tmp/slate-ui-check.xml; rm -rf '{STAGING}'; rmdir '{LIBRARY}'", check=False)
            adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where", "\"_data LIKE '%/p4-ui-sample%'\"", check=False)
            adb(serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
            found_crashes = crashes(serial, since)
            if not args.keep_plugin:
                status.append(restore(serial, PLUGIN, saved))
    print(f"device {model} API {api} ({serial})")
    for ok, line in results:
        print(f"{'PASS' if ok else 'FAIL'} {line}")
    print(f"{'PASS' if not found_crashes else 'FAIL'} crashes {len(found_crashes)}{': ' + found_crashes[0] if found_crashes else ''}")
    for line in status:
        print(("PASS " if "NOT" not in line else "FAIL ") + line)
    failed = sum(1 for ok, _ in results if not ok) + (1 if found_crashes else 0) + sum(1 for line in status if "NOT" in line)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
