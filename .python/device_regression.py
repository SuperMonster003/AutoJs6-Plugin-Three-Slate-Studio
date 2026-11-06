"""3-Slate Studio device regression driver (roadmap P2.6).

Generates the sources on the PC with ffmpeg (a 1920x1080 30 fps clip of 60 s with a 440 Hz tone (the length of the
appendix F baseline), a 640x360 clip of
3 s, a 5 s tone in M4A, a PNG logo and an SRT file), pushes them into the plugin's own external files folder, grants
the plugin all files access through appops for the run (the outputs go to ``/sdcard/Download/slate-regression`` and
``video.frame`` keeps its temporary image in the public Movies folder; the previous mode is restored afterwards; before
Android 11 the storage permissions are granted instead), pushes ``scripts/device-regression.js`` with its configuration, runs it through the
host's RunIntentActivity and prints one line per operation (PASS / FAIL, milliseconds, value), the video encoders and
the 1080p transcode speed. Needs the AutoJs6 host with the media studio modules (build 6641 or later) and the plugin
installed; nothing on the phone has to be tapped. Afterwards the pushed files and the outputs are deleted (``--keep``
keeps them).

zh-CN: 3-Slate Studio 真机回归驱动 (路线图 P2.6). 在 PC 上用 ffmpeg 生成素材 (1920x1080 30 fps 60 秒带 440 Hz 音调的片段 (附录 F
基线的长度),
640x360 3 秒片段, 5 秒 M4A 音调, PNG 图标, SRT 文件), 推到插件自己的外部文件目录, 运行期间经 appops 授予插件全部文件访问权限 (输出在 ``/sdcard/Download/slate-regression``,
``video.frame`` 的临时图片在公共 Movies 目录; 结束后还原原模式; Android 11 之前改为授予存储权限), 推送
``scripts/device-regression.js`` 及其配置, 经宿主 RunIntentActivity 运行, 每个操作输出一行 (PASS / FAIL, 毫秒, 数值), 以及视频
编码器与 1080p 转码倍速. 需要含媒体工坊模块的 AutoJs6 宿主 (构建 6641 或更新) 并已安装插件; 手机上无需点按. 结束后删除推送的
文件与输出 (``--keep`` 保留).

Usage: py -3.12 .python/device_regression.py --serial QV770340J7 [--extreme] [--json result.json]
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "device-regression.js"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
HOST = "org.autojs.autojs6"
RUN_INTENT = f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity"
WORK = f"/sdcard/Android/data/{PLUGIN}/files/regression"
REMOTE = "/sdcard/Download/slate-regression"
STAGING = "/data/local/tmp/slate-regression"
DEFAULT_FFMPEG = r"D:\Program Files\MeGUI_2418_x86\tools\ffmpeg\ffmpeg.exe"

SRT = (
    "1\r\n00:00:00,200 --> 00:00:01,500\r\n<b>3-Slate</b> regression\r\n\r\n"
    "2\r\n00:00:01,000 --> 00:00:02,500\r\n{\\an8}top line\r\n\r\n"
    "3\r\n00:00:02,000 --> 00:00:03,000\r\nlast <i>cue</i>\r\n"
)


def adb(serial: str, *args: str, check: bool = True, timeout: int = 300) -> str:
    env = dict(os.environ, MSYS_NO_PATHCONV="1")
    done = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout, env=env)
    if check and done.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {done.stderr.strip() or done.stdout.strip()}")
    return done.stdout


def ffmpeg(binary: str, *args: str) -> None:
    subprocess.run([binary, "-hide_banner", "-loglevel", "error", "-y", *args], check=True)


def sources(binary: str, directory: Path) -> dict[str, Path]:
    files = {
        "video": directory / "src-1080p-60s.mp4",
        "video2": directory / "src-360p.mp4",
        "audio": directory / "tone.m4a",
        "logo": directory / "logo.png",
        "srt": directory / "cues.srt",
    }
    if not files["video"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "testsrc2=size=1920x1080:rate=30:duration=60", "-f", "lavfi", "-i", "sine=frequency=440:duration=60",
               "-c:v", "libx264", "-pix_fmt", "yuv420p", "-b:v", "8M", "-g", "60", "-c:a", "aac", "-b:a", "128k", "-shortest", str(files["video"]))
    if not files["video2"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "testsrc2=size=640x360:rate=30:duration=3", "-f", "lavfi", "-i", "sine=frequency=660:duration=3",
               "-c:v", "libx264", "-pix_fmt", "yuv420p", "-g", "30", "-c:a", "aac", "-shortest", str(files["video2"]))
    if not files["audio"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "sine=frequency=440:duration=5:sample_rate=44100", "-c:a", "aac", "-b:a", "128k", str(files["audio"]))
    if not files["logo"].exists():
        ffmpeg(binary, "-f", "lavfi", "-i", "color=c=red:size=128x128:duration=1", "-frames:v", "1", str(files["logo"]))
    files["srt"].write_bytes(b"\xef\xbb\xbf" + SRT.encode("utf-8"))
    return files


def clean(serial: str) -> None:
    adb(serial, "shell", f"rm -rf '{WORK}' '{REMOTE}' '{STAGING}'", check=False)
    # Deleted folders keep their MediaStore rows (and before Android 11 deleted files too).
    # zh-CN: 删除的文件夹仍留有 MediaStore 记录 (Android 11 之前删除的文件也是).
    # adb joins the arguments with spaces, so the condition is quoted for the device shell.
    # zh-CN: adb 用空格拼接参数, 因此条件要为设备上的 shell 加引号.
    adb(serial, "shell", "content", "delete", "--uri", "content://media/external/file", "--where",
        "\"_data LIKE '%/Download/slate-regression%'\"", check=False)


def sdk(serial: str) -> int:
    return int(adb(serial, "shell", "getprop", "ro.build.version.sdk", check=False).strip() or 0)


def launcher(serial: str) -> str:
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", HOST)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(HOST + "/") and "leakcanary" not in line]
    return own[0] if own else f"{HOST}/org.autojs.autojs.ui.splash.SplashActivity"


def run(serial: str, files: dict[str, Path], extreme: bool, timeout: int) -> dict | None:
    clean(serial)
    adb(serial, "shell", f"mkdir -p '{WORK}' '{REMOTE}'", check=False)
    # A folder shell creates there may be shell-owned with mode 2770 (Redmi 22120RN86C), so open it for the plugin.
    # zh-CN: shell 在此创建的目录可能属于 shell 且权限 2770 (Redmi 22120RN86C), 因此对插件开放.
    adb(serial, "shell", "chmod", "777", WORK, check=False)
    remote = {}
    # A push straight into the plugin's folder fails on some devices when adb sets the owner (XQ-DQ72, Android 13:
    # "remote fchown() failed"), so stage the files in /data/local/tmp and copy them with the shell.
    # zh-CN: 有的设备直接推进插件目录会在 adb 设置属主时失败 (XQ-DQ72, Android 13: "remote fchown() failed"), 因此先放
    # /data/local/tmp, 再由 shell 复制.
    adb(serial, "shell", f"rm -rf '{STAGING}'; mkdir -p '{STAGING}'", check=False)
    for key, path in files.items():
        remote[key] = f"{WORK}/{path.name}"
        adb(serial, "push", str(path), f"{STAGING}/{path.name}")
        adb(serial, "shell", f"cp '{STAGING}/{path.name}' '{remote[key]}'")
    adb(serial, "shell", f"rm -rf '{STAGING}'", check=False)
    adb(serial, "shell", f"chmod 666 '{WORK}'/*", check=False)
    # Outputs go to the public folder: files the plugin writes into its own folder belong to it, and the shell of some
    # devices cannot delete them (Xiaomi 23046RP50C, Android 15; run-as cannot reach /storage either).
    # zh-CN: 输出放公共目录: 插件写进自己目录的文件属于插件, 有的设备 shell 删不掉 (Xiaomi 23046RP50C, Android 15; run-as 也进不了 /storage).
    config = dict(remote, work=f"{REMOTE}/out-{int(time.time())}", out=f"{REMOTE}/result.json", extreme=extreme)
    with tempfile.TemporaryDirectory() as temporary:
        local = Path(temporary) / "device-regression.json"
        local.write_text(json.dumps(config), encoding="utf-8")
        adb(serial, "push", str(local), f"{REMOTE}/device-regression.json")
    adb(serial, "push", str(SCRIPT), f"{REMOTE}/device-regression.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP", check=False)
    # The outputs and the temporary image of video.frame are in public folders, so grant all files access for the run.
    # zh-CN: 输出与 video.frame 的临时图片都在公共目录, 因此运行期间授予全部文件访问权限.
    adb(serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", "allow", check=False)
    if sdk(serial) < 30:
        # Before Android 11 the same needs the storage permissions (granted through pm, the plugin keeps them).
        # zh-CN: Android 11 之前需要存储权限 (经 pm 授予, 插件保留).
        for permission in ("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
            adb(serial, "shell", "pm", "grant", PLUGIN, f"android.permission.{permission}", check=False)
    adb(serial, "shell", "am", "start", "-n", launcher(serial), check=False)
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/device-regression.js", "-n", RUN_INTENT)
    deadline = time.time() + timeout
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/result.json", check=False)
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(3)
    return None


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--ffmpeg", default=shutil.which("ffmpeg") or DEFAULT_FFMPEG)
    parser.add_argument("--extreme", action="store_true",
                        help="also run 8 video + 16 audio tracks (appendix D Q34 (c)); on failure, fewer tracks until one passes")
    parser.add_argument("--timeout", type=int, default=1200)
    parser.add_argument("--json", type=Path, help="save the result JSON here")
    parser.add_argument("--cache", type=Path, default=Path(tempfile.gettempdir()) / "slate-regression-sources")
    parser.add_argument("--keep", action="store_true", help="keep the pushed files and outputs on the device")
    args = parser.parse_args(argv)
    # Error messages may be in the host language; keep them readable when the output goes to a file.
    # zh-CN: 错误消息可能是宿主语言, 输出重定向到文件时仍保持可读.
    sys.stdout.reconfigure(encoding="utf-8")
    args.cache.mkdir(parents=True, exist_ok=True)
    files = sources(args.ffmpeg, args.cache)
    mode = adb(args.serial, "shell", "appops", "get", PLUGIN, "MANAGE_EXTERNAL_STORAGE", check=False).partition(":")[2].split(";")[0].strip() or "default"
    try:
        result = run(args.serial, files, args.extreme, args.timeout)
    finally:
        adb(args.serial, "shell", "appops", "set", PLUGIN, "MANAGE_EXTERNAL_STORAGE", mode if mode in ("allow", "ignore", "deny", "default") else "default", check=False)
        if not args.keep:
            clean(args.serial)
    if result is None:
        print(f"FAIL no result within {args.timeout} s (see logcat GlobalConsole for [slate-regression])")
        return 2
    if args.json:
        args.json.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    device = result["device"]
    print(f"device {device['model']} API {device['sdk']}, host {device['host']}, plugin {result['plugin']}")
    if not result["plugin"].get("available"):
        print(f"FAIL plugin unavailable, nothing was run: {result['plugin'].get('error', '')}")
        return 2
    for entry in result["results"]:
        detail = entry.get("value") if entry["ok"] else entry.get("error")
        print(f"{'PASS' if entry['ok'] else 'FAIL'} {entry['name']} {entry['ms']} ms {detail or ''}".rstrip())
    for codec in result["encoders"]:
        print(f"encoder {codec['type']} {codec['name']} {'hardware' if codec['hardware'] else 'software'}")
    if result.get("extremeMax") is not None:
        print(f"extreme largest passing timeline: {result['extremeMax'] or 'none'}")
    for key in ("speedH264", "speedH265"):
        if result.get(key):
            print(f"{key} {result[key]:.2f}x ({result['sourceDurationMillis']} ms source)")
    failed = [entry for entry in result["results"] if not entry["ok"]]
    print(f"{len(result['results']) - len(failed)} / {len(result['results'])} PASS")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
