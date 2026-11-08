# The host contract classes (PluginInfo and the AIDL stubs) cross the process boundary by name.
-keep class org.autojs.plugin.** { *; }
-dontwarn kotlinx.parcelize.Parcelize

# R8 8.13 horizontal class merging merged media3 Transformer classes that hold API 31 LogSessionId values (for example
# DefaultAssetLoaderFactory and lambdas capturing the session id) into classes loaded on every API level; the merged
# fields become Object with check-casts, which throw NoClassDefFoundError on Android 7 - 11 (API 24 - 30) before any
# export starts (roadmap P5.4, appendix D Q121). Shrinking and obfuscation stay; optimization (inlining and class
# merging) is disabled, as in 3-Block Compose.
# zh-CN: R8 8.13 的横向类合并把持有 API 31 LogSessionId 的 media3 Transformer 类 (例如 DefaultAssetLoaderFactory 与捕获会话 id 的
# lambda) 合并进所有 API 级别都会加载的类; 合并后的字段变为 Object 并带 check-cast, 在 Android 7 - 11 (API 24 - 30) 上导出开始前
# 即抛 NoClassDefFoundError (路线图 P5.4, 附录 D Q121). 保留压缩与混淆, 关闭优化 (内联与类合并), 与 3-Block Compose 相同.
-dontoptimize
