"""3-Slate Studio timeline editor check on a device (roadmap P4.3).

Generates the sources of the 30 s acceptance project on the PC with ffmpeg (three 1280x720 30 fps clips of 12 s with
tones, a 35 s M4A, an SRT of eight cues and a PNG logo), pushes them into the plugin's own external files folder,
grants the plugin all files access through appops for the run (before Android 11 the storage permissions), installs the
debug plugin and its test APK (and with ``--host-apk`` that host build), sets the plugin's appearance (``--appearance``:
``zh`` light Simplified Chinese or ``ar`` Arabic right to left, through ``run-as``), and runs the instrumented
``TimelineDeviceTest``: the project is built partly through the editor's controls, 12 undo / redo steps and the
``.slate.json`` round trip are checked, the preview plays, and the export goes through the export dialog into the task
queue. The equivalent ``video.compose`` script of the result page then runs in AutoJs6 through RunIntentActivity with
``scripts/device-timeline-compare.js`` (rewritten to write next to the editor output), and both outputs are compared by
size, duration (3 % or 60 ms), codecs, sample rate, channels and track count. ``--mode basic`` (the API 24 emulator)
only opens the editor, edits, plays and opens the dialogs. With ``--shots`` the screenshots of the test are pulled
there. Nothing on the phone has to be tapped. Afterwards the pushed files, the outputs and their MediaStore rows and the
appearance preferences are deleted, the appops mode is restored, the test APK is uninstalled, and the host and plugin
found before the run are installed again and checked by MD5 (a plugin that was not installed is uninstalled).

zh-CN: 3-Slate Studio 时间线剪辑器真机检查 (路线图 P4.3). 在 PC 上用 ffmpeg 生成 30 秒验收工程的素材 (三段 1280x720 30 fps 12 秒
带音调的片段, 35 秒 M4A, 8 条 cue 的 SRT, PNG 图标), 推到插件自己的外部文件目录, 运行期间经 appops 授予插件全部文件访问权限
(Android 11 之前为存储权限), 安装 debug 插件与测试 APK (给出 ``--host-apk`` 时也安装该宿主), 设置插件外观 (``--appearance``:
``zh`` 简体中文浅色, ``ar`` 阿拉伯语从右到左, 经 ``run-as``), 运行仪器测试 ``TimelineDeviceTest``: 部分经剪辑器控件搭建工程,
检查 12 步撤销 / 重做与 ``.slate.json`` 往返, 播放预览, 再经导出对话框把导出交给任务队列. 然后经 RunIntentActivity 用
``scripts/device-timeline-compare.js`` 在 AutoJs6 中运行结果页的等价 ``video.compose`` 脚本 (改写为输出到剪辑器输出旁边),
按尺寸, 时长 (3 % 或 60 ms), 编码, 采样率, 声道与轨道数比对两份输出. ``--mode basic`` (API 24 模拟器) 只打开剪辑器, 编辑, 播放
并打开对话框. 给出 ``--shots`` 时把测试截图拉取到该目录. 手机上无需点按. 结束后删除推送的文件, 输出及其 MediaStore 记录与外观
偏好, 还原 appops 模式, 卸载测试 APK, 并重新安装运行前的宿主与插件且按 MD5 核对 (运行前未安装插件时卸载).

Usage: py -3.12 .python/device_timeline_check.py --serial 968e9f18 [--host-apk host-6641.apk] [--appearance zh|ar]
       [--no-export] [--mode project|basic] [--shots <folder>] [--json result.json]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import tempfile
import time
from pathlib import Path

from device_regression import DEFAULT_FFMPEG, HOST, PLUGIN, RUN_INTENT, adb, ffmpeg, launcher, sdk
from device_ui_check import appearance, locked
from device_wizard_check import PLUGIN_APK, TEST_APK, TEST_PACKAGE, backup, device_md5, installed, restore

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "device-timeline-compare.js"
TEST_CLASS = PLUGIN + ".ui.TimelineDeviceTest"
WORK = f"/sdcard/Android/data/{PLUGIN}/files/timeline-check"
REMOTE = "/sdcard/Download/slate-timeline-check"
STAGING = "/data/local/tmp/slate-timeline-check"
PROJECTS = "/sdcard/Movies/3-Slate Studio/Projects"
PREFS = "shared_prefs/app-appearance.xml"
APPEARANCES = {"zh": ("zh-Hans", "light"), "ar": ("ar", "light")}
COMPARED = ("width", "height", "videoCodec", "audioCodec", "sampleRate", "channels", "mime", "trackCount", "trackTypes")


def stamp(millis: int) -> str:
    return f"{millis // 3_600_000:02d}:{millis // 60_000 % 60:02d}:{millis // 1_000 % 60:02d},{millis % 1_000:03d}"


def sources(binary: str, directory: Path) -> dict[str, Path]:
    directory.mkdir(parents=True, exist_ok=True)
    files = {name: directory / name for name in ("clip1.mp4", "clip2.mp4", "clip3.mp4", "music.m4a", "subtitles.srt", "logo.png")}
    patterns = {"clip1.mp4": ("testsrc2", 440), "clip2.mp4": ("smptehdbars", 550), "clip3.mp4": ("testsrc", 660)}
    for name, (pattern, tone) in patterns.items():
        if not files[name].exists():
            ffmpeg(binary, "-f", "lavfi", "-i", f"{pattern}=size=1280x720:rate=30:duration=12", "-f", "lavfi", "-i", f"sine=frequency={tone}:duration=12",
                   "-c:v", "libx264", "-pix_fmt", "yuv420p", "-b:v", "4M", "-g", "30", "-c:a", "aac", "-b:a", "128k", "-shortest", str(files[name]))
    if not files["music.m4a"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "sine=frequency=330:duration=35:sample_rate=44100", "-c:a", "aac", "-b:a", "128k", str(files["music.m4a"]))
    if not files["logo.png"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "testsrc=size=256x256:rate=1:duration=1", "-frames:v", "1", str(files["logo.png"]))
    crlf = chr(13) + chr(10)
    cues = "".join(f"{i + 1}{crlf}{stamp(i * 3_700 + 300)} --> {stamp(i * 3_700 + 3_300)}{crlf}Timeline cue {i + 1}{crlf}{crlf}" for i in range(8))
    files["subtitles.srt"].write_bytes(cues.encode("utf-8"))
    return files


def clean(serial: str) -> None:
    adb(serial, "shell", f"rm -rf '{WORK}' '{REMOTE}' '{STAGING}'", check=False)
    adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where",
        "\"_data LIKE '%/Download/slate-timeline-check%'\"", check=False)
    # The projects folder and the Studio folder are removed only when the run left them empty; MediaStore keeps a row
    # for a removed folder, so the row goes too. zh-CN: 只在运行后为空时删除; 目录删除后 MediaStore 仍留有它的记录, 一并删除.
    for folder in (PROJECTS, "/sdcard/Movies/3-Slate Studio"):
        adb(serial, "shell", f"rmdir '{folder}'", check=False)
        if adb(serial, "shell", f"[ -e '{folder}' ] && echo kept || echo gone", check=False).strip() == "gone":
            adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where",
                f"\"_data LIKE '%/{folder.removeprefix('/sdcard/')}'\"", check=False)


def push_sources(serial: str, files: dict[str, Path]) -> None:
    adb(serial, "shell", f"mkdir -p '{WORK}' '{REMOTE}/editor' '{REMOTE}/script' '{REMOTE}/shots'; chmod 777 '{REMOTE}' '{REMOTE}/editor' '{REMOTE}/script' '{REMOTE}/shots'", check=False)
    # A push straight into the plugin's folder can fail on fchown, so stage it first. zh-CN: 直接推进插件目录可能因 fchown 失败, 先暂存.
    adb(serial, "shell", f"rm -rf '{STAGING}'; mkdir -p '{STAGING}'", check=False)
    for path in files.values():
        adb(serial, "push", str(path), f"{STAGING}/{path.name}")
        adb(serial, "shell", f"cp '{STAGING}/{path.name}' '{WORK}/{path.name}'")
    adb(serial, "shell", f"rm -rf '{STAGING}'; chmod 666 '{WORK}'/*", check=False)


def instrument(serial: str, timeout: int, extra: tuple[str, ...]) -> tuple[bool, list[str]]:
    output = adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS, "-e", "timelineIn", WORK,
                 "-e", "timelineOut", f"{REMOTE}/editor", "-e", "timelineShots", f"{REMOTE}/shots", *extra,
                 f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner", check=False, timeout=timeout)
    lines, test, failed, passed, stacks = [], None, set(), set(), {}
    stack: list[str] | None = None
    for line in output.splitlines():
        if stack is not None and not line.startswith("INSTRUMENTATION_"):
            if len(stack) < 12:
                stack.append(line.strip())
            continue
        stack = None
        if line.startswith("INSTRUMENTATION_STATUS: test="):
            test = line.split("=", 1)[1].strip()
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: ") and test:
            code = int(line.split(": ", 1)[1])
            if code in (-1, -2):
                failed.add(test)
            elif code == 0 and test not in failed:
                passed.add(test)
        elif line.startswith("INSTRUMENTATION_STATUS: stack=") and test:
            stack = [line.split("=", 1)[1].strip()]
            stacks[test] = stack
    # An assumption failure (code -4) is a skipped test, not a failure. zh-CN: 假设失败 (代码 -4) 是跳过, 不是失败.
    lines += [f"FAIL test {name}: {' | '.join(stacks.get(name, []))[:2000]}" for name in sorted(failed)]
    lines += [f"PASS test {name}" for name in sorted(passed)]
    ok = "OK (" in output and not failed
    if "OK (" not in output and "FAILURES!!!" not in output:
        lines.append("FAIL instrumentation: " + " ".join(output.split())[-400:])
    return ok, lines


def run_script(serial: str, code: str, editor: str, timeout: int) -> dict | None:
    script_output = editor.replace("/editor/wizard-", "/script/script-")
    config = {"out": f"{REMOTE}/compare-result.json", "code": code.replace(editor, script_output), "editor": editor, "script": script_output}
    with tempfile.TemporaryDirectory() as temporary:
        local = Path(temporary) / "device-timeline-compare.json"
        local.write_text(json.dumps(config, ensure_ascii=False), encoding="utf-8")
        adb(serial, "push", str(local), f"{REMOTE}/device-timeline-compare.json")
    adb(serial, "push", str(SCRIPT), f"{REMOTE}/device-timeline-compare.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    adb(serial, "shell", "am", "start", "-n", launcher(serial), check=False)
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/device-timeline-compare.js", "-n", RUN_INTENT)
    deadline = time.time() + timeout
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/compare-result.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(3)
    return None


def compare(editor: dict, script: dict) -> str | None:
    for side, value in (("editor", editor), ("script", script)):
        if value.get("missing"):
            return f"no {side} output"
        if value.get("error"):
            return f"{side} measure: {value['error']}"
    for key in COMPARED:
        if editor.get(key) != script.get(key):
            return f"{key} {editor.get(key)} / {script.get(key)}"
    a, b = editor.get("durationMillis") or 0, script.get("durationMillis") or 0
    if abs(a - b) > max(60, 0.03 * max(a, b)):
        return f"duration {a} / {b} ms"
    return None


def describe(value: dict) -> str:
    return (f"{value.get('width')}x{value.get('height')} {value.get('durationMillis')} ms {value.get('videoCodec')} / {value.get('audioCodec')} "
            f"{value.get('sampleRate')} Hz x{value.get('channels')}, {value.get('trackCount')} tracks ({value.get('trackTypes')}), {value.get('sizeBytes')} bytes")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--host-apk", type=Path, help="install this host for the run (the original host is installed again afterwards)")
    parser.add_argument("--plugin-apk", type=Path, default=PLUGIN_APK)
    parser.add_argument("--test-apk", type=Path, default=TEST_APK)
    parser.add_argument("--ffmpeg", default=DEFAULT_FFMPEG)
    parser.add_argument("--timeout", type=int, default=1200)
    parser.add_argument("--appearance", choices=sorted(APPEARANCES), default="zh")
    parser.add_argument("--mode", choices=("project", "basic"), default="project")
    parser.add_argument("--no-export", action="store_true", help="build and check the project without exporting it")
    parser.add_argument("--name", help="screenshot name prefix (default <model>-<appearance>)")
    parser.add_argument("--shots", type=Path, help="pull the screenshots here")
    parser.add_argument("--json", type=Path, help="save the report and the comparison here")
    parser.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "slate-timeline-check-sources")
    args = parser.parse_args(argv)

    if locked(args.serial):
        print(f"BLOCKED {args.serial}: the screen is locked; ask the maintainer to unlock it, nothing was installed")
        return 2
    export = args.mode == "project" and not args.no_export
    model = adb(args.serial, "shell", "getprop", "ro.product.model", check=False).strip().replace(" ", "_") or args.serial
    name = args.name or f"{model}-{args.appearance}"
    files = sources(args.ffmpeg, args.cache)
    status: list[str] = []
    report: dict = {}
    result = None
    ok = False
    with tempfile.TemporaryDirectory() as temporary:
        folder = Path(temporary)
        saved_host = backup(args.serial, HOST, folder) if args.host_apk else None
        saved_plugin = backup(args.serial, PLUGIN, folder)
        mode = adb(args.serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
        try:
            if args.host_apk:
                adb(args.serial, "install", "-r", "-d", str(args.host_apk))
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
            language, dark_mode = APPEARANCES[args.appearance]
            appearance(args.serial, language, dark_mode)
            adb(args.serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
            adb(args.serial, "shell", "wm", "dismiss-keyguard", check=False)
            extra = ("-e", "timelineName", name, "-e", "timelineMode", args.mode, "-e", "timelineExport", "true" if export else "false")
            ok, lines = instrument(args.serial, args.timeout, extra)
            print("\n".join(lines))
            if args.mode == "project":
                raw = adb(args.serial, "shell", "cat", f"{REMOTE}/editor/timeline-report.json", check=False)
                report = json.loads(raw) if raw.startswith("{") else {}
            if export and report.get("state") == "completed":
                result = run_script(args.serial, report["code"], report["output"], args.timeout)
            if args.shots:
                args.shots.mkdir(parents=True, exist_ok=True)
                listed = adb(args.serial, "shell", "ls", f"{REMOTE}/shots", check=False).split()
                for shot in (entry for entry in listed if entry.endswith(".png")):
                    adb(args.serial, "pull", f"{REMOTE}/shots/{shot}", str(args.shots / shot), check=False)
                    status.append(f"screenshot {shot}")
        finally:
            adb(args.serial, "shell", "am", "force-stop", HOST, check=False)
            adb(args.serial, "shell", "am", "force-stop", PLUGIN, check=False)
            saved_project = report.get("savedProject")
            if saved_project:
                adb(args.serial, "shell", f"rm -f '{saved_project}'", check=False)
            adb(args.serial, "shell", f"run-as {PLUGIN} rm -f {PREFS}", check=False)
            adb(args.serial, "shell", f"run-as {PLUGIN} rm -rf files/projects cache/preview-spike", check=False)
            clean(args.serial)
            adb(args.serial, "uninstall", TEST_PACKAGE, check=False)
            adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
            if args.host_apk:
                status.append(restore(args.serial, HOST, saved_host))
            status.append(restore(args.serial, PLUGIN, saved_plugin))
    if args.json:
        args.json.write_text(json.dumps({"report": report, "comparison": result}, indent=2, ensure_ascii=False), encoding="utf-8")
    failures = 0 if ok else 1
    if args.mode == "project":
        if not report:
            print("FAIL no timeline report")
            failures += 1
        else:
            good = report.get("undoSteps") == 12 and report.get("roundTrip") is True and report.get("previewPlayedMs", 0) > 0
            failures += 0 if good else 1
            print(f"{'PASS' if good else 'FAIL'} undo {report.get('undoSteps')} steps, round trip {report.get('roundTrip')}, "
                  f"preview played {report.get('previewPlayedMs', 0):.0f} ms, {report.get('cues')} cues")
        if export and report:
            if report.get("state") != "completed":
                print(f"FAIL export: {report.get('state')} {report.get('errorCode')} {report.get('errorMessage')}")
                failures += 1
            elif result is None:
                print(f"FAIL no script result within {args.timeout} s (see logcat GlobalConsole for [slate-timeline-compare])")
                failures += 1
            else:
                device = result["device"]
                print(f"device {device['model']} API {device['sdk']}, host {device['host']}, plugin {result['plugin']}")
                problem = result.get("error") if not result.get("ok") else compare(result["editor"], result["script"])
                if problem:
                    failures += 1
                    print(f"FAIL compose {problem}")
                else:
                    print(f"PASS compose editor {report.get('exportMs')} ms / script {result['ms']} ms: editor {describe(result['editor'])} / script {describe(result['script'])}")
    for line in status:
        print(("FAIL " if "NOT" in line else "PASS ") + line)
        failures += "NOT" in line
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
