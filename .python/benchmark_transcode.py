"""Transcodes a 1080p 60 second clip on a device to H.264 and H.265 and reports time and bit rate (roadmap P2.2, appendix F).

`TranscodeBenchmarkTest` (androidTest) generates the source in the cache of the debug plugin (60 s, 30 fps, a detailed
texture whose window jumps every frame, so the encoders run at their target bit rate, as H.264 at 16 Mbps plus a
440 Hz AAC tone), transcodes it through the Binder of `IMediaStudioPlugin` with the default options of each codec,
measures the video bit rate of each output from its samples and checks whether the processing foreground service runs
13 s after the submit; a concat of three copies checks the foreground service once more, is cancelled and the service
has to stop. The files are deleted afterwards. This script installs
the built debug and androidTest APKs with `adb install` (the debug APK stays installed, the test APK is removed again
unless `--keep-test-apk`), runs the test with `benchmark=true` and prints one line per codec.

zh-CN: 在设备上把 1080p 60 秒片段转码为 H.264 与 H.265, 报告耗时与码率 (路线图 P2.2, 附录 F). `TranscodeBenchmarkTest`
(androidTest) 在 debug 插件缓存目录生成源文件 (60 秒, 30 fps, 窗口每帧跳位的高细节纹理, 让编码器按目标码率工作, H.264 16 Mbps,
外加 440 Hz AAC 音调), 经 `IMediaStudioPlugin` 的 Binder 以各编码的默认选项转码, 按样本计算每个输出的视频码率, 并在提交 13 秒后
检查处理前台服务是否在运行; 再用三份拼接检查一次前台服务, 取消后服务须停止. 文件事后删除. 本脚本用 `adb install` 安装已构建的 debug 与 androidTest APK (debug APK 保留, 测试 APK 事后卸载, 除非
`--keep-test-apk`), 以 `benchmark=true` 运行测试, 每个编码输出一行.

Prerequisite: `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. Output: a device line, a source line, one
`PASS` / `FAIL` line per codec, one for the foreground check (`RESTRICTED` when the system restricts the plugin in the background and ignores the
foreground service, which is not a failure) and `BENCHMARK_OK` or `BENCHMARK_ERROR`.
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
TEST_CLASS = f"{PACKAGE}.TranscodeBenchmarkTest"
STATUS_PREFIX = "INSTRUMENTATION_STATUS: benchmark="
TIMEOUT_S = 40 * 60


class BenchmarkError(Exception):
    pass


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    result = subprocess.run(
        [os.environ.get("ADB", "adb"), "-s", serial, *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout,
    )
    if check and result.returncode != 0:
        raise BenchmarkError(f"adb {' '.join(args[:2])} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout


def single_apk(directory: Path) -> Path:
    apks = sorted(directory.glob("*.apk"))
    if len(apks) != 1:
        raise BenchmarkError(f"expected one APK in {directory.relative_to(ROOT)}, found {len(apks)}")
    return apks[0]


def install(serial: str) -> None:
    for apk in (single_apk(ROOT / "app/build/outputs/apk/debug"), single_apk(ROOT / "app/build/outputs/apk/androidTest/debug")):
        output = adb(serial, "install", "-r", "-t", str(apk), check=False)
        if "Success" not in output:
            raise BenchmarkError(f"install {apk.name}: {output.strip().splitlines()[-1][:300] if output.strip() else 'no output'}")


def fields(line: str) -> dict[str, str]:
    return dict(part.split("=", 1) for part in line.split(";") if "=" in part)


def mbps(value: str) -> str:
    return f"{int(value) / 1_000_000:.2f} Mbps"


def run(serial: str) -> list[dict[str, str]]:
    output = adb(serial, "shell", "am", "instrument", "-r", "-w", "-e", "class", TEST_CLASS, "-e", "benchmark", "true", RUNNER, timeout=TIMEOUT_S)
    reports = [fields(line[len(STATUS_PREFIX):].strip()) for line in output.splitlines() if line.startswith(STATUS_PREFIX)]
    codes = [line.split(":", 1)[1].strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS_CODE:")]
    if any(code in ("-1", "-2", "-4") for code in codes) or "INSTRUMENTATION_FAILED" in output:
        stack = [line.strip() for line in output.splitlines() if line.startswith("INSTRUMENTATION_STATUS: stack=") or line.strip().startswith("at ")]
        raise BenchmarkError(f"TranscodeBenchmarkTest did not pass: {' | '.join(stack[:4])[:600]}")
    return reports


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), help="adb serial (default: ANDROID_SERIAL)")
    parser.add_argument("--no-install", action="store_true", help="use the APKs already installed on the device")
    parser.add_argument("--keep-test-apk", action="store_true", help="keep the androidTest APK installed")
    args = parser.parse_args(argv)
    failures = 0
    try:
        if not args.serial:
            raise BenchmarkError("pass --serial or set ANDROID_SERIAL")
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
        source = by_name.get("source")
        if source is None:
            raise BenchmarkError("the test sent no source report (skipped?)")
        print(f"SOURCE 1920x1080 60 s, video {mbps(source['videoBitRate'])}, {int(source['sizeBytes']) / 1e6:.1f} MB, generated in {int(source['generatedMillis']) / 1000:.1f} s")
        encoders = by_name.get("encoders", {}).get("video", "")
        print(f"ENCODERS {encoders or '(none)'}")
        if by_name.get("encoders", {}).get("warnings"):
            print(f"CAPABILITY_WARNINGS {by_name['encoders']['warnings']}")
        for codec in ("h264", "h265"):
            report = by_name.get(codec)
            if report is None:
                print(f"SKIP {codec}: no encoder on this device" if codec not in encoders.split("|") else f"FAIL {codec}: no report")
                failures += codec in encoders.split("|")
                continue
            if report.get("pass") != "true":
                failures += 1
                print(f"FAIL {codec}: {report.get('error', report)}")
                continue
            print(
                f"PASS {codec} {int(report['millis']) / 1000:.1f} s, video {mbps(report['videoBitRate'])}, "
                f"{int(report['sizeBytes']) / 1e6:.1f} MB, {report['size']}, {int(report['durationMillis']) / 1000:.2f} s, "
                f"foreground service: {report['foregroundService']}" + (f", warnings: {report['warnings']}" if report.get("warnings") else "")
            )
        foreground = by_name.get("foreground")
        if foreground is None:
            failures += 1
            print("FAIL foreground: no report")
        else:
            cleanup = foreground["cancelled"] == "TASK_CANCELLED" and not foreground["stoppedAfterCancel"].startswith("still")
            # A background restriction of the system makes startForeground() a no-op; the task still runs.
            # zh-CN: 系统的后台限制让 startForeground() 无效; 任务照常进行.
            restricted = foreground.get("restricted") == "true" and foreground["state"] == "running"
            verdict = "PASS" if cleanup and foreground["state"] == "foreground" else "RESTRICTED" if cleanup and restricted else "FAIL"
            failures += verdict == "FAIL"
            print(
                f"{verdict} foreground (concat x3) at 13 s: {foreground['state']}, background restricted: {foreground.get('restricted')}, "
                f"cancel: {foreground['cancelled']}, service stopped after: {foreground['stoppedAfterCancel']}"
            )
    except (BenchmarkError, subprocess.TimeoutExpired) as error:
        print(f"BENCHMARK_ERROR {error}")
        return 1
    if failures:
        print(f"BENCHMARK_ERROR {failures} codec(s) failed")
        return 1
    print("BENCHMARK_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
