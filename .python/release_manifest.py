"""Writes the official index release manifest draft for one signed release APK (roadmap P0.3, 5.5).

The draft follows `release-manifests/<package>/<versionCode>.json` of AutoJs6-Official-Plugins-Index (schemaVersion 1).
The script checks the tag against version.properties, the APK file name and CRC32 against the build convention
`autojs6-plugin-three-slate-studio-v{VERSION_NAME}-{CRC32}.apk`, the package and version against `aapt2 dump badging`,
and reads the signer digests from `apksigner verify --print-certs`. Registering the draft in the index stays a manual
release step (roadmap P5.4).

zh-CN: 为一个已签名的 release APK 写出官方索引发布清单草稿 (路线图 P0.3, 5.5 节). 草稿格式同
AutoJs6-Official-Plugins-Index 的 `release-manifests/<包名>/<versionCode>.json` (schemaVersion 1). 脚本按 version.properties
核对 tag, 按构建约定核对 APK 文件名与 CRC32, 用 `aapt2 dump badging` 核对包名与版本, 用 `apksigner verify --print-certs`
读取签名摘要. 把草稿登记进索引仍是手动发布步骤 (路线图 P5.4).

Output: `MANIFEST_OK <path> versionCode=<n> artifacts=1` or `MANIFEST_ERROR <reason>` with a non-zero exit code.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
OWNER = "SuperMonster003"
REPOSITORY = "AutoJs6-Plugin-Three-Slate-Studio"
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.slate.studio"
FILE_PREFIX = "autojs6-plugin-three-slate-studio"
ABI_ORDER = ("arm64-v8a", "armeabi-v7a", "x86_64", "x86")


class ManifestError(Exception):
    pass


def version_name(root: Path) -> str:
    text = (root / "version.properties").read_text(encoding="utf-8-sig")
    match = re.search(r"^VERSION_NAME=(.+)$", text, re.M)
    if not match:
        raise ManifestError("version.properties has no VERSION_NAME")
    return match.group(1).strip()


def crc32(path: Path) -> str:
    value = 0
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            value = zlib.crc32(block, value)
    return f"{value & 0xFFFFFFFF:08x}"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def tool(build_tools: Path | None, name: str) -> str:
    candidates = []
    if build_tools:
        candidates.append(build_tools)
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk and (Path(sdk) / "build-tools").is_dir():
        versions = sorted(
            (Path(sdk) / "build-tools").iterdir(),
            key=lambda p: [(0, int(x), "") if x.isdigit() else (1, 0, x) for x in re.split(r"[.-]", p.name)],
        )
        candidates.extend(reversed(versions))
    for directory in candidates:
        for suffix in ("", ".exe", ".bat"):
            path = directory / f"{name}{suffix}"
            if path.is_file():
                return str(path)
    found = shutil.which(name)
    if not found:
        raise ManifestError(f"{name} not found; pass --build-tools or set ANDROID_HOME")
    return found


def run(command: list[str]) -> str:
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise ManifestError(f"{Path(command[0]).name} failed: {(result.stderr or result.stdout).strip()[:400]}")
    return result.stdout


def badging(aapt2: str, apk: Path) -> dict:
    output = run([aapt2, "dump", "badging", str(apk)])
    match = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", output, re.M)
    if not match:
        raise ManifestError("aapt2 badging has no package line")
    return {"packageName": match.group(1), "versionCode": int(match.group(2)), "versionName": match.group(3)}


def signer_digests(apksigner: str, apk: Path) -> list[str]:
    output = run([apksigner, "verify", "--print-certs", str(apk)])
    # Older build-tools print "Signer #1 certificate ...", 35+ print "V2 Signer: certificate ..." per scheme.
    # zh-CN: 旧版 build-tools 输出 "Signer #1 certificate ...", 35 起按签名方案输出 "V2 Signer: certificate ...".
    pattern = r"^(?:Signer #\d+|V\d+(?:\.\d+)? Signer(?: #\d+)?):? certificate SHA-256 digest: ([0-9a-f]{64})\s*$"
    digests = list(dict.fromkeys(re.findall(pattern, output, re.M)))
    if not digests:
        raise ManifestError("apksigner printed no signer digest")
    return digests


def supported_abis(apk: Path) -> list[str]:
    with zipfile.ZipFile(apk) as archive:
        abis = {name.split("/")[1] for name in archive.namelist() if name.startswith("lib/") and name.count("/") >= 2}
    unknown = abis - set(ABI_ORDER)
    if unknown:
        raise ManifestError(f"unexpected ABI directories {sorted(unknown)}")
    # Roadmap D25 / appendix D Q9: one universal APK; the graphics-path library of Compose ships for all four ABIs.
    return [abi for abi in ABI_ORDER if abi in abis] or list(ABI_ORDER)


def build_manifest(apk: Path, tag: str, commit: str, build_tools: Path | None, root: Path) -> dict:
    expected_version = version_name(root)
    if tag != f"v{expected_version}":
        raise ManifestError(f"tag {tag} does not match VERSION_NAME {expected_version}")
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ManifestError("commit must be a full 40 character SHA-1")
    expected_name = f"{FILE_PREFIX}-v{expected_version}-{crc32(apk)}.apk"
    if apk.name != expected_name:
        raise ManifestError(f"APK name {apk.name} does not match {expected_name}")
    package = badging(tool(build_tools, "aapt2"), apk)
    if package["packageName"] != PACKAGE or package["versionName"] != expected_version:
        raise ManifestError(f"APK identity {package} does not match {PACKAGE} {expected_version}")
    return {
        "schemaVersion": 1,
        "owner": OWNER,
        "repository": REPOSITORY,
        "releaseTag": tag,
        "sourceCommit": commit,
        "packageName": PACKAGE,
        "versionName": expected_version,
        "versionCode": package["versionCode"],
        "signerSha256": signer_digests(tool(build_tools, "apksigner"), apk),
        "artifacts": [{"name": apk.name, "sha256": sha256(apk), "sizeBytes": apk.stat().st_size}],
        "supportedAbis": supported_abis(apk),
    }


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True, type=Path, help="the collected release APK under releases/")
    parser.add_argument("--tag", required=True, help="the release tag, v{VERSION_NAME}")
    parser.add_argument("--commit", required=True, help="the full SHA-1 of the tagged commit")
    parser.add_argument("--output-dir", type=Path, default=ROOT / "releases" / "manifest", help="directory of <versionCode>.json")
    parser.add_argument("--build-tools", type=Path, help="Android build-tools directory with aapt2 and apksigner")
    parser.add_argument("--root", type=Path, default=ROOT, help=argparse.SUPPRESS)
    args = parser.parse_args(argv)
    try:
        if not args.apk.is_file():
            raise ManifestError(f"{args.apk} is not a file")
        manifest = build_manifest(args.apk, args.tag, args.commit, args.build_tools, args.root)
        args.output_dir.mkdir(parents=True, exist_ok=True)
        output = args.output_dir / f"{manifest['versionCode']}.json"
        output.write_bytes((json.dumps(manifest, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))
    except ManifestError as error:
        print(f"MANIFEST_ERROR {error}")
        return 1
    print(f"MANIFEST_OK {output} versionCode={manifest['versionCode']} artifacts={len(manifest['artifacts'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
