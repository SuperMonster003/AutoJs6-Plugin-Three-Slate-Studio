"""3-Slate Studio recording check on a device (roadmap P4.5).

Installs the debug plugin and its test APK, grants all files access for the run (appops; the storage permissions before
Android 11; the previous state is restored afterwards), runs ``RecordingDeviceTest#page`` (the sources offered for the
API level) and then ``RecordingDeviceTest#deviceRecording`` once per case: the recording starts through the Studio page,
so the maintainer answers the microphone, notification and screen casting dialogs on the phone (nothing is tapped by
the script; each case waits up to 3 minutes for them). A playback or mixed case plays a 440 Hz tone from the test app.
Each case records, pauses, resumes, optionally turns the screen off (``--screen-off-ms`` for every case, or a case
written as ``microphone@off`` for that case only, 30 s by default), stops, and prints one line with the probe of the
saved file (duration, sample rate, channels, peak level, foreground service). Screenshots of the page while recording,
paused and saved go to ``--shots`` (none while the keyguard shows: it may show other apps' notifications). Afterwards
the recordings and their MediaStore rows are deleted by the test, the test APK is uninstalled, the plugin found before
the run is installed again and checked by MD5 (a plugin that was not installed is uninstalled), and microphone /
notification permissions the run granted are revoked along with the user-set flags of the answered dialogs.

zh-CN: 3-Slate Studio 录音真机检查 (路线图 P4.5). 安装 debug 插件与测试 APK, 运行期间授予所有文件访问 (appops; Android 11 之前为
存储权限; 结束后还原原状态), 运行 ``RecordingDeviceTest#page`` (该 API 级别提供的音源), 然后每个用例运行一次
``RecordingDeviceTest#deviceRecording``: 录音经 Studio 页面开始, 所以麦克风, 通知与投屏对话框由维护者在手机上回答 (脚本不点按;
每个用例最多等 3 分钟). 内录或混合用例由测试应用播放 440 Hz 测试音. 每个用例录音, 暂停, 继续, 可选熄屏 (``--screen-off-ms``
对全部用例, 或把用例写成 ``microphone@off`` 只对该用例, 默认 30 秒), 停止, 并输出一行保存文件的探测结果 (时长, 采样率, 声道,
峰值电平, 前台服务). 录音中, 暂停与保存后的页面截图放到 ``--shots`` (锁屏时不截: 锁屏可能显示其他应用的通知). 结束后测试删除录音
及其 MediaStore 记录, 卸载测试 APK, 重新安装运行前的插件并按 MD5 核对 (运行前未安装时卸载), 并撤销本次运行授予的麦克风 / 通知
权限, 连同回答对话框留下的 user-set 标志.

Usage: py -3.12 .python/device_record_check.py --serial QV770340J7 --cases microphone,playback,microphone@off
       [--screen-off-ms 30000] [--shots DIR] [--plugin-apk A.apk --test-apk B.apk]
"""

from __future__ import annotations

import argparse
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from device_regression import PLUGIN, adb, sdk
from device_wizard_check import backup, restore

ROOT = Path(__file__).resolve().parents[1]
PLUGIN_APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "autojs6-plugin-three-slate-studio-v1.0.0.apk"
TEST_APK = ROOT / "app" / "build" / "outputs" / "apk" / "androidTest" / "debug" / "app-debug-androidTest.apk"
TEST_PACKAGE = PLUGIN + ".test"
TEST_CLASS = PLUGIN + ".record.RecordingDeviceTest"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
PERMISSIONS = ("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS")


def granted(serial: str, permission: str) -> bool:
    dump = adb(serial, "shell", "dumpsys", "package", PLUGIN, check=False)
    return any(line.strip().startswith(f"{permission}: granted=true") for line in dump.splitlines())


def screenshot(serial: str, folder: Path | None, name: str) -> None:
    """Captures the screen unless the keyguard shows. zh-CN: 截屏, 锁屏时跳过."""
    if folder is None:
        return
    if "isKeyguardShowing=true" in adb(serial, "shell", "dumpsys", "window", check=False):
        print(f"  (screenshot {name} skipped: keyguard)", flush=True)
        return
    folder.mkdir(parents=True, exist_ok=True)
    data = subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
    if data:
        (folder / f"{name}.png").write_bytes(data)


def instrument(serial: str, method: str, extras: dict[str, str], timeout: int, on_event=None) -> tuple[bool, dict[str, str], list[str]]:
    """Runs one test method and streams its status lines. zh-CN: 运行一个测试方法并逐行读取其状态."""
    command = ["adb", "-s", serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", f"{TEST_CLASS}#{method}"]
    for key, value in extras.items():
        command += ["-e", key, value]
    command.append(RUNNER)
    values: dict[str, str] = {}
    failures: list[str] = []
    output: list[str] = []
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
    deadline = time.time() + timeout
    assert process.stdout is not None
    for line in process.stdout:
        output.append(line)
        if line.startswith("INSTRUMENTATION_STATUS: record.event="):
            if on_event:
                on_event(line.split("=", 1)[1].strip())
        elif line.startswith("INSTRUMENTATION_STATUS: record."):
            key, _, value = line[len("INSTRUMENTATION_STATUS: "):].partition("=")
            values[key] = value.strip()
        elif line.startswith("INSTRUMENTATION_STATUS: stack="):
            failures.append(line.split("=", 1)[1].strip()[:400])
        if time.time() > deadline:
            process.kill()
            failures.append(f"timed out after {timeout} s")
            break
    process.wait(timeout=30)
    text = "".join(output)
    # -3 ignored, -4 assumption failure (AndroidJUnitRunner). zh-CN: -3 为忽略, -4 为假设不成立.
    skipped = "INSTRUMENTATION_STATUS_CODE: -3" in text or "INSTRUMENTATION_STATUS_CODE: -4" in text
    ok = "OK (" in text and not failures and not skipped
    if skipped:
        failures.append("skipped (assumption failed)")
    if "OK (" not in text and "FAILURES!!!" not in text and not failures:
        failures.append(" ".join(text.split())[-300:])
    return ok, values, failures


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--cases", default="", help="comma separated sources: microphone, playback, mixed, each optionally with @off (default: microphone, plus playback on API 29+)")
    parser.add_argument("--format", default="m4a", choices=("m4a", "wav"))
    parser.add_argument("--record-ms", type=int, default=3000)
    parser.add_argument("--pause-ms", type=int, default=1000)
    parser.add_argument("--screen-off-ms", type=int, default=0)
    parser.add_argument("--after-ms", type=int, default=2000)
    parser.add_argument("--plugin-apk", type=Path, default=PLUGIN_APK)
    parser.add_argument("--test-apk", type=Path, default=TEST_APK)
    parser.add_argument("--shots", type=Path, help="folder for the screenshots of the recording page")
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args(argv)

    api = sdk(args.serial)
    cases = [case for case in args.cases.split(",") if case] or (["microphone", "playback"] if api >= 29 else ["microphone"])
    failures = 0
    status: list[str] = []
    with tempfile.TemporaryDirectory() as temporary:
        saved_plugin = backup(args.serial, PLUGIN, Path(temporary))
        before = {permission: saved_plugin is not None and granted(args.serial, permission) for permission in PERMISSIONS}
        mode = adb(args.serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
        try:
            adb(args.serial, "install", "-r", "-d", "-t", str(args.plugin_apk))
            adb(args.serial, "install", "-r", "-t", str(args.test_apk))
            if api >= 30:
                adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", "allow", check=False)
            else:
                for permission in ("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
                    adb(args.serial, "shell", "pm", "grant", PLUGIN, f"android.permission.{permission}", check=False)
            adb(args.serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
            adb(args.serial, "shell", "wm", "dismiss-keyguard", check=False)
            ok, _, problems = instrument(args.serial, "page", {"grant": "false"}, args.timeout)
            print(f"{'PASS' if ok else 'FAIL'} page API {api}: sources {'microphone, playback, mixed' if api >= 29 else 'microphone only'}" + ("" if ok else f": {problems}"))
            failures += 0 if ok else 1
            for token in cases:
                case, _, flag = token.partition("@")
                screen_off_ms = args.screen_off_ms or (30_000 if flag == "off" else 0)
                extras = {
                    "grant": "false",
                    "recordSource": case,
                    "recordFormat": args.format,
                    "recordMillis": str(args.record_ms),
                    "pauseMillis": str(args.pause_ms),
                    "screenOffMillis": str(screen_off_ms),
                    "afterMillis": str(args.after_ms),
                }
                name = f"{args.serial}-{case}" + ("-screen-off" if screen_off_ms else "")
                print(f"RUN {case}: answer the dialogs on the phone if they appear (microphone, notifications, screen casting)", flush=True)
                ok, values, problems = instrument(args.serial, "deviceRecording", extras, args.timeout,
                                                  on_event=lambda event, base=name: screenshot(args.serial, args.shots, f"{base}-{event}"))
                failures += 0 if ok else 1
                detail = ", ".join(f"{key[len('record.'):]}={value}" for key, value in values.items() if key != "record.path")
                print(f"{'PASS' if ok else 'FAIL'} {token}: {detail}" + ("" if ok else f"; {problems}"), flush=True)
        finally:
            adb(args.serial, "shell", "am", "force-stop", PLUGIN, check=False)
            adb(args.serial, "uninstall", TEST_PACKAGE, check=False)
            adb(args.serial, "shell", "rmdir '/sdcard/Music/3-Slate Studio'", check=False)
            if saved_plugin is not None:
                adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
                for permission, was in before.items():
                    if not was:
                        adb(args.serial, "shell", "pm", "revoke", PLUGIN, permission, check=False)
                        adb(args.serial, "shell", "pm", "clear-permission-flags", PLUGIN, permission, "user-set", "user-fixed", check=False)
            status.append(restore(args.serial, PLUGIN, saved_plugin))
    print("\n".join(status))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
