package io.github.supermonster003.autojs6.plugin.three.slate.studio.core

import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The option rules of `overlay` (roadmap P2.3, D29): the host copies them from the hand-over note `3-slate-p2b-notes.md`,
 * so the messages are part of the contract.
 * zh-CN: `overlay` 的选项规则 (路线图 P2.3, D29): 宿主按交接笔记 `3-slate-p2b-notes.md` 照抄, 因此消息也是契约的一部分.
 */
class OverlayOptionsTest {

    private fun overlay(vararg items: Map<String, Any?>, extra: Map<String, Any?> = emptyMap()) =
        OperationOptions.parse(MediaStudioOps.OVERLAY, 1, mapOf("overlays" to items.toList()) + extra) as OperationSpec.Overlay

    private fun item(vararg pairs: Pair<String, Any?>) = overlay(mapOf(*pairs)).items.single()

    private fun invalid(options: Map<String, Any?>): String {
        val error = assertThrows(MediaStudioException::class.java) { OperationOptions.parse(MediaStudioOps.OVERLAY, 1, options) }
        assertEquals(MediaStudioErrorCodes.INVALID_OPTIONS, error.code)
        return error.detail
    }

    private fun invalidItem(vararg pairs: Pair<String, Any?>) = invalid(mapOf("overlays" to listOf(mapOf(*pairs))))

    private val center = OverlayPlacement(Length.Percent(50.0), Length.Percent(50.0), Anchor.CENTER, 0, null, 1.0)

    @Test
    fun `a text overlay needs only its text and is centered white sans-serif 48 px`() {
        assertEquals(
            OverlayItem.Text(
                text = "AutoJs6",
                font = "sans-serif",
                fontSize = 48.0,
                bold = false,
                italic = false,
                color = Colors.WHITE,
                strokeColor = null,
                strokeWidth = 0.0,
                shadowColor = null,
                shadowRadius = 4.0,
                shadowDx = 2.0,
                shadowDy = 2.0,
                align = TextAlign.CENTER,
                placement = center,
            ),
            item("type" to "text", "text" to "AutoJs6"),
        )
        assertEquals(OperationSpec.Overlay(listOf(item("type" to "text", "text" to "a")), VideoEncoding()), overlay(mapOf("type" to "text", "text" to "a")))
    }

    @Test
    fun `stroke and shadow turn on with any of their keys and default to black`() {
        val stroked = item("type" to "text", "text" to "a", "fontSize" to 32, "strokeWidth" to 3) as OverlayItem.Text
        assertEquals(Colors.BLACK, stroked.strokeColor)
        assertEquals(3.0, stroked.strokeWidth, 0.0)
        val colored = item("type" to "text", "text" to "a", "fontSize" to 32, "stroke" to "#F00") as OverlayItem.Text
        assertEquals(0xFFFF0000.toInt(), colored.strokeColor)
        assertEquals("stroke width defaults to font size / 16", 2.0, colored.strokeWidth, 0.0)
        val shadowed = item("type" to "text", "text" to "a", "shadowDy" to -5) as OverlayItem.Text
        assertEquals(Colors.BLACK, shadowed.shadowColor)
        assertEquals(-5.0, shadowed.shadowDy, 0.0)
        assertNull((item("type" to "text", "text" to "a", "strokeWidth" to 0) as OverlayItem.Text).strokeColor)
    }

    @Test
    fun `every text key is read`() {
        val text = item(
            "type" to "text", "text" to "line 1\nline 2", "font" to "/sdcard/Fonts/a.ttf", "fontSize" to 20.5, "bold" to true, "italic" to true,
            "color" to "#80112233", "stroke" to "white", "strokeWidth" to 1, "shadow" to -16777216L, "shadowRadius" to 0, "shadowDx" to 1,
            "shadowDy" to 1, "align" to "right", "x" to 10, "y" to "-5%", "anchor" to "bottom-right", "from" to 500, "to" to 1500L, "alpha" to 0.25,
        ) as OverlayItem.Text
        assertEquals("/sdcard/Fonts/a.ttf", text.fontFile)
        assertEquals(0x80112233.toInt(), text.color)
        assertEquals(Colors.WHITE, text.strokeColor)
        assertEquals(Colors.BLACK, text.shadowColor)
        assertEquals(TextAlign.RIGHT, text.align)
        assertEquals(OverlayPlacement(Length.Pixels(10.0), Length.Percent(-5.0), Anchor.BOTTOM_RIGHT, 500, 1500, 0.25), text.placement)
    }

    @Test
    fun `image overlays take a scale or a size in pixels or percent`() {
        assertEquals(OverlayItem.Image("/sdcard/logo.png", null, null, null, center), item("type" to "image", "src" to "/sdcard/logo.png"))
        assertEquals(OverlayItem.Image("/sdcard/logo.png", 0.3, null, null, center), item("type" to "image", "src" to "/sdcard/logo.png", "scale" to 0.3))
        assertEquals(
            OverlayItem.Image("/sdcard/logo.png", null, Length.Percent(25.0), Length.Pixels(40.0), center),
            item("type" to "image", "src" to "/sdcard/logo.png", "width" to " 25 % ", "height" to 40),
        )
        assertEquals(
            "options.overlays[0].scale and options.overlays[0].width / options.overlays[0].height are exclusive",
            invalidItem("type" to "image", "src" to "/a.png", "scale" to 1, "height" to 10),
        )
        assertEquals("options.overlays[0].src must be an absolute path, got logo.png", invalidItem("type" to "image", "src" to "logo.png"))
        assertEquals("options.overlays[0].src is required", invalidItem("type" to "image"))
        assertEquals("options.overlays[0].width must be greater than 0 and at most 16384 px, got 0", invalidItem("type" to "image", "src" to "/a.png", "width" to 0))
        assertEquals("options.overlays[0].scale must be greater than 0 and at most 8, got 0", invalidItem("type" to "image", "src" to "/a.png", "scale" to 0))
    }

    @Test
    fun `the overlays array is required and holds 1 to 64 objects`() {
        assertEquals("options.overlays is required", invalid(emptyMap()))
        assertEquals("options.overlays must hold 1 to 64 overlays, got 0", invalid(mapOf("overlays" to emptyList<Any>())))
        val text = mapOf("type" to "text", "text" to "a")
        assertEquals(64, overlay(*Array(64) { text }).items.size)
        assertEquals("options.overlays must hold 1 to 64 overlays, got 65", invalid(mapOf("overlays" to List(65) { text })))
        assertEquals("options.overlays must be an array of objects", invalid(mapOf("overlays" to "text")))
        assertEquals("options.overlays[1] must be an object", invalid(mapOf("overlays" to listOf(text, "b"))))
        assertEquals("options.overlays[0].type is required", invalidItem("text" to "a"))
        assertEquals("options.overlays[0].type must be one of text, image, got video", invalidItem("type" to "video"))
    }

    @Test
    fun `keys of the other type are unknown and the encoder keys stay at the top level`() {
        assertEquals("unknown option options.overlays[0].src", invalidItem("type" to "text", "text" to "a", "src" to "/a.png"))
        assertEquals("unknown option options.overlays[0].fontSize", invalidItem("type" to "image", "src" to "/a.png", "fontSize" to 10))
        assertEquals("unknown option options.width", invalid(mapOf("overlays" to listOf(mapOf("type" to "text", "text" to "a")), "width" to 100)))
        assertEquals(
            VideoEncoding(VideoCodec.H265, 2_000_000),
            overlay(mapOf("type" to "text", "text" to "a"), extra = mapOf("codec" to "h265", "bitRate" to 2_000_000)).video,
        )
    }

    @Test
    fun `text, font, sizes, positions and times are checked`() {
        assertEquals("options.overlays[0].text is required", invalidItem("type" to "text"))
        assertEquals("options.overlays[0].text must have 1 to 1000 characters, got 0", invalidItem("type" to "text", "text" to ""))
        assertEquals("options.overlays[0].text must have 1 to 1000 characters, got 1001", invalidItem("type" to "text", "text" to "x".repeat(1001)))
        assertEquals("options.overlays[0].text must be a string", invalidItem("type" to "text", "text" to 5))
        assertEquals(
            "options.overlays[0].font must be one of sans-serif, serif, monospace or an absolute font file path, got Roboto",
            invalidItem("type" to "text", "text" to "a", "font" to "Roboto"),
        )
        assertEquals("options.overlays[0].fontSize must be greater than 0 and at most 1000, got 0", invalidItem("type" to "text", "text" to "a", "fontSize" to 0))
        assertEquals(
            "options.overlays[0].x must be a number of pixels or a percentage like \"50%\", got 50",
            invalidItem("type" to "text", "text" to "a", "x" to "50"),
        )
        assertEquals(
            "options.overlays[0].y must be at least -1000% and at most 1000%, got 1001%",
            invalidItem("type" to "text", "text" to "a", "y" to "1001%"),
        )
        assertEquals("options.overlays[0].x must be at least -16384 px and at most 16384 px, got -20000", invalidItem("type" to "text", "text" to "a", "x" to -20_000.0))
        assertEquals(
            "options.overlays[0].to (1000) must be greater than options.overlays[0].from (1000)",
            invalidItem("type" to "text", "text" to "a", "from" to 1_000, "to" to 1_000),
        )
        assertEquals("options.overlays[0].alpha must be at least 0 and at most 1, got 2", invalidItem("type" to "text", "text" to "a", "alpha" to 2))
        assertEquals(
            "options.overlays[0].anchor must be one of center, top-left, top, top-right, left, right, bottom-left, bottom, bottom-right, got middle",
            invalidItem("type" to "text", "text" to "a", "anchor" to "middle"),
        )
        assertEquals("options.overlays[0].from must be an integer, got 1.5", invalidItem("type" to "text", "text" to "a", "from" to 1.5))
    }

    @Test
    fun `colors take hex forms, names and ARGB integers`() {
        assertEquals(0xFFFFFFFF.toInt(), Colors.parse("#fff"))
        assertEquals(0xFF112233.toInt(), Colors.parse("#112233"))
        assertEquals(0x00112233, Colors.parse("#00112233"))
        assertEquals(0xFFFF0000.toInt(), Colors.parse(" Red "))
        assertEquals(0, Colors.parse("transparent"))
        assertNull(Colors.parse("#12345"))
        assertNull(Colors.parse("#GGGGGG"))
        assertNull(Colors.parse("rouge"))
        assertEquals(0xFF00FF00.toInt(), (item("type" to "text", "text" to "a", "color" to 0xFF00FF00L) as OverlayItem.Text).color)
        assertEquals(
            "options.overlays[0].color must be #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer, got #12",
            invalidItem("type" to "text", "text" to "a", "color" to "#12"),
        )
        assertEquals(
            "options.overlays[0].color as a number must be an ARGB integer, got 4294967296",
            invalidItem("type" to "text", "text" to "a", "color" to 4_294_967_296L),
        )
        assertEquals("options.overlays[0].color must be a number", invalidItem("type" to "text", "text" to "a", "color" to true))
    }

    @Test
    fun `placement resolves the anchor point and the time window`() {
        val placement = OverlayPlacement(Length.Percent(100.0), Length.Pixels(10.0), Anchor.TOP_RIGHT, 1_000, 2_000, 1.0)
        assertEquals(1180.0 to 10.0, placement.topLeft(1280, 720, 100.0, 50.0))
        assertEquals(590.0 to 335.0, center.topLeft(1280, 720, 100.0, 50.0))
        assertEquals(false, placement.isActive(999.9))
        assertEquals(true, placement.isActive(1_000.0))
        assertEquals(false, placement.isActive(2_000.0))
        assertEquals(true, center.isActive(1e12))
    }

    @Test
    fun `image size keeps the ratio when only one side is given`() {
        val image = OverlayItem.Image("/a.png", null, Length.Percent(50.0), null, center)
        assertEquals(640.0 to 160.0, image.drawnSize(400, 100, 1280, 720))
        assertEquals(80.0 to 20.0, image.copy(width = null, scale = 0.2).drawnSize(400, 100, 1280, 720))
        assertEquals(200.0 to 72.0, image.copy(width = Length.Pixels(200.0), height = Length.Percent(10.0)).drawnSize(400, 100, 1280, 720))
        assertEquals(100.0 to 25.0, image.copy(width = null, height = Length.Pixels(25.0)).drawnSize(400, 100, 1280, 720))
    }

    @Test
    fun `pictures and font files are the extra files the planner checks`() {
        val spec = overlay(
            mapOf("type" to "image", "src" to "/sdcard/a.png"),
            mapOf("type" to "text", "text" to "x", "font" to "/sdcard/f.ttf"),
            mapOf("type" to "image", "src" to "/sdcard/a.png", "scale" to 2),
            mapOf("type" to "text", "text" to "y", "font" to "serif"),
        )
        assertEquals(listOf("/sdcard/a.png", "/sdcard/f.ttf"), OperationOptions.resources(spec))
    }

}
