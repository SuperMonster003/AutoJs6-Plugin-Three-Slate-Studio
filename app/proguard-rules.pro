# The host contract classes (PluginInfo and the AIDL stubs) cross the process boundary by name.
-keep class org.autojs.plugin.** { *; }
-dontwarn kotlinx.parcelize.Parcelize
