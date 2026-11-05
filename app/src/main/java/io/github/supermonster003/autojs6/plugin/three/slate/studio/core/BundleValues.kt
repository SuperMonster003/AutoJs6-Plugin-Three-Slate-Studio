package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import android.os.Bundle

/**
 * Converts the `options` Bundle of a request into the plain values [OperationOptions] reads. The host serializes the
 * script object like `JSON.stringify` (shared notes of roadmap P1.3): integers as Long, other numbers as Double, nested
 * objects as Bundle, arrays as String[] / long[] / double[] / boolean[] or a list of Bundles, null and undefined
 * omitted. Nested Bundles become maps, arrays lists, Float becomes Double.
 * zh-CN: 把请求的 `options` Bundle 转成 [OperationOptions] 读取的纯值. 宿主按 `JSON.stringify` 的方式序列化脚本对象 (路线图
 * P1.3 共享笔记): 整数为 Long, 其他数为 Double, 嵌套对象为 Bundle, 数组为 String[] / long[] / double[] / boolean[] 或 Bundle
 * 列表, null 与 undefined 略去. 嵌套 Bundle 转为 Map, 数组转为 List, Float 转为 Double.
 */
internal object BundleValues {

    fun toMap(bundle: Bundle?): Map<String, Any?> {
        if (bundle == null) return emptyMap()
        bundle.classLoader = BundleValues::class.java.classLoader
        return bundle.keySet().sorted().associateWith { key -> value(bundle.raw(key)) }
    }

    fun value(raw: Any?): Any? = when (raw) {
        null -> null
        is Bundle -> toMap(raw)
        is BooleanArray -> raw.toList()
        is IntArray -> raw.map { it.toLong() }
        is LongArray -> raw.toList()
        is FloatArray -> raw.map { it.toDouble() }
        is DoubleArray -> raw.toList()
        is Array<*> -> raw.map(::value)
        is Collection<*> -> raw.map(::value)
        is Float -> raw.toDouble()
        is Int -> raw.toLong()
        is Short -> raw.toLong()
        is Byte -> raw.toLong()
        is CharSequence -> raw.toString()
        else -> raw
    }

    @Suppress("DEPRECATION")
    private fun Bundle.raw(key: String): Any? = get(key)

}
