******

### 發行歷史

******

# v1.0.0

###### 2026/11/08

* `提示` 在指令碼中使用 video / audio 的處理方法需要 AutoJs6 6.8.1 (內部版本號 6641) 或更新版本; 外掛畫面, 時間軸剪輯器與 Studio 錄音不依賴 AutoJs6
* `提示` 指令碼 API 請參閱 AutoJs6 文件的 [video](https://docs.autojs6.com/#/video) 與 [audio](https://docs.autojs6.com/#/audio) 章節
* `新增` 影片處理: 裁剪, 串接, 變速 (0.25 - 8 倍), 縮放, 旋轉, 裁切與翻轉, 文字或圖片浮水印, 擷取或移除音軌, 轉碼 (H.264, 裝置支援時可選 H.265)
* `新增` 影格, 縮圖與 GIF: 儲存為 PNG / JPG 檔案或傳回 Image 物件, 內建 GIF 編碼器 (調色盤, 遞色, 循環)
* `新增` 音訊處理: 裁剪, 串接, 混音 (會削波時自動縮放), 音量與淡入淡出, 變速, 轉換為 M4A / WAV / FLAC / OGG (OGG 需要 Android 10+)
* `新增` 多軌合成 video.compose: 最多 8 條影片軌與 16 條音訊軌, 7 種轉場, 文字 / 圖片疊加與字幕
* `新增` 字幕: SRT 讀寫與 ASS 基礎子集, 可用 video.burnSubtitles 燒錄進畫面, 用 video.subtitles.parse / write 讀寫字幕檔案
* `新增` AutoJs6 指令碼模組 video / audio (需要 AutoJs6 6.8.1, 支援 Rhino, Node.js 與 Bun): 每個處理方法都有 xxxAsync 形式與進度回呼, 停止指令碼即取消其工作
* `新增` Studio 畫面: 媒體庫, 工具箱, 工作佇列與設定頁面; 外觀跟隨 AutoJs6, 也可單獨選擇語言, 夜間模式與主題色; 設定頁面引導授予所有檔案存取與電池無限制
* `新增` 工具箱精靈: 每個處理功能一個精靈頁面, 表單附預設值與取值範圍, 結果頁面顯示輸出, 媒體資訊與等價指令碼
* `新增` 時間軸剪輯器: 多軌編輯, 轉場, 疊加與字幕編輯, 即時預覽, 復原 / 重做, 專案儲存為 .slate.json, 匯出進入工作佇列
* `新增` 程式碼產生: 精靈, 時間軸與錄音頁面都提供等價的完整指令碼, 可複製或在 AutoJs6 中開啟為新指令碼
* `新增` Studio 錄音: 麥克風, 裝置內部聲音 (Android 10+) 或兩者混合, 儲存為 M4A / WAV, 支援暫停 / 繼續, 螢幕關閉時錄音與通知列控制
* `新增` 長時間的工作在前景服務中執行並顯示進度通知; 設定頁面可選擇硬體編碼器偏好與同時執行的工作數 (1 - 2)
* `新增` 省略輸出路徑時, 影片, 音訊與圖片分別儲存到 Movies, Music 與 Pictures 下的 3-Slate Studio 資料夾, 並登記到媒體庫
* `新增` 檢查更新: 讀取官方外掛索引, 開啟首頁時最多每 12 小時檢查一次, 可略過某個版本
* `新增` 所有 ABI 共用一個 APK: 唯一的原生程式庫來自 Jetpack Compose, 已依 16 KB 記憶體分頁對齊
* `新增` 10 種語言的畫面, README, 外掛中心說明與更新日誌
* `相依性` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 與 UI, 用於處理與預覽)
* `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00 (外掛畫面)
* `相依性` 附加 AndroidX Activity Compose 版本 1.13.0
* `相依性` 附加 Navigation Compose 版本 2.10.2
* `相依性` 附加 AndroidX Core KTX 版本 1.18.0
* `相依性` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1 (外掛畫面的主題色)
