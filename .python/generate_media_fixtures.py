"""Generates the media fixtures on a device with MediaCodec and pulls them for manual checks (roadmap P0.3).

The repository carries no binary media: `MediaFixturesTest` (androidTest) encodes 3 seconds of color bars as H.264 MP4
and a 440 Hz sine as AAC M4A and 16 bit WAV into the cache of the debug plugin. This script runs that test with
`keepMediaFixtures=true`, copies the files with `run-as` into an ignored local directory, checks the containers
(WAV header, MP4 `ftyp` / `moov` and the `mvhd` duration) and removes the copies from the device.

zh-CN: 在设备上用 MediaCodec 生成媒体夹具并拉取到本地供人工检查 (路线图 P0.3). 仓库不含二进制媒体: `MediaFixturesTest`
(androidTest) 把 3 秒彩条编码为 H.264 MP4, 把 440 Hz 正弦波编码为 AAC M4A 与 16 位 WAV, 写到 debug 插件的缓存目录. 本脚本以
`keepMediaFixtures=true` 运行该测试, 用 `run-as` 复制到被忽略的本地目录, 检查容器 (WAV 头, MP4 的 `ftyp` / `moov` 与 `mvhd`
时长), 然后删除设备上的副本.

Prerequisite: the debug and androidTest APKs are installed on the device (`--install` runs the Gradle install tasks
for that device only). Output: one `PASS` / `FAIL` line per check and `FIXTURES_OK` or `FIXTURES_ERROR`.
"""
from pathlib import Path
import argparse
import os
import shutil
import struct
import subprocess
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
RUNNER = f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = f"{PACKAGE}.fixtures.MediaFixturesTest"
DEVICE_DIRECTORY = "cache/media-fixtures"
FILES = ("color-bars-3s.mp4", "sine-440hz-3s.m4a", "sine-440hz-3s.wav")
DURATION_S = 3.0
TOLERANCE_S = 0.1
SAMPLE_RATE = 44_100


class FixtureError(Exception):
    pass


def adb(serial: str, *args: str, binary: bool = False, check: bool = True):
    result = subprocess.run([os.environ.get("ADB", "adb"), "-s", serial, *args], capture_output=True)
    if check and result.returncode != 0:
        raise FixtureError(f"adb {' '.join(args[:3])} failed: {result.stderr.decode('utf-8', 'replace').strip()[:300]}")
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")


def install(serial: str) -> None:
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    environment = dict(os.environ, ANDROID_SERIAL=serial)
    result = subprocess.run(
        [str(gradlew), ":app:installDebug", ":app:installDebugAndroidTest", "--console=plain"],
        cwd=ROOT, env=environment, capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if result.returncode != 0:
        lines = (result.stdout + result.stderr).strip().splitlines()
        raise FixtureError("Gradle install failed" + (f": {lines[-1][:300]}" if lines else ""))


def run_test(serial: str) -> None:
    output = adb(serial, "shell", "am", "instrument", "-w", "-e", "class", TEST_CLASS, "-e", "keepMediaFixtures", "true", RUNNER)
    if "OK (1 test)" not in output:
        tail = " | ".join(line.strip() for line in output.strip().splitlines()[-6:])
        raise FixtureError(f"MediaFixturesTest did not pass: {tail[:600]}")


def boxes(data: bytes, start: int = 0, end: int | None = None):
    """Yields (type, payload start, payload end) of the ISO BMFF boxes in data[start:end]."""
    end = len(data) if end is None else end
    offset = start
    while offset + 8 <= end:
        size, kind = struct.unpack(">I4s", data[offset:offset + 8])
        header = 8
        if size == 1:
            size = struct.unpack(">Q", data[offset + 8:offset + 16])[0]
            header = 16
        elif size == 0:
            size = end - offset
        if size < header or offset + size > end:
            raise FixtureError(f"truncated box {kind!r} at {offset}")
        yield kind.decode("latin-1"), offset + header, offset + size
        offset += size


def mp4_duration(path: Path) -> float:
    data = path.read_bytes()
    top = {kind: (begin, finish) for kind, begin, finish in boxes(data)}
    if "ftyp" not in top or "moov" not in top:
        raise FixtureError(f"{path.name}: missing ftyp or moov ({sorted(top)})")
    for kind, begin, finish in boxes(data, *top["moov"]):
        if kind == "mvhd":
            version = data[begin]
            if version == 1:
                timescale, duration = struct.unpack(">IQ", data[begin + 20:begin + 32])
            else:
                timescale, duration = struct.unpack(">II", data[begin + 12:begin + 20])
            return duration / timescale
    raise FixtureError(f"{path.name}: moov has no mvhd")


def wav_duration(path: Path) -> float:
    with wave.open(str(path), "rb") as audio:
        if (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) != (1, 2, SAMPLE_RATE):
            raise FixtureError(f"{path.name}: expected mono 16 bit {SAMPLE_RATE} Hz")
        return audio.getnframes() / audio.getframerate()


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), help="adb serial (default: ANDROID_SERIAL)")
    parser.add_argument("--output", type=Path, default=ROOT / "app" / "build" / "media-fixtures", help="local directory")
    parser.add_argument("--install", action="store_true", help="install the debug and androidTest APKs on --serial first")
    parser.add_argument("--keep-on-device", action="store_true", help="keep the fixtures in the plugin cache")
    args = parser.parse_args(argv)
    failures = 0
    try:
        if not args.serial:
            raise FixtureError("pass --serial or set ANDROID_SERIAL")
        if args.install:
            install(args.serial)
            print("PASS install")
        run_test(args.serial)
        print("PASS MediaFixturesTest (MediaCodec encode + MediaExtractor / MediaMetadataRetriever checks)")
        if args.output.exists():
            shutil.rmtree(args.output)
        args.output.mkdir(parents=True)
        for name in FILES:
            data = adb(args.serial, "exec-out", "run-as", PACKAGE, "cat", f"{DEVICE_DIRECTORY}/{name}", binary=True)
            target = args.output / name
            target.write_bytes(data)
            try:
                duration = wav_duration(target) if name.endswith(".wav") else mp4_duration(target)
                if abs(duration - DURATION_S) > TOLERANCE_S:
                    raise FixtureError(f"{name}: duration {duration:.3f} s")
                print(f"PASS {name} {len(data)} bytes {duration:.3f} s")
            except (FixtureError, wave.Error, struct.error) as error:
                failures += 1
                print(f"FAIL {name}: {error}")
        if not args.keep_on_device:
            adb(args.serial, "shell", "run-as", PACKAGE, "rm", "-r", DEVICE_DIRECTORY, check=False)
    except FixtureError as error:
        print(f"FIXTURES_ERROR {error}")
        return 1
    if failures:
        print(f"FIXTURES_ERROR {failures} fixture(s) failed")
        return 1
    print(f"FIXTURES_OK {len(FILES)} files in {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
