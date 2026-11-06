"""Builds the transition samples on a device and puts them in the Movies folder to look at (roadmap P3.2, D30).

`TransitionSampleTest` (androidTest) joins the color bars (2 s) and the reversed color bars (2 s) with every transition
of D30 (1 s; `slide` and `wipe` to the left and upwards) through `compose` over the Binder of `IMediaStudioPlugin`.
This script installs the built debug and androidTest APKs with `adb install` (the test APK is removed again unless
`--keep-test-apk`, the debug APK with `--uninstall`), runs the test with `transitionSamples=true`, copies the nine
samples to `/sdcard/Movies/3-slate-transitions/` and asks the media scanner to index them, so any gallery finds them.
A sample whose warnings report a GPU fallback is printed as `FALLBACK`. `--pull-to` also keeps a copy on the computer.

zh-CN: 在设备上生成转场样本并放进 Movies 目录供目视 (路线图 P3.2, D30). `TransitionSampleTest` (androidTest) 经
`IMediaStudioPlugin` 的 Binder 用 `compose` 以 D30 的每种转场 (1 秒; `slide` 与 `wipe` 各做向左与向上) 连接彩条 (2 秒)
与反序彩条 (2 秒). 本脚本用 `adb install` 安装已构建的 debug 与 androidTest APK (测试 APK 事后卸载, 除非 `--keep-test-apk`;
debug APK 在 `--uninstall` 时卸载), 以 `transitionSamples=true` 运行测试, 把九个样本复制到 `/sdcard/Movies/3-slate-transitions/`
并请媒体扫描器收录, 任何相册都能找到. 警告中报告 GPU 回退的样本输出为 `FALLBACK`. `--pull-to` 另在电脑上保留副本.

Prerequisite: `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. Output: a device line, one `PASS` or
`FALLBACK` line per sample (duration, size, warnings), the folder on the device and `TRANSITIONS_OK` or
`TRANSITIONS_ERROR`. `--remove` deletes the folder on the device again.
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
TEST_CLASS = f"{PACKAGE}.TransitionSampleTest"
STATUS_PREFIX = "INSTRUMENTATION_STATUS: transition="
FILES = tuple(
    f"3-slate-{name}.mp4"
    for name in ("cut", "fade", "dissolve", "slide-left", "slide-up", "wipe-left", "wipe-up", "zoom", "blur")
)
SOURCE_DIR = f"/sdcard/Android/data/{PACKAGE}/files/Movies"
TARGET_DIR = "/sdcard/Movies/3-slate-transitions"
TIMEOUT_S = 10 * 60


class TransitionError(Exception):
    pass


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    result = subprocess.run(
        [os.environ.get("ADB", "adb"), "-s", serial, *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout,
    )
    if check and result.returncode != 0:
        raise TransitionError(f"adb {' '.join(args[:2])} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout


def single_apk(directory: Path) -> Path:
    apks = sorted(directory.glob("*.apk"))
    if len(apks) != 1:
        raise TransitionError(f"expected one APK in {directory.relative_to(ROOT)}, found {len(apks)}")
    return apks[0]


def install(serial: str) -> None:
    for apk in (single_apk(ROOT / "app/build/outputs/apk/debug"), single_apk(ROOT / "app/build/outputs/apk/androidTest/debug")):
        output = adb(serial, "install", "-r", "-t", str(apk), check=False)
        if "Success" not in output:
            raise TransitionError(f"install {apk.name}: {output.strip().splitlines()[-1][:300] if output.strip() else 'no output'}")


def fields(line: str) -> dict[str, str]:
    return dict(part.split("=", 1) for part in line.split(";") if "=" in part)


def run(serial: str) -> list[dict[str, str]]:
    output = adb(serial, "shell", "am", "instrument", "-r", "-w", "-e", "class", TEST_CLASS, "-e", "transitionSamples", "true", RUNNER, timeout=TIMEOUT_S)
    reports = [fields(line[len(STATUS_PREFIX):].strip()) for line in output.splitlines() if line.startswith(STATUS_PREFIX)]
    codes = [line.split(":", 1)[1].strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS_CODE:")]
    if any(code in ("-1", "-2", "-4") for code in codes) or "INSTRUMENTATION_FAILED" in output:
        stack = [line.strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS: stack=") or line.strip().startswith("at ")]
        raise TransitionError(f"TransitionSampleTest did not pass: {' | '.join(stack[:4])[:600]}")
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
    parser.add_argument("--uninstall", action="store_true", help="uninstall the debug plugin afterwards (the copies in Movies stay)")
    parser.add_argument("--pull-to", type=Path, help="also copy the samples to this folder on the computer")
    parser.add_argument("--remove", action="store_true", help="only delete the folder on the device and exit")
    args = parser.parse_args(argv)
    try:
        if not args.serial:
            raise TransitionError("pass --serial or set ANDROID_SERIAL")
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
        for name in FILES:
            report = by_name.get(name)
            if report is None:
                raise TransitionError(f"the test sent no report for {name} (skipped?)")
            warnings = report.get("warnings", "")
            status = "FALLBACK" if "GPU of this device failed" in warnings else "PASS"
            print(
                f"{status} {name} {int(report['durationMillis']) / 1000:.2f} s, {report['size']}"
                + (f", warnings: {warnings}" if warnings else "")
            )
        publish(args.serial)
        if args.pull_to:
            args.pull_to.mkdir(parents=True, exist_ok=True)
            for name in FILES:
                adb(args.serial, "pull", f"{TARGET_DIR}/{name}", str(args.pull_to / name))
        if args.uninstall:
            adb(args.serial, "uninstall", PACKAGE, check=False)
        print(f"FOLDER {TARGET_DIR} ({', '.join(FILES)})")
    except (TransitionError, subprocess.TimeoutExpired) as error:
        print(f"TRANSITIONS_ERROR {error}")
        return 1
    print("TRANSITIONS_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
