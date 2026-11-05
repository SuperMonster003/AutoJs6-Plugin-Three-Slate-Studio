# Staged binary artifacts

This directory holds the exact, hash-locked binary artifacts the plugin consumes. Gradle never resolves
them from sibling repositories or from `mavenLocal()`: `app/build.gradle.kts` reads `../locks/host-api-aars.lock`
and fails configuration when a staged file is missing, listed with an invalid digest or different from its
locked SHA-256.

## Host contract AARs (lock `../locks/host-api-aars.lock`)

Release artifacts of the AutoJs6 host, consumed with `implementation` because the plugin runs in its own
process and needs the AIDL stubs and constants at run time.

| File | Host module | Content | SHA-256 |
| --- | --- | --- | --- |
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract`, `PluginSettingsTransferProvider` | `9849b15091dcb255cd1c47140790081eebc074aec2e3e9b28c18f12bf9ef4a6a` |
| `media-studio-api.aar` | `plugin-api/media-studio-api` | `IMediaStudioPlugin`, `IMediaStudioCallback`, `MediaStudioContract`, `MediaStudioPluginIds`, `MediaStudioPluginActions`, `MediaStudioCapabilityKeys`, `MediaStudioErrorCodes`, `MediaStudioRequestKeys`, `MediaStudioResultKeys`, `MediaStudioProgressKeys`, `MediaStudioLimits`, `MediaStudioOps` | `66c31dee7227d9cb7cbd7ce3cd519d69a56d8db52df0404e04ebd9528a1a50f5` |

Provenance (2026-11-05, roadmap P0.1):

- `common-plugin-api.aar` (63,915 B) is the 1.1.1 release build of host commit `1676e7489d` (unsigned), the
  copy 3-Stove Agent stages, byte for byte. It is the line of the new repository rules; 1.1.0 lacks the settings
  transfer purposes `import` / `undo`.
- `media-studio-api.aar` (26,749 B) is the 1.0.0 release build of host commit `ddc91edf4f`, the commit that
  introduces `plugin-api/media-studio-api` (roadmap P1.1, contract version 1). The host card of P1.1 assembled it
  with `:plugin-api:media-studio-api:assemble` and handed it over with its SHA-256; an independent release build of
  the same module sources, made for this repository before the commit landed, is byte-identical. The AAR does not
  embed `common-plugin-api` (`PluginInfo` lives there), so both are staged. The Maven coordinate
  `io.github.supermonster003:autojs6-media-studio-api:1.0.0` is reserved but not published yet.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and run `py .python\generate_third_party_notices.py`.
