<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>影片與音訊處理, 多軌時間軸剪輯, 轉場與字幕, 介面操作可產生指令碼</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言 (Languages)

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Slate Studio 是 AutoJs6 的影片與音訊外掛, 也是不安裝 AutoJs6 也能單獨使用的剪輯應用程式. 它可以裁剪, 串接, 變速, 縮放, 旋轉, 裁切影片並加上浮水印, 擷取影格, 轉 GIF 與轉碼; 可以裁剪, 串接, 混音, 淡入淡出與轉換音訊; 多軌時間軸剪輯器把片段與轉場, 疊加和字幕合成為一部影片.

處理在外掛內以 Jetpack Media3 Transformer 與裝置的硬體編解碼器完成, AutoJs6 主程式因此保持精簡. AutoJs6 指令碼透過全域模組 `video` 與 `audio` 處理媒體; 螢幕錄影 (`video.record`) 與錄音 (`audio.record`) 內建於 AutoJs6, 不安裝外掛也能使用. 外掛的每個工具畫面都會顯示等價的指令碼, 可以複製, 也可以在 AutoJs6 中新增為指令碼.

> 目前為開發預覽版: 外掛可以被 AutoJs6 外掛中心識別, 但處理功能, Studio 畫面與 `video` / `audio` 模組的處理方法尚未交付. 下文描述的是各版本的設計目標, 進度請見開發路線圖.

******

### 功能亮點

******

- 影片剪輯: 依時間裁剪, 串接, 變速 (0.25 到 8 倍), 縮放, 旋轉, 裁切與翻轉, 文字或圖片浮水印, 擷取或移除音軌, 轉碼 (預設 H.264, 裝置支援時可選 H.265).
- 影格與 GIF: 單一影格, 影格序列與縮圖, 可儲存為圖片檔或傳回 `Image` 物件; 內建編碼器產生 GIF 動畫.
- 音訊: 裁剪, 串接, 多路混音, 音量調整與淡入淡出, 變速, 轉換為 M4A, WAV, FLAC 或 OGG.
- 多軌時間軸: 影片, 音訊與疊加多軌, 7 種轉場 (切換, 淡入淡出, 交叉溶解, 推移, 擦除, 縮放, 模糊), 字幕支援 SRT 匯入匯出與 ASS 基礎子集, 匯出時燒錄進畫面.
- 程式碼產生: 每個工具畫面與時間軸匯出都會產生等價的 `video.*` / `audio.*` 程式碼, 可直接複製, 也可在 AutoJs6 中新增為指令碼.
- Studio 錄音: 在外掛畫面中錄製麥克風, 裝置內部聲音 (Android 10 以上) 或兩者混合.
- FFmpeg 大型程式庫 (1.1.0): 依需求下載, 提供 MP3, WebM 與 MKV 輸出, `video.ffmpeg` 指令傳遞與完整的 ASS 算繪.

******

### 使用方法

******

1. 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) 下載 APK 並安裝. 所有 ABI 共用一個安裝套件.
2. 開啟外掛, 依提示授予所有檔案存取權限, 以便讀寫你選擇的路徑.
3. 如需在指令碼中使用 `video` 與 `audio` 的處理方法, 請將 AutoJs6 升級到內部版本 6641 (6.8.1) 或更新版本, 並在外掛中心啟用本外掛.

> 外掛支援 Android 7.0 (API 24) 以上的裝置. 唯一的原生程式庫來自 Jetpack Compose, 已依 16 KB 記憶體分頁對齊, 因此所有 ABI 共用一個安裝套件.

******

### 腳本 API

******

指令碼透過全域模組 `video` 與 `audio` (也可寫作 `$video` 與 `$audio`) 使用這些功能 (1.0.0 設計草案, 以發布版本的文件為準):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }], null);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });

let gif = video.toGif(joined.path, { fps: 10, width: 480 });
toastLog(gif.path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }], null);

let recording = video.record(null, { audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- 每個處理方法都有阻塞形式與傳回 Promise 的 `xxxAsync` 形式; `options.onProgress(progress)` 回報 `percent`, `stage` 與 `etaMillis`, 停止指令碼即取消其工作.
- 省略 `output` 時, 影片輸出到 `Movies/3-Slate Studio/`, 音訊輸出到 `Music/3-Slate Studio/`, 檔名由來源檔名, 操作與時間組成; 結果附帶實際路徑與媒體資訊, 並登記到媒體庫.
- `video.compose(timeline)` 算繪以物件描述的多軌時間軸 (軌道, 片段, 轉場, 疊加, 字幕), 與時間軸剪輯器匯出的格式相同.
- `video.tasks()` / `audio.tasks()` 列出並可取消本指令碼進行中的工作; 失敗時擲出 `MediaStudioError`, 其 `code` 為穩定的錯誤碼, 例如 `INVALID_OPTIONS` 或 `STORAGE_PERMISSION_DENIED`.

******

### 常見問題

******

#### 不安裝 AutoJs6 能用嗎?

可以. 工具畫面, 時間軸剪輯器與 Studio 錄音都能單獨使用, 只有在指令碼中處理媒體時才需要 AutoJs6.

#### 為什麼外掛要申請所有檔案存取權限?

指令碼傳入的是 `/sdcard/DCIM/...` 這樣的一般檔案路徑, 外掛與 3-Maple Image 一樣直接讀寫這些路徑. 未授權時處理會回報 `STORAGE_PERMISSION_DENIED`, 外掛畫面會說明如何授權.

#### 螢幕錄影需要安裝外掛嗎?

不需要. `video.record` 與 `audio.record` 內建於 AutoJs6 (內部版本 6641 或更新版本); 外掛提供處理方法與自己的 Studio 錄音.

******

### 權限與安全

******

外掛在設計上盡量縮小權限範圍:

- 受保護的入口: 能力服務與啟動入口都受 AutoJs6 外掛權限 (`org.autojs.permission.PLUGIN`) 保護, 其他應用程式無法直接呼叫.
- 儲存空間: 所有檔案存取權限只用於指令碼與 Studio 畫面指定的路徑; 未指定路徑的輸出只寫入 `Movies` 與 `Music` 下的外掛資料夾.
- 錄音: 只有 Studio 錄音進行中才擷取麥克風與裝置內部聲音, 並顯示前景通知; 擷取裝置內部聲音還需要系統的螢幕擷取確認.
- 網路: 1.0.0 只用於檢查更新; 自 1.1.0 起, FFmpeg 大型程式庫在你要求時下載, 載入前驗證 SHA-256.

******

### FFmpeg 大型程式庫

******

自 1.1.0 起, 外掛可以依需求下載以 LGPL-2.1-or-later 建置的 FFmpeg 大型程式庫. 程式庫不打包進 APK: 只在你要求時依裝置 ABI 下載並動態載入, 隨附自己的授權聲明與對應原始碼的取得位置. 不下載程式庫時, 1.0.0 的全部功能照常可用.

> FFmpeg 支援的部分編解碼器 (例如 H.265 / HEVC 與 AAC) 在某些國家或地區可能受專利保護. 程式庫僅為方便使用而提供, 不附帶任何專利授權; 使用前請確認適用於你的規定.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者, 主程式透過這些識別碼發現外掛並完成能力協商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.slate.studio
plugin id: three-slate-studio
engine: media-studio
variant: default
info service: org.autojs.plugin.INFO (category three-slate-studio)
capability service: org.autojs.plugin.MEDIA_STUDIO (category media-studio)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so of Jetpack Compose only (arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB aligned); a single APK for every ABI
```

******

### 開發路線圖

******

功能規劃, 設計決策與完成情況以可勾選清單維護在 ROADMAP.md 中. 未勾選的項目表示規劃意向, 不代表目前版本的能力.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/11/05_

- `提示` 開發預覽: 外掛可以被 AutoJs6 外掛中心辨識, 處理功能, Studio 畫面與 video / audio 指令碼模組的處理方法尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6641) 或更新版本 (準確的最低版本號待主程式端改動落地後回填)
- `新增` 外掛骨架: Wake 啟用協定, 外掛中心身分服務與媒體處理能力服務 (契約版本 2)
- `新增` 首頁提供所有檔案存取權限的授權入口
- `新增` 所有 ABI 共用一個 APK: 唯一的原生程式庫來自 Jetpack Compose, 已依 16 KB 記憶體分頁對齊
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌, 由 JSON 來源檔統一產生
- `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00 (外掛畫面)
- `相依性` 附加 Navigation Compose 版本 2.10.2
- `相依性` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 與 UI, 用於處理與預覽)
- `相依性` 附加 AndroidX Core KTX 版本 1.18.0

##### 更多發行歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置

******

本節面向希望從原始碼建置外掛的開發者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### 授權

******

專案程式碼採用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). 第三方元件與資料的授權見第三方聲明.

- [第三方聲明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
