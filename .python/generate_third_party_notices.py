# -*- coding: utf-8 -*-
"""Generate THIRD_PARTY_NOTICES.md from the dependency locks (roadmap D39, P0.1).

Sources of truth:
  locks/host-api-aars.lock          staged AutoJs6 host contract AARs (libs/*.aar)
  locks/runtime-dependencies.lock   Maven artifacts of the release runtime classpath, written by
                                    `gradlew :app:writeRuntimeDependencyLock` and enforced by
                                    `:app:verifyRuntimeDependencyLock` (run by `check` and release builds)

Every locked Maven artifact must match one entry of FAMILIES (source and license); an unmatched artifact
fails the generator, so a new dependency cannot ship without a reviewed license entry. NOTICE_TEXTS holds
the NOTICE files the locked artifacts carry (none at the 1.0.0 dependency line) and LICENSE_TEXTS the
license texts that must travel with the APK (MIT for the Checker Framework qualifiers).

Usage:
  py .python/generate_third_party_notices.py            rewrite THIRD_PARTY_NOTICES.md
  py .python/generate_third_party_notices.py --check    verify it matches the locks (CI gate, writes nothing)

Exit protocol: prints NOTICES_OK on success, NOTICES_ERROR <reason> on failure (exit code 1).
"""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

OUTPUT = "THIRD_PARTY_NOTICES.md"
HOST_LOCK = "locks/host-api-aars.lock"
RUNTIME_LOCK = "locks/runtime-dependencies.lock"
HOST_REPOSITORY = "https://github.com/SuperMonster003/AutoJs6"
ANDROIDX_SOURCE = "https://android.googlesource.com/platform/frameworks/support/"
NATIVE_ARTIFACT = "androidx.graphics:graphics-path"
NATIVE_ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64", "x86")


class NoticesError(Exception):
    pass


@dataclass(frozen=True)
class Family:
    title: str
    source: str
    license: str
    matches: Callable[[str, str], bool]


def group_is(*groups: str) -> Callable[[str, str], bool]:
    return lambda group, artifact: group in groups


def group_starts(prefix: str) -> Callable[[str, str], bool]:
    return lambda group, artifact: group.startswith(prefix)


def artifact_starts(group_name: str, prefix: str) -> Callable[[str, str], bool]:
    return lambda group, artifact: group == group_name and artifact.startswith(prefix)


# First match wins. zh-CN: 按顺序匹配, 先匹配者生效.
FAMILIES: tuple[Family, ...] = (
    Family("AndroidX Media3", "https://github.com/androidx/media", "Apache-2.0", group_is("androidx.media3")),
    Family("Jetpack Compose", ANDROIDX_SOURCE, "Apache-2.0", group_starts("androidx.compose.")),
    Family("Other AndroidX libraries", ANDROIDX_SOURCE, "Apache-2.0", group_starts("androidx.")),
    Family("Kotlin standard library", "https://github.com/JetBrains/kotlin", "Apache-2.0", group_is("org.jetbrains.kotlin")),
    Family("Kotlin coroutines", "https://github.com/Kotlin/kotlinx.coroutines", "Apache-2.0",
           artifact_starts("org.jetbrains.kotlinx", "kotlinx-coroutines-")),
    Family("Kotlin serialization", "https://github.com/Kotlin/kotlinx.serialization", "Apache-2.0",
           artifact_starts("org.jetbrains.kotlinx", "kotlinx-serialization-")),
    Family("JetBrains Java annotations", "https://github.com/JetBrains/java-annotations", "Apache-2.0",
           group_is("org.jetbrains")),
    Family("Guava", "https://github.com/google/guava", "Apache-2.0", group_is("com.google.guava")),
    Family("Error Prone annotations", "https://github.com/google/error-prone", "Apache-2.0", group_is("com.google.errorprone")),
    Family("J2ObjC annotations", "https://github.com/google/j2objc", "Apache-2.0", group_is("com.google.j2objc")),
    Family("JSR 305 annotations", "https://github.com/findbugsproject/findbugs", "Apache-2.0",
           group_is("com.google.code.findbugs")),
    Family("JSpecify annotations", "https://github.com/jspecify/jspecify", "Apache-2.0", group_is("org.jspecify")),
    Family("Checker Framework qualifiers", "https://github.com/typetools/checker-framework", "MIT",
           group_is("org.checkerframework")),
    # The theme accent of the Studio screens (roadmap P4.1, standalone settings specification section 6): MaterialKolor's
    # Kotlin port is MIT, its sources keep the Apache-2.0 headers of Google's material-color-utilities.
    # zh-CN: Studio 界面的主题色 (路线图 P4.1, 独立设置规范第 6 节): MaterialKolor 的 Kotlin 移植为 MIT, 源码保留 Google
    # material-color-utilities 的 Apache-2.0 文件头.
    Family("Material Color Utilities, Kotlin port by MaterialKolor", "https://github.com/jordond/materialkolor",
           "MIT (port), Apache-2.0 (code from Google's material-color-utilities)", group_is("com.materialkolor")),
    Family("Poko annotations", "https://github.com/drewhamilton/Poko", "Apache-2.0", group_is("dev.drewhamilton.poko")),
)

# NOTICE files carried by locked artifacts, keyed by `group:artifact` (Apache-2.0 section 4 (d)). The Android build
# drops META-INF NOTICE files from the APK, so they are reproduced here. None at the 1.0.0 dependency line.
# zh-CN: 锁定产物自带的 NOTICE 文件, 键为 `group:artifact` (Apache-2.0 第 4 (d) 条). Android 构建会从 APK 去掉
# META-INF 下的 NOTICE, 因此在此转载. 1.0.0 的依赖线上没有.
NOTICE_TEXTS: dict[str, str] = {}

# License texts that must accompany the binary (`META-INF/LICENSE.txt` of checker-qual 3.43.0, the MIT `LICENSE` of
# MaterialKolor 4.1.1; the Apache-2.0 text of the rest is linked from each row).
# zh-CN: 必须随二进制分发的许可证全文 (checker-qual 3.43.0 的 `META-INF/LICENSE.txt`, MaterialKolor 4.1.1 的 MIT `LICENSE`;
# 其余 Apache-2.0 的全文由各行链接).
LICENSE_TEXTS: dict[str, str] = {
    "com.materialkolor:material-color-utilities-android": """MaterialKolor (`LICENSE` of tag 4.1.1)

MIT License

Copyright (c) 2025 Jordon de Hoog

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.""",
    "org.checkerframework:checker-qual": """Checker Framework qualifiers
Copyright 2004-present by the Checker Framework developers

MIT License:

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.""",
}


def read_lock(root: Path, relative: str) -> dict[str, str]:
    """Same rules as `readLock` in app/build.gradle.kts. zh-CN: 规则同 app/build.gradle.kts 的 `readLock`."""
    path = root / relative
    if not path.is_file():
        raise NoticesError(f"missing {relative}")
    entries: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise NoticesError(f"malformed line in {relative}: {line}")
        key, value = (part.strip() for part in line.split("=", 1))
        if key in entries:
            raise NoticesError(f"duplicate key in {relative}: {key}")
        entries[key] = value
    if entries.pop("format", None) != "1":
        raise NoticesError(f"unsupported lock format in {relative}")
    return entries


def host_artifacts(root: Path) -> list[str]:
    lock = read_lock(root, HOST_LOCK)
    ids = sorted(key[: -len(".file")] for key in lock if key.endswith(".file"))
    if not ids:
        raise NoticesError(f"{HOST_LOCK} lists no artifacts")
    for artifact_id in ids:
        if lock[f"{artifact_id}.file"] != f"{artifact_id}.aar":
            raise NoticesError(f"{HOST_LOCK}: {artifact_id}.file must be {artifact_id}.aar")
    return ids


def runtime_families(root: Path) -> tuple[int, list[tuple[Family, list[tuple[str, str]]]]]:
    lock = read_lock(root, RUNTIME_LOCK)
    grouped: dict[str, list[tuple[str, str]]] = {family.title: [] for family in FAMILIES}
    for coordinate, version in lock.items():
        parts = coordinate.split(":")
        if len(parts) != 2 or not all(parts) or not version:
            raise NoticesError(f"malformed coordinate in {RUNTIME_LOCK}: {coordinate}={version}")
        family = next((candidate for candidate in FAMILIES if candidate.matches(*parts)), None)
        if family is None:
            raise NoticesError(f"no license entry for {coordinate}: add it to FAMILIES after reviewing its license")
        grouped[family.title].append((coordinate, version))
    for key in list(NOTICE_TEXTS) + list(LICENSE_TEXTS):
        if key not in lock:
            raise NoticesError(f"NOTICE_TEXTS / LICENSE_TEXTS name {key}, which {RUNTIME_LOCK} does not lock")
    if NATIVE_ARTIFACT not in lock:
        raise NoticesError(f"{NATIVE_ARTIFACT} is no longer locked: update the native library section and the APK gate")
    families = [(family, sorted(grouped[family.title])) for family in FAMILIES if grouped[family.title]]
    return len(lock), families


def render(root: Path) -> str:
    hosts = host_artifacts(root)
    count, families = runtime_families(root)
    lines = [
        "# Third-party notices",
        "",
        "3-Slate Studio is licensed under the Mozilla Public License 2.0 (see `LICENSE`). The APK bundles the",
        "components below; their licenses apply to those components only.",
        "",
        f"This file is generated by `.python/generate_third_party_notices.py` from `{HOST_LOCK}` and",
        f"`{RUNTIME_LOCK}`; do not edit it by hand. After a dependency change, run",
        "`gradlew :app:writeRuntimeDependencyLock` and then the generator.",
        "",
        "## AutoJs6 host contract libraries",
        "",
        "| Artifact | Source | License |",
        "| --- | --- | --- |",
    ]
    for artifact_id in hosts:
        lines.append(f"| `libs/{artifact_id}.aar` | AutoJs6 host module `plugin-api/{artifact_id}` ({HOST_REPOSITORY}) | MPL-2.0 |")
    lines += [
        "",
        f"Provenance and SHA-256 digests are recorded in `{HOST_LOCK}` and `libs/README.md`.",
        "",
        "## Maven libraries",
        "",
        f"The release runtime classpath packages {count} Maven artifacts (`{RUNTIME_LOCK}`), grouped by project below.",
        "Platform BOMs only align versions and ship nothing.",
    ]
    for family, modules in families:
        lines += [
            "",
            f"### {family.title}",
            "",
            f"Source: {family.source}. License: {family.license}.",
            "",
            "| Artifact | Version |",
            "| --- | --- |",
        ]
        lines += [f"| `{coordinate}` | {version} |" for coordinate, version in modules]
    abis = ", ".join(NATIVE_ABIS[:-1]) + " and " + NATIVE_ABIS[-1]
    lines += [
        "",
        "## Native libraries",
        "",
        "The plugin has no native code of its own and Media3 brings none. Jetpack Compose UI graphics depends on",
        f"`{NATIVE_ARTIFACT}` (Apache-2.0, listed above), which packages `libandroidx.graphics.path.so` for",
        f"{abis}. These four files are the only native libraries in the APK; the release build",
        "checks that they are stored uncompressed and that the 64-bit ones are aligned to 16 KB pages.",
        "",
        "## FFmpeg library (from 1.1.0)",
        "",
        "The FFmpeg library of version 1.1.0 is not part of this APK. It is built under the LGPL-2.1-or-later,",
        "downloaded on request for the device ABI and loaded at run time, and it carries its own notices and the",
        "location of its corresponding source (see the \"FFmpeg library\" section of `.readme/README-en.md`).",
        "",
        "## Notices and license texts",
        "",
        "The full text of the Apache License 2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.",
    ]
    if NOTICE_TEXTS:
        for key, text in sorted(NOTICE_TEXTS.items()):
            lines += ["", f"`{key}` carries the following `NOTICE`:", "", "```text", text, "```"]
    else:
        lines += ["None of the locked artifacts carries a `NOTICE` file."]
    for key, text in sorted(LICENSE_TEXTS.items()):
        lines += ["", f"`{key}` is licensed under the following terms:", "", "```text", text, "```"]
    lines += [
        "",
        "Libraries used only by the debug build (Compose UI tooling) and by the JVM and device tests (JUnit,",
        "AndroidX Test, Espresso, Compose UI test) are not part of the release APK.",
        "",
    ]
    return "\n".join(lines)


def parse_arguments(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate THIRD_PARTY_NOTICES.md from the dependency locks.")
    parser.add_argument("--check", action="store_true", help="verify the file instead of writing it")
    parser.add_argument("--root", type=Path, help="repository root (defaults to the parent of .python)")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    arguments = parse_arguments(argv)
    root = (arguments.root or Path(__file__).resolve().parents[1]).resolve()
    mode = "check" if arguments.check else "write"
    try:
        content = render(root)
        target = root / OUTPUT
        if arguments.check:
            if not target.is_file() or target.read_text(encoding="utf-8") != content:
                raise NoticesError(f"{OUTPUT} is stale: run py .python/generate_third_party_notices.py")
        else:
            target.write_text(content, encoding="utf-8", newline="\n")
    except NoticesError as error:
        print(f"NOTICES_ERROR {error}")
        return 1
    hosts = content.count("| `libs/")
    print(f"NOTICES_OK host={hosts} maven={content.count('| `') - hosts} mode={mode}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
