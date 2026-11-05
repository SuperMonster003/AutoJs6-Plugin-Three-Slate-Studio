******

### 发行历史

******

# v1.0.0

###### 2026/11/05

* `提示` 开发预览: 插件可以被 AutoJs6 插件中心识别, 加工功能, Studio 界面与 video / audio 脚本模块的加工方法尚未交付
* `提示` 需要 AutoJs6 6.8.1 (6641) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
* `新增` 插件骨架: Wake 激活协议, 插件中心身份服务与媒体加工能力服务 (契约版本 1)
* `新增` 首页提供所有文件访问权限的授权入口
* `新增` 所有 ABI 共用一个 APK: 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐
* `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
* `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
* `依赖` 附加 Navigation Compose 版本 2.10.2
* `依赖` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 与 UI, 用于加工与预览)
* `依赖` 附加 AndroidX Core KTX 版本 1.18.0
