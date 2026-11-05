package io.github.supermonster003.autojs6.plugin.three.slate.studio

import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.android
import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.child
import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles.children
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Size, transparency and safe circle of the Icon Studio resources (roadmap D16, P0.2), measured with the formulas of
 * the Icon Studio kernel 1.4.0 (`.python/icon_studio_runtime.py`): `visual_size` and `maximum_radius` on a 432 px
 * canvas, so the 216 px list circle and the 132 px (33 dp of 108 dp) launcher safe circle.
 * zh-CN: Icon Studio 资源的尺寸, 透明度与安全圆 (路线图 D16, P0.2), 按 Icon Studio 内核 1.4.0 (`.python/icon_studio_runtime.py`)
 * 的公式测量: 432 px 画布上的 `visual_size` 与 `maximum_radius`, 即 216 px 的列表圆与 132 px (108 dp 中的 33 dp) 的启动器安全圆.
 */
class IconResourcesTest {

    @Test
    fun `every raster icon is a 432 px RGBA canvas with transparent corners`() {
        val pngs = Files.walk(RepositoryFiles.path("app/src/main/res")).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".png") }.map { RepositoryFiles.root.relativize(it).toString().replace('\\', '/') }.toList()
        }.sorted()
        assertEquals(14, pngs.size)
        pngs.forEach { relative ->
            val image = image(relative)
            assertEquals(relative, SIZE, image.width)
            assertEquals(relative, SIZE, image.height)
            assertTrue("$relative must carry alpha", image.colorModel.hasAlpha())
            val alpha = alpha(image)
            listOf(0 to 0, SIZE - 1 to 0, 0 to SIZE - 1, SIZE - 1 to SIZE - 1).forEach { (x, y) ->
                assertEquals("$relative ($x, $y)", 0, alpha[y * SIZE + x])
            }
        }
    }

    @Test
    fun `the Plugin Center pair is neutral, normalized to V 0_52 and inside the list circle`() {
        val day = image("app/src/main/res/mipmap/ic_plugin_center.png")
        val night = image("app/src/main/res/mipmap-night/ic_plugin_center.png")
        val scale = Regex("\"scale\"\\s*:\\s*([0-9.]+)").find(RepositoryFiles.text(".icons/recipe.json"))!!.groupValues[1].toDouble()
        assertEquals("roadmap P0.2: V = 0.52 without extra scaling", 1.0, scale, 0.0)
        listOf(day, night).forEach { image ->
            val alpha = alpha(image)
            assertTrue("more than half of the canvas is transparent", alpha.count { it == 0 } > SIZE * SIZE / 2)
            assertEquals(VISUAL_TARGET * scale, visualSize(alpha), RASTER_TOLERANCE)
            assertTrue("inside the 216 px list circle", maximumRadius(alpha) <= 216.0)
            assertTrue("grayscale artwork", pixels(image).all { argb -> argb ushr 24 == 0 || (red(argb) == green(argb) && green(argb) == blue(argb)) })
        }
        assertArrayEquals("light and dark artwork share one outline", alpha(day), alpha(night))
        assertTrue("dark glyph on the light canvas", meanOpaqueRed(day) < 128)
        assertTrue("light glyph on the dark canvas", meanOpaqueRed(night) > 128)
    }

    @Test
    fun `launcher foregrounds and monochrome layers stay inside the 33 dp safe circle`() {
        listOf(
            "mipmap/ic_launcher_system_foreground.png",
            "mipmap/ic_launcher_system_light_foreground.png",
            "mipmap/ic_launcher_monochrome.png",
            "mipmap/ic_icon_studio_application_foreground.png",
            "mipmap-night/ic_icon_studio_application_foreground.png",
            "mipmap/ic_icon_studio_application_monochrome.png",
        ).forEach { relative ->
            val alpha = alpha(image("app/src/main/res/$relative"))
            val radius = maximumRadius(alpha)
            assertTrue("$relative radius $radius", radius in 1.0..132.0)
        }
        listOf("mipmap/ic_launcher.png", "mipmap-night/ic_launcher.png").forEach { relative ->
            assertTrue("$relative inside the list circle", maximumRadius(alpha(image("app/src/main/res/$relative"))) <= 216.0)
        }
    }

    @Test
    fun `the four launcher aliases point at the generated icon modes`() {
        val application = RepositoryFiles.xml("app/src/main/AndroidManifest.xml").child("application")
        val aliases = application.children("activity-alias").associate { it.android("name") to it.android("icon") }
        assertEquals(
            mapOf(
                ".launcher.AdaptiveLightIconAlias" to "@mipmap/ic_launcher_system_light",
                ".launcher.AdaptiveDarkIconAlias" to "@mipmap/ic_launcher_system",
                ".launcher.AdaptiveAutoIconAlias" to "@mipmap/ic_launcher_system_auto",
                ".launcher.TransparentIconAlias" to "@mipmap/ic_launcher",
            ),
            aliases,
        )
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), application.children("activity-alias").filter { it.android("enabled") == "true" }.map { it.android("name") })
        // The automatic mode follows the system theme: dark by default, light under notnight.
        assertEquals("@mipmap/ic_launcher_system_foreground", foreground("mipmap-anydpi-v26/ic_launcher_system_auto.xml"))
        assertEquals("@mipmap/ic_launcher_system_light_foreground", foreground("mipmap-notnight-anydpi-v26/ic_launcher_system_auto.xml"))
        listOf("ic_launcher_system", "ic_launcher_system_light", "ic_launcher_system_auto").forEach { name ->
            val icon = RepositoryFiles.xml("app/src/main/res/mipmap-anydpi-v26/$name.xml")
            assertEquals("@mipmap/ic_launcher_monochrome", icon.child("monochrome").android("drawable"))
        }
    }

    private fun foreground(relative: String): String =
        RepositoryFiles.xml("app/src/main/res/$relative").child("foreground").android("drawable")

    private fun image(relative: String): BufferedImage =
        requireNotNull(ImageIO.read(RepositoryFiles.path(relative).toFile())) { "Unreadable $relative" }

    private fun pixels(image: BufferedImage): IntArray = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

    private fun alpha(image: BufferedImage): IntArray = pixels(image).map { it ushr 24 }.toIntArray()

    private fun red(argb: Int): Int = argb shr 16 and 0xFF

    private fun green(argb: Int): Int = argb shr 8 and 0xFF

    private fun blue(argb: Int): Int = argb and 0xFF

    private fun meanOpaqueRed(image: BufferedImage): Double = pixels(image).filter { it ushr 24 == 255 }.map(::red).average()

    /** `visual_size` of the kernel: the mean of the bounding box area and the ink area. zh-CN: 内核的 `visual_size`. */
    private fun visualSize(alpha: IntArray): Double {
        var left = SIZE
        var top = SIZE
        var right = -1
        var bottom = -1
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            if (alpha[y * SIZE + x] >= 16) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        if (right < 0) return 0.0
        val area = (right - left + 1).toDouble() * (bottom - top + 1)
        val ink = alpha.sum() / 255.0
        return sqrt((area + ink) / 2) / SIZE
    }

    /** `maximum_radius` of the kernel: the farthest non-transparent pixel from the center. zh-CN: 内核的 `maximum_radius`. */
    private fun maximumRadius(alpha: IntArray): Double {
        val center = (SIZE - 1) / 2.0
        var maximum = 0.0
        for (y in 0 until SIZE) {
            val occupied = (0 until SIZE).filter { alpha[y * SIZE + it] != 0 }
            if (occupied.isNotEmpty()) {
                maximum = maxOf(maximum, hypot(occupied.first() - center, y - center), hypot(occupied.last() - center, y - center))
            }
        }
        return maximum
    }

    private companion object {
        const val SIZE = 432
        const val VISUAL_TARGET = 0.52
        const val RASTER_TOLERANCE = 0.006
    }

}
