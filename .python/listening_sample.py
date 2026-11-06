"""Builds a mix on a device and puts it in the Music folder to listen to (roadmap P2.5, D15).

`ListeningSampleTest` (androidTest) synthesizes a 16 second loop in three parts, each in another format (an arpeggio as
44.1 kHz mono WAV, a bass line entering at 4 s as 22.05 kHz mono FLAC, drums as 48 kHz stereo M4A), mixes them with
`audioMix` to `3-slate-mix.m4a` and applies `audioVolume` (normalize to -1 dBFS, 3 s fade out) to
`3-slate-mix-faded.m4a`, through the Binder of `IMediaStudioPlugin`. This script installs the built debug and
androidTest APKs with `adb install` (the test APK is removed again unless `--keep-test-apk`, the debug APK with
`--uninstall`), runs the test with `listeningSample=true`, copies both files to `/sdcard/Music/3-slate-listening/` and
asks the media scanner to index them, so any music player finds them. `--pull-to` also keeps a copy on the computer.

zh-CN: 在设备上生成混音并放进 Music 目录供试听 (路线图 P2.5, D15). `ListeningSampleTest` (androidTest) 合成 16 秒循环的
三个声部, 各用不同格式 (琶音为 44.1 kHz 单声道 WAV, 4 秒进入的低音为 22.05 kHz 单声道 FLAC, 鼓为 48 kHz 立体声 M4A),
经 `IMediaStudioPlugin` 的 Binder 用 `audioMix` 混成 `3-slate-mix.m4a`, 再用 `audioVolume` (归一化到 -1 dBFS, 淡出 3 秒)
得到 `3-slate-mix-faded.m4a`. 本脚本用 `adb install` 安装已构建的 debug 与 androidTest APK (测试 APK 事后卸载, 除非
`--keep-test-apk`; debug APK 在 `--uninstall` 时卸载), 以 `listeningSample=true` 运行测试, 把两个文件复制到
`/sdcard/Music/3-slate-listening/` 并请媒体扫描器收录, 任何音乐播放器都能找到. `--pull-to` 另在电脑上保留副本.

Prerequisite: `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. Output: a device line, one `PASS` line per
file (duration, codec, sample rate, channels, warnings), the folder on the device and `LISTENING_OK` or
`LISTENING_ERROR`. `--remove` deletes the folder on the device again.
"""
from pathlib import Path
import argparse
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
TEST_PACKAGE = f"{PACKAGE}.test"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = f"{PACKAGE}.ListeningSampleTest"
STATUS_PREFIX = "INSTRUMENTATION_STATUS: listening="
FILES = ("3-slate-mix.m4a", "3-slate-mix-faded.m4a")
SOURCE_DIR = f"/sdcard/Android/data/{PACKAGE}/files/Music"
TARGET_DIR = "/sdcard/Music/3-slate-listening"
TIMEOUT_S = 10 * 60


class ListeningError(Exception):
    pass


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    result = subprocess.run(
        [os.environ.get("ADB", "adb"), "-s", serial, *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout,
    )
    if check and result.returncode != 0:
        raise ListeningError(f"adb {' '.join(args[:2])} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout


def single_apk(directory: Path) -> Path:
    apks = sorted(directory.glob("*.apk"))
    if len(apks) != 1:
        raise ListeningError(f"expected one APK in {directory.relative_to(ROOT)}, found {len(apks)}")
    return apks[0]


def install(serial: str) -> None:
    for apk in (single_apk(ROOT / "app/build/outputs/apk/debug"), single_apk(ROOT / "app/build/outputs/apk/androidTest/debug")):
        output = adb(serial, "install", "-r", "-t", str(apk), check=False)
        if "Success" not in output:
            raise ListeningError(f"install {apk.name}: {output.strip().splitlines()[-1][:300] if output.strip() else 'no output'}")


def fields(line: str) -> dict[str, str]:
    return dict(part.split("=", 1) for part in line.split(";") if "=" in part)


def run(serial: str) -> list[dict[str, str]]:
    output = adb(serial, "shell", "am", "instrument", "-r", "-w", "-e", "class", TEST_CLASS, "-e", "listeningSample", "true", RUNNER, timeout=TIMEOUT_S)
    reports = [fields(line[len(STATUS_PREFIX):].strip()) for line in output.splitlines() if line.startswith(STATUS_PREFIX)]
    codes = [line.split(":", 1)[1].strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS_CODE:")]
    if any(code in ("-1", "-2", "-4") for code in codes) or "INSTRUMENTATION_FAILED" in output:
        stack = [line.strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS: stack=") or line.strip().startswith("at ")]
        raise ListeningError(f"ListeningSampleTest did not pass: {' | '.join(stack[:4])[:600]}")
    return reports


def publish(serial: str) -> None:
    adb(serial, "shell", "mkdir", "-p", TARGET_DIR)
    for name in FILES:
        adb(serial, "shell", "cp", f"{SOURCE_DIR}/{name}", f"{TARGET_DIR}/{name}")
        adb(serial, "shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", f"file://{TARGET_DIR}/{name}", check=False)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), help="adb serial (default: ANDROID_SERIAL)")
    parser.add_argument("--no-install", action="store_true", help="use the APKs already installed on the device")
    parser.add_argument("--keep-test-apk", action="store_true", help="keep the androidTest APK installed")
    parser.add_argument("--uninstall", action="store_true", help="uninstall the debug plugin afterwards (the copies in Music stay)")
    parser.add_argument("--pull-to", type=Path, help="also copy both files to this folder on the computer")
    parser.add_argument("--remove", action="store_true", help="only delete the folder on the device and exit")
    args = parser.parse_args(argv)
    try:
        if not args.serial:
            raise ListeningError("pass --serial or set ANDROID_SERIAL")
        if args.remove:
            adb(args.serial, "shell", "rm", "-rf", TARGET_DIR)
            print(f"REMOVED {TARGET_DIR}")
            return 0
        model = adb(args.serial, "shell", "getprop", "ro.product.model").strip()
        sdk = adb(args.serial, "shell", "getprop", "ro.build.version.sdk").strip()
        print(f"DEVICE {args.serial} {model} API {sdk}")
        if not args.no_install:
            install(args.serial)
            print("PASS install (debug + androidTest)")
        try:
            reports = run(args.serial)
        finally:
            if not args.keep_test_apk:
                adb(args.serial, "uninstall", TEST_PACKAGE, check=False)
        by_name = {report.get("name"): report for report in reports}
        for name in ("mix", "faded"):
            report = by_name.get(name)
            if report is None:
                raise ListeningError(f"the test sent no {name} report (skipped?)")
            print(
                f"PASS {name} {int(report['durationMillis']) / 1000:.2f} s, {report['codec']}, {report['sampleRate']} Hz, "
                f"{report['channels']} ch" + (f", warnings: {report['warnings']}" if report.get("warnings") else "")
            )
        publish(args.serial)
        if args.pull_to:
            args.pull_to.mkdir(parents=True, exist_ok=True)
            for name in FILES:
                adb(args.serial, "pull", f"{TARGET_DIR}/{name}", str(args.pull_to / name))
        if args.uninstall:
            adb(args.serial, "uninstall", PACKAGE, check=False)
        print(f"FOLDER {TARGET_DIR} ({', '.join(FILES)})")
    except (ListeningError, subprocess.TimeoutExpired) as error:
        print(f"LISTENING_ERROR {error}")
        return 1
    print("LISTENING_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
