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
| `media-studio-api.aar` | `plugin-api/media-studio-api` | `IMediaStudioPlugin`, `IMediaStudioCallback`, `MediaStudioContract`, `MediaStudioPluginIds`, `MediaStudioPluginActions`, `MediaStudioCapabilityKeys`, `MediaStudioErrorCodes`, `MediaStudioRequestKeys`, `MediaStudioResultKeys`, `MediaStudioProgressKeys`, `MediaStudioLimits`, `MediaStudioOps` | `e408474a4ec85f06a27a362c00dc18b025182dd3f08c5dbccf285639379dc258` |

Provenance (2026-11-05, roadmap P0.1):

- `common-plugin-api.aar` (63,915 B) is the 1.1.1 release build of host commit `1676e7489d` (unsigned), the
  copy 3-Stove Agent stages, byte for byte. It is the line of the new repository rules; 1.1.0 lacks the settings
  transfer purposes `import` / `undo`.
- `media-studio-api.aar` (26,881 B, re-staged 2026-11-07) is the 1.0.0 release build of the module with contract
  version 2 (roadmap appendix D Q36 (b): `subtitlesParse` answers its cues through the file descriptor
  `MediaStudioResultKeys.CUES_FILE`, new keys `CUE_START` / `CUE_END` / `CUE_TEXT`, `MediaStudioLimits.MAX_CUES_FILE_BYTES`).
  The batch 6 card 3-slate-q36-q38 assembled it with `:plugin-api:media-studio-api:assembleRelease` in an isolated
  clone of host `1f877537e6` plus its contract change; the module sources equal its host commit (roadmap appendix C
  names it). It replaces the contract version 1 build of host commit `ddc91edf4f` (roadmap P1.1, 26,749 B, SHA-256
  `66c31dee...a50f5`), which was never published. The AAR does not embed `common-plugin-api` (`PluginInfo` lives
  there), so both are staged. The Maven coordinate `io.github.supermonster003:autojs6-media-studio-api:1.0.0` is
  reserved but not published yet; its first release carries contract version 2.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and run `py .python\generate_third_party_notices.py`.
