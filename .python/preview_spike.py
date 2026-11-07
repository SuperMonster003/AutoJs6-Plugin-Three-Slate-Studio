"""Runs the timeline preview spike on one device (roadmap P4.3, appendix D Q3, D34).

`PreviewSpikeTest` (androidTest) plays timelines with media3 `CompositionPlayer` into an ImageReader and reports, per
scenario, the output frame rate, frame gaps, dropped frames, frame lateness against the audio clock, A/V offset, time
to the first frame, extra wall time and seek latency. This script backs up the installed plugin APK (if any), installs
the built debug and androidTest APKs, runs the test with `previewSpike=true`, prints one `SPIKE` line per scenario,
deletes the generated sources on the device, removes the test APK and puts the original plugin back (md5 checked), or
uninstalls the plugin when it was not installed before.

zh-CN: 在一台设备上运行时间线预览 spike (路线图 P4.3, 附录 D Q3, D34). `PreviewSpikeTest` (androidTest) 用 media3
`CompositionPlayer` 把时间线播放到 ImageReader, 按场景报告输出帧率, 帧间隔, 丢帧, 相对音频时钟的帧迟到, 音画偏差, 首帧耗时,
多用的墙钟时间与跳转延迟. 本脚本备份已安装的插件 APK (如有), 安装已构建的 debug 与 androidTest APK, 以 `previewSpike=true`
运行测试, 每个场景输出一行 `SPIKE`, 删除设备上生成的素材, 卸载测试 APK 并装回原插件 (核对 md5); 测前未安装时卸载插件.

Prerequisite: `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`. Output: a device line, `SPIKE` lines and
`SPIKE_OK` or `SPIKE_ERROR`.
"""
from pathlib import Path
import argparse
import hashlib
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
TEST_PACKAGE = f"{PACKAGE}.test"
RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = f"{PACKAGE}.preview.PreviewSpikeTest"
STATUS_PREFIX = "INSTRUMENTATION_STATUS: spike="
TIMEOUT_S = 20 * 60


class SpikeError(Exception):
    pass


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    result = subprocess.run(
        [os.environ.get("ADB", "adb"), "-s", serial, *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout,
    )
    if check and result.returncode != 0:
        raise SpikeError(f"adb {' '.join(args[:2])} failed: {(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout


def single_apk(directory: Path) -> Path:
    apks = sorted(directory.glob("*.apk"))
    if len(apks) != 1:
        raise SpikeError(f"expected one APK in {directory.relative_to(ROOT)}, found {len(apks)}")
    return apks[0]


def md5(path: Path) -> str:
    return hashlib.md5(path.read_bytes()).hexdigest()


def installed_apk(serial: str) -> str | None:
    out = adb(serial, "shell", "pm", "path", PACKAGE, check=False)
    paths = [line.split(":", 1)[1].strip() for line in out.splitlines() if line.startswith("package:")]
    return next((p for p in paths if p.endswith("base.apk")), None)


def install(serial: str, apk: Path) -> None:
    out = adb(serial, "install", "-r", "-d", str(apk), check=False, timeout=600)
    if "Success" not in out:
        raise SpikeError(f"install {apk.name} failed: {out.strip()[:300]}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--scenarios", default="main,pip")
    args = parser.parse_args()
    serial = args.serial
    debug = single_apk(ROOT / "app/build/outputs/apk/debug")
    test = single_apk(ROOT / "app/build/outputs/apk/androidTest/debug")
    model = adb(serial, "shell", "getprop", "ro.product.model").strip()
    sdk = adb(serial, "shell", "getprop", "ro.build.version.sdk").strip()
    print(f"DEVICE {serial} {model} API {sdk}")
    backup_dir = Path(tempfile.mkdtemp(prefix="slate-spike-"))
    original = installed_apk(serial)
    backup = None
    if original:
        backup = backup_dir / "original.apk"
        adb(serial, "pull", original, str(backup))
        print(f"BACKUP plugin md5 {md5(backup)[:8]}")
    ok = False
    try:
        install(serial, debug)
        install(serial, test)
        out = adb(serial, "shell", "am", "instrument", "-w", "-r", "-e", "previewSpike", "true", "-e", "scenarios", args.scenarios,
                  "-e", "class", TEST_CLASS, RUNNER, check=False, timeout=TIMEOUT_S)
        lines = [line[len(STATUS_PREFIX):].strip() for line in out.splitlines() if line.startswith(STATUS_PREFIX)]
        for line in lines:
            print(f"SPIKE {line}")
        failed = "FAILURES!!!" in out or "INSTRUMENTATION_FAILED" in out or "Process crashed" in out
        if failed:
            start = out.find("INSTRUMENTATION_STATUS: stack=")
            stack = out[start:start + 2500] if start >= 0 else out[-1500:]
            print("TEST_FAILURE " + " | ".join(line.strip() for line in stack.splitlines()[:25]))
        ok = bool(lines) and not failed and not any("error=" in line for line in lines)
    finally:
        adb(serial, "shell", "run-as", PACKAGE, "rm", "-rf", "cache/preview-spike", check=False)
        adb(serial, "uninstall", TEST_PACKAGE, check=False)
        if backup is not None:
            install(serial, backup)
            restored = installed_apk(serial)
            check = backup_dir / "restored.apk"
            if restored:
                adb(serial, "pull", restored, str(check))
            same = check.is_file() and md5(check) == md5(backup)
            print(f"RESTORE plugin md5 {'match' if same else 'MISMATCH'}")
            ok = ok and same
        else:
            adb(serial, "uninstall", PACKAGE, check=False)
            print("RESTORE plugin uninstalled (not installed before)")
        for file in backup_dir.glob("*"):
            file.unlink()
        backup_dir.rmdir()
    print("SPIKE_OK" if ok else "SPIKE_ERROR")
    return 0 if ok else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except SpikeError as e:
        print(f"SPIKE_ERROR {e}")
        sys.exit(1)
