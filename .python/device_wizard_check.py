"""3-Slate Studio wizard check on a device (roadmap P4.2).

Every Studio wizard runs once and its output is compared with the output of the script call the wizard shows:
generates small sources on the PC with ffmpeg (two 640x360 clips of 3 s with a tone, a 5 s M4A tone, a PNG logo and
an SRT file), pushes them into the plugin's own external files folder, grants the plugin all files access through
appops for the run (the previous mode is restored afterwards; before Android 11 the storage permissions are granted
instead), installs the debug plugin and its test APK (and with ``--host-apk`` that host build), runs the instrumented
``WizardDeviceTest`` (each wizard through the Studio queue the way its screen submits it, plus the trim wizard through
the screens), rewrites each reported call to write next to the wizard output, runs the calls through the host's
RunIntentActivity with ``scripts/device-wizard-compare.js`` and prints one line per wizard (PASS / FAIL and the measured
values of both outputs). Needs a host with media studio contract version 2 (6641 built from the host commit
``ca938a9d89`` or later) where 3-Slate Studio is enabled in Plugin Center; nothing on the phone has to be tapped
(the screens test drives the plugin's own screens). Afterwards the pushed files, outputs and their MediaStore rows are
deleted, the test APK is uninstalled, and the host and plugin found before the run are installed again and checked by
MD5 (a plugin that was not installed is uninstalled).

With ``--open-in-autojs6`` (roadmap P4.4; the host must be a debug build of 6641 or later) the instrumented
``OpenInAutoJs6DeviceTest`` then presses "Open in AutoJs6" once on a wizard result page (inline code) and once on a
script over the inline limits (content URI through the plugin's FileProvider); each case waits for the host's
``CREATE_SCRIPT accepted`` and its editor on top, the URI case also finds the MD5 of the sent code in the host's private
copy (run-as). The editors are closed by force-stopping the host, and both private copies are deleted afterwards.

zh-CN: 3-Slate Studio 向导真机检查 (路线图 P4.2). 每个 Studio 向导运行一次, 其输出与向导显示的脚本调用的输出比对: 在 PC 上用 ffmpeg
生成小素材 (两段 640x360 3 秒带音调的片段, 5 秒 M4A 音调, PNG 图标, SRT 文件), 推到插件自己的外部文件目录, 运行期间经 appops
授予插件全部文件访问权限 (结束后还原原模式; Android 11 之前改为授予存储权限), 安装 debug 插件与测试 APK (给出 ``--host-apk`` 时
也安装该宿主), 运行仪器测试 ``WizardDeviceTest`` (每个向导按其页面的提交方式经 Studio 队列运行, 另有修剪向导经页面完整走通),
把报告中的每个调用改写为输出到向导输出旁边, 经宿主 RunIntentActivity 用 ``scripts/device-wizard-compare.js`` 运行, 每个向导输出
一行 (PASS / FAIL 与两份输出的测量值). 需要媒体工坊契约版本 2 的宿主 (由宿主提交 ``ca938a9d89`` 或之后构建的 6641), 且插件中心
已启用 3-Slate Studio; 手机上无需点按 (页面测试操作的是插件自己的页面). 结束后删除推送的文件, 输出及其 MediaStore 记录, 卸载
测试 APK, 并重新安装运行前的宿主与插件且按 MD5 核对 (运行前未安装插件时卸载).

给出 ``--open-in-autojs6`` 时 (路线图 P4.4; 宿主须为 6641 或更新的 debug 构建), 随后运行仪器测试 ``OpenInAutoJs6DeviceTest``:
在向导结果页按一次 "在 AutoJs6 中打开" (内联代码), 对一个超过内联上限的脚本再按一次 (经插件 FileProvider 的 content URI); 每种
情形都等待宿主的 ``CREATE_SCRIPT accepted`` 与其编辑器到前台, URI 情形还在宿主私有副本中找到所发送代码的 MD5 (run-as). 编辑器
经强制停止宿主关闭, 两边的私有副本随后删除.

Usage: py -3.12 .python/device_wizard_check.py --serial QV770340J7 [--host-apk host-6641.apk] [--open-in-autojs6] [--json result.json]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import tempfile
import time
from pathlib import Path

from device_regression import DEFAULT_FFMPEG, HOST, PLUGIN, RUN_INTENT, SRT, adb, ffmpeg, launcher, sdk

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "device-wizard-compare.js"
PLUGIN_APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "autojs6-plugin-three-slate-studio-v1.0.0.apk"
TEST_APK = ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug" / "app-debug-androidTest.apk"
TEST_PACKAGE = PLUGIN + ".test"
TEST_CLASS = PLUGIN + ".ui.WizardDeviceTest"
OPEN_CLASS = PLUGIN + ".ui.OpenInAutoJs6DeviceTest"
OPEN_CASES = ("inline", "contentUri")
WORK = f"/sdcard/Android/data/{PLUGIN}/files/wizard-check"
REMOTE = "/sdcard/Download/slate-wizard-check"
STAGING = "/data/local/tmp/slate-wizard-check"
LIBRARY_FIXTURE = "/sdcard/Movies/3-Slate Studio/p4-wizard-check.mp4"
MEDIA_KINDS = ("video", "audio")


def sources(binary: str, directory: Path) -> dict[str, Path]:
    directory.mkdir(parents=True, exist_ok=True)
    files = {name: directory / name for name in ("video.mp4", "video2.mp4", "audio.m4a", "logo.png", "cues.srt")}
    if not files["video.mp4"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "testsrc2=size=640x360:rate=30:duration=3", "-f", "lavfi", "-i", "sine=frequency=440:duration=3",
               "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "30", "-c:a", "aac", "-shortest", str(files["video.mp4"]))
    if not files["video2.mp4"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "smptebars=size=640x360:rate=30:duration=3", "-f", "lavfi", "-i", "sine=frequency=660:duration=3",
               "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "30", "-c:a", "aac", "-shortest", str(files["video2.mp4"]))
    if not files["audio.m4a"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "sine=frequency=440:duration=5:sample_rate=44100", "-c:a", "aac", "-b:a", "128k", str(files["audio.m4a"]))
    if not files["logo.png"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "color=c=red:size=128x128:duration=1", "-frames:v", "1", str(files["logo.png"]))
    files["cues.srt"].write_bytes(b"\xef\xbb\xbf" + SRT.encode("utf-8"))
    return files


def installed(serial: str, package: str) -> str | None:
    """The base APK path of ``package``, or None. zh-CN: ``package`` 的 base APK 路径, 未安装时为 None."""
    lines = [line[len("package:"):].strip() for line in adb(serial, "shell", "pm", "path", package, check=False).splitlines() if line.startswith("package:")]
    return next((line for line in lines if line.endswith("/base.apk")), lines[0] if lines else None)


def device_md5(serial: str, path: str) -> str:
    return adb(serial, "shell", "md5sum", path, check=False).split(" ")[0].strip()


def backup(serial: str, package: str, folder: Path) -> tuple[Path, str] | None:
    path = installed(serial, package)
    if path is None:
        return None
    local = folder / f"{package}.apk"
    adb(serial, "pull", path, str(local))
    digest = hashlib.md5(local.read_bytes()).hexdigest()
    if digest != device_md5(serial, path):
        raise RuntimeError(f"backup of {package} does not match the device copy")
    return local, digest


def restore(serial: str, package: str, saved: tuple[Path, str] | None) -> str:
    if saved is None:
        adb(serial, "uninstall", package, check=False)
        return f"{package} uninstalled (not installed before)"
    local, digest = saved
    adb(serial, "install", "-r", "-d", str(local), check=False)
    path = installed(serial, package)
    now = device_md5(serial, path) if path else ""
    return f"{package} {'restored' if now == digest else 'NOT restored'} (md5 {digest[:8]}{'' if now == digest else ', device ' + now[:8]})"


def clean(serial: str) -> None:
    adb(serial, "shell", f"rm -rf '{WORK}' '{REMOTE}' '{STAGING}' '{LIBRARY_FIXTURE}'", check=False)
    for pattern in ("%/Download/slate-wizard-check%", "%/Movies/3-Slate Studio/p4-wizard-check%"):
        adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where", f"\"_data LIKE '{pattern}'\"", check=False)
    # The library folder is removed only when the check left it empty. zh-CN: 媒体库目录只在检查后为空时删除.
    adb(serial, "shell", "rmdir '/sdcard/Movies/3-Slate Studio'", check=False)


def push_sources(serial: str, files: dict[str, Path]) -> None:
    adb(serial, "shell", f"mkdir -p '{WORK}' '{REMOTE}/wizard' '{REMOTE}/script'; chmod 777 '{WORK}' '{REMOTE}' '{REMOTE}/wizard' '{REMOTE}/script'", check=False)
    # A push straight into the plugin's folder can fail on fchown (see device_regression.py), so stage it first.
    # zh-CN: 直接推进插件目录可能因 fchown 失败 (见 device_regression.py), 因此先暂存.
    adb(serial, "shell", f"rm -rf '{STAGING}'; mkdir -p '{STAGING}'", check=False)
    for path in files.values():
        adb(serial, "push", str(path), f"{STAGING}/{path.name}")
        adb(serial, "shell", f"cp '{STAGING}/{path.name}' '{WORK}/{path.name}'")
    # The screens test picks a clip from the library dialog, which lists the scanned media of the library folders.
    # zh-CN: 页面测试从媒体库对话框选片段, 对话框列出媒体库目录中已扫描的媒体.
    adb(serial, "shell", f"mkdir -p '{LIBRARY_FIXTURE.rsplit('/', 1)[0]}'; cp '{STAGING}/video.mp4' '{LIBRARY_FIXTURE}'", check=False)
    adb(serial, "shell", f"content call --uri content://media --method scan_file --arg '{LIBRARY_FIXTURE}'", check=False)
    adb(serial, "shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", f"'file://{LIBRARY_FIXTURE}'", check=False)
    adb(serial, "shell", f"rm -rf '{STAGING}'; chmod 666 '{WORK}'/*", check=False)


def instrument(serial: str, timeout: int, test_class: str = TEST_CLASS, extra: tuple[str, ...] = ()) -> tuple[bool, list[str]]:
    output = adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", test_class, "-e", "wizardIn", WORK,
                 "-e", "wizardOut", f"{REMOTE}/wizard", "-e", "wizardLibrary", f"'{LIBRARY_FIXTURE}'", *extra,
                 f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner", check=False, timeout=timeout)
    lines, test, failed = [], None, set()
    for line in output.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: test="):
            test = line.split("=", 1)[1].strip()
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: ") and test:
            code = int(line.split(": ", 1)[1])
            if code in (-1, -2):
                failed.add(test)
            elif code == -3:
                lines.append(f"SKIP test {test}")
            elif code == 0 and test not in failed:
                lines.append(f"PASS test {test}")
        elif line.startswith("INSTRUMENTATION_STATUS: stack=") and test:
            lines.append(f"FAIL test {test}: {line.split('=', 1)[1].strip()[:300]}")
    ok = "OK (" in output and not failed
    if "OK (" not in output and "FAILURES!!!" not in output:
        lines.append("FAIL instrumentation: " + " ".join(output.split())[-300:])
    return ok, lines


def compare(kind: str, wizard: dict, script: dict) -> str | None:
    for side, value in (("wizard", wizard), ("script", script)):
        if value.get("missing"):
            return f"no {side} output"
        if value.get("error"):
            return f"{side} measure: {value['error']}"
    if kind in MEDIA_KINDS:
        for key in ("width", "height", "codec", "videoCodec", "audioCodec", "sampleRate", "channels", "mime"):
            if wizard.get(key) != script.get(key):
                return f"{key} {wizard.get(key)} / {script.get(key)}"
        a, b = wizard.get("durationMillis") or 0, script.get("durationMillis") or 0
        if abs(a - b) > max(60, 0.03 * max(a, b)):
            return f"duration {a} / {b} ms"
        return None
    if kind == "subtitle":
        return None if wizard.get("text") == script.get("text") else "texts differ"
    keys = ("count", "width", "height") if kind == "folder" else ("width", "height")
    for key in keys:
        if wizard.get(key) != script.get(key):
            return f"{key} {wizard.get(key)} / {script.get(key)}"
    return None


def describe(kind: str, value: dict) -> str:
    if kind in MEDIA_KINDS:
        size = f"{value['width']}x{value['height']} " if value.get("width") else ""
        codec = value.get("videoCodec") or value.get("codec") or value.get("audioCodec") or ""
        return f"{size}{value.get('durationMillis')} ms {codec}".strip()
    if kind == "subtitle":
        return f"{len(value.get('text') or '')} chars"
    if kind == "folder":
        return f"{value.get('count')} x {value.get('width')}x{value.get('height')}"
    return f"{value.get('width')}x{value.get('height')}"


def run_script(serial: str, report: list[dict], timeout: int) -> dict | None:
    items = []
    for entry in report:
        if entry["state"] != "completed":
            continue
        script_output = entry["output"].replace("/wizard/wizard-", "/script/script-")
        items.append({
            "op": entry["op"],
            "kind": entry["kind"],
            "code": entry["code"].replace("/wizard/wizard-", "/script/script-"),
            "wizard": entry["output"],
            "script": script_output,
        })
    config = {"out": f"{REMOTE}/compare-result.json", "items": items}
    with tempfile.TemporaryDirectory() as temporary:
        local = Path(temporary) / "device-wizard-compare.json"
        local.write_text(json.dumps(config, ensure_ascii=False), encoding="utf-8")
        adb(serial, "push", str(local), f"{REMOTE}/device-wizard-compare.json")
    adb(serial, "push", str(SCRIPT), f"{REMOTE}/device-wizard-compare.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "am", "start", "-n", launcher(serial), check=False)
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/device-wizard-compare.js", "-n", RUN_INTENT)
    deadline = time.time() + timeout
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/compare-result.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(3)
    return None


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--host-apk", type=Path, help="install this host for the run (the original host is installed again afterwards)")
    parser.add_argument("--plugin-apk", type=Path, default=PLUGIN_APK)
    parser.add_argument("--test-apk", type=Path, default=TEST_APK)
    parser.add_argument("--ffmpeg", default=DEFAULT_FFMPEG)
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--open-in-autojs6", action="store_true", help="also check \"Open in AutoJs6\" inline and by content URI (roadmap P4.4)")
    parser.add_argument("--json", type=Path, help="save the wizard report and the comparison here")
    parser.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "slate-wizard-check-sources")
    args = parser.parse_args(argv)

    from device_ui_check import locked
    if locked(args.serial):
        print(f"BLOCKED {args.serial}: the screen is locked (the screens test needs it); ask the maintainer to unlock it, nothing was installed")
        return 2
    files = sources(args.ffmpeg, args.cache)
    status: list[str] = []
    with tempfile.TemporaryDirectory() as temporary:
        folder = Path(temporary)
        saved_host = backup(args.serial, HOST, folder) if args.host_apk else None
        saved_plugin = backup(args.serial, PLUGIN, folder)
        mode = adb(args.serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
        report, result, ok, opened, open_ok = [], None, False, [], True
        try:
            if args.host_apk:
                adb(args.serial, "install", "-r", "-d", str(args.host_apk))
                # The host under test must be this APK: an install that did not take would run the calls on the old host.
                # zh-CN: 被测宿主必须是这个 APK: 安装未生效时调用会在旧宿主上运行.
                expected = hashlib.md5(args.host_apk.read_bytes()).hexdigest()
                path = installed(args.serial, HOST)
                actual = device_md5(args.serial, path) if path else ""
                status.append(f"host under test {'is' if actual == expected else 'is NOT'} the given APK (md5 {expected[:8]}{'' if actual == expected else ', device ' + actual[:8]})")
                if actual != expected:
                    raise RuntimeError("the host APK was not installed")
            adb(args.serial, "install", "-r", "-d", "-t", str(args.plugin_apk))
            adb(args.serial, "install", "-r", "-t", str(args.test_apk))
            clean(args.serial)
            push_sources(args.serial, files)
            adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", "allow", check=False)
            if sdk(args.serial) < 30:
                for permission in ("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
                    adb(args.serial, "shell", "pm", "grant", PLUGIN, f"android.permission.{permission}", check=False)
            adb(args.serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
            adb(args.serial, "shell", "wm", "dismiss-keyguard", check=False)
            ok, lines = instrument(args.serial, args.timeout)
            print("\n".join(lines))
            raw = adb(args.serial, "shell", "cat", f"{REMOTE}/wizard/wizard-report.json", check=False)
            report = json.loads(raw) if raw.startswith("[") else []
            if report:
                result = run_script(args.serial, report, args.timeout)
            if args.open_in_autojs6:
                adb(args.serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
                open_ok, lines = instrument(args.serial, args.timeout, OPEN_CLASS, ("-e", "openInAutoJs6", "1"))
                print(chr(10).join(lines))
                for case in OPEN_CASES:
                    raw = adb(args.serial, "shell", "cat", f"{REMOTE}/wizard/open-in-autojs6-{case}.json", check=False)
                    opened.append(json.loads(raw) if raw.startswith("{") else {"case": case})
                    # A failed case leaves a screenshot. zh-CN: 失败的情形留有截图.
                    if not raw.startswith("{") and args.json:
                        adb(args.serial, "pull", f"{REMOTE}/wizard/open-in-autojs6-{case}.png", str(args.json.with_name(f"{args.json.stem}-{case}.png")), check=False)
        finally:
            adb(args.serial, "shell", "am", "force-stop", HOST, check=False)
            adb(args.serial, "shell", "am", "force-stop", PLUGIN, check=False)
            if args.open_in_autojs6:
                # The force-stopped editors leave the host's private copy; the plugin's copy waits for a host answer.
                # zh-CN: 被强制停止的编辑器留下宿主私有副本; 插件的副本等待宿主应答后才删除.
                for package in (HOST, PLUGIN):
                    adb(args.serial, "shell", "run-as", package, "rm", "-rf", "cache/create-script", check=False)
            clean(args.serial)
            adb(args.serial, "uninstall", TEST_PACKAGE, check=False)
            adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
            if args.host_apk:
                status.append(restore(args.serial, HOST, saved_host))
            status.append(restore(args.serial, PLUGIN, saved_plugin))
    if args.json:
        args.json.write_text(json.dumps({"report": report, "comparison": result}, indent=2, ensure_ascii=False), encoding="utf-8")
    failures = 0 if ok else 1
    if not report:
        print("FAIL no wizard report")
        failures += 1
    for entry in report:
        if entry["state"] != "completed":
            print(f"FAIL wizard {entry['op']}: {entry['state']} {entry.get('errorCode')} {entry.get('errorMessage')}")
            failures += 1
    if report and result is None:
        print(f"FAIL no script result within {args.timeout} s (see logcat GlobalConsole for [slate-wizard-compare])")
        failures += 1
    elif result is not None:
        device = result["device"]
        print(f"device {device['model']} API {device['sdk']}, host {device['host']}, plugin {result['plugin']}")
        kinds = {entry["op"]: entry["kind"] for entry in report}
        for entry in result["results"]:
            kind = kinds[entry["op"]]
            problem = entry.get("error") if not entry["ok"] else compare(kind, entry["wizard"], entry["script"])
            if problem:
                failures += 1
            detail = problem or f"wizard {describe(kind, entry['wizard'])} / script {describe(kind, entry['script'])}"
            print(f"{'FAIL' if problem else 'PASS'} {entry['op']} {entry['ms']} ms {detail}")
        print(f"{len(result['results']) - sum(1 for e in result['results'] if not e['ok'] or compare(kinds[e['op']], e['wizard'], e['script']))} / {len(report)} wizards match their script call")
    if args.open_in_autojs6:
        failures += 0 if open_ok else 1
        for entry in opened:
            if "md5" not in entry:
                print(f"FAIL open-in-autojs6 {entry['case']}: no summary")
                failures += 1
                continue
            copy = f", host copy {entry['hostCopy']} has md5 {entry['md5'][:8]}" if entry.get("hostCopy") else ""
            print(f"PASS open-in-autojs6 {entry['case']}: {entry['transport']} {entry['chars']} chars / {entry['bytes']} bytes, "
                  f"{entry['suggestedName']} accepted, editor on top{copy}")
    for line in status:
        print(("PASS " if "NOT" not in line else "FAIL ") + line)
        failures += "NOT" in line
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
