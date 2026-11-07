******

### 发行历史

******

# v1.0.0

###### 2026/11/08

* `提示` 在脚本中使用 video / audio 的加工方法需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 插件界面, 时间线剪辑器与 Studio 录音不依赖 AutoJs6
* `提示` 脚本 API 参阅 AutoJs6 文档的 [video](https://docs.autojs6.com/#/video) 与 [audio](https://docs.autojs6.com/#/audio) 章节
* `新增` 视频加工: 裁剪, 拼接, 变速 (0.25 - 8 倍), 缩放, 旋转, 裁切与翻转, 文字或图片水印, 抽取或去除音轨, 转码 (H.264, 设备支持时可选 H.265)
* `新增` 抽帧, 缩略图与 GIF: 保存为 PNG / JPG 文件或返回 Image 对象, 内置 GIF 编码器 (调色板, 抖动, 循环)
* `新增` 音频加工: 裁剪, 拼接, 混音 (会削波时自动缩放), 音量与淡入淡出, 变速, 转换为 M4A / WAV / FLAC / OGG (OGG 需要 Android 10+)
* `新增` 多轨合成 video.compose: 至多 8 条视频轨与 16 条音频轨, 7 种转场, 文字 / 图片叠加与字幕
* `新增` 字幕: SRT 读写与 ASS 基础子集, 可用 video.burnSubtitles 烧录进画面, 用 video.subtitles.parse / write 读写字幕文件
* `新增` AutoJs6 脚本模块 video / audio (需要 AutoJs6 6.8.1, 支持 Rhino, Node.js 与 Bun): 每个加工方法都有 xxxAsync 形态与进度回调, 停止脚本即取消其任务
* `新增` Studio 界面: 媒体库, 工具箱, 任务队列与设置页; 外观跟随 AutoJs6, 也可单独选择语言, 夜间模式与主题色; 设置页引导授予所有文件访问与电池无限制
* `新增` 工具箱向导: 每个加工功能一个向导页, 表单带默认值与取值范围, 结果页显示输出, 媒体信息与等价脚本
* `新增` 时间线剪辑器: 多轨编辑, 转场, 叠加与字幕编辑, 实时预览, 撤销 / 重做, 工程保存为 .slate.json, 导出进入任务队列
* `新增` 代码生成: 向导, 时间线与录音页都给出等价的完整脚本, 可复制或在 AutoJs6 中打开为新脚本
* `新增` Studio 录音: 麦克风, 设备内部声音 (Android 10+) 或两者混合, 保存为 M4A / WAV, 支持暂停 / 继续, 熄屏录音与通知栏控制
* `新增` 长任务在前台服务中运行并显示进度通知; 设置页可选择硬件编码器偏好与同时运行的任务数 (1 - 2)
* `新增` 省略输出路径时, 视频, 音频与图片分别保存到 Movies, Music 与 Pictures 下的 3-Slate Studio 目录, 并登记到媒体库
* `新增` 更新检查: 读取官方插件索引, 打开首页时最多每 12 小时检查一次, 可忽略某个版本
* `新增` 所有 ABI 共用一个 APK: 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐
* `新增` 10 种语言的界面, README, 插件中心说明与更新日志
* `依赖` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 与 UI, 用于加工与预览)
* `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
* `依赖` 附加 AndroidX Activity Compose 版本 1.13.0
* `依赖` 附加 Navigation Compose 版本 2.10.2
* `依赖` 附加 AndroidX Core KTX 版本 1.18.0
* `依赖` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1 (插件界面的主题色)
