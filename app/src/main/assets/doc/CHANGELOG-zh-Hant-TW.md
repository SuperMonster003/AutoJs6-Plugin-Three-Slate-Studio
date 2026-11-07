******

### 發行歷史

******

# v1.0.0

###### 2026/11/05

* `提示` 開發預覽: 外掛可以被 AutoJs6 外掛中心辨識, 處理功能, Studio 畫面與 video / audio 指令碼模組的處理方法尚未交付
* `提示` 需要 AutoJs6 6.8.1 (6641) 或更新版本 (準確的最低版本號待主程式端改動落地後回填)
* `新增` 外掛骨架: Wake 啟用協定, 外掛中心身分服務與媒體處理能力服務 (契約版本 2)
* `新增` 首頁提供所有檔案存取權限的授權入口
* `新增` 所有 ABI 共用一個 APK: 唯一的原生程式庫來自 Jetpack Compose, 已依 16 KB 記憶體分頁對齊
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌, 由 JSON 來源檔統一產生
* `相依性` 附加 Jetpack Compose BOM 版本 2026.09.00 (外掛畫面)
* `相依性` 附加 Navigation Compose 版本 2.10.2
* `相依性` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 與 UI, 用於處理與預覽)
* `相依性` 附加 AndroidX Core KTX 版本 1.18.0
