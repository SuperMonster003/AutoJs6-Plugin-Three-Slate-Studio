@file:OptIn(UnstableApi::class)

package io.github.supermonster003.autojs6.plugin.three.slate.studio.pipeline

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionDirection
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.TransitionType

/**
 * The per-clip half of the shader transitions of roadmap P3.2 (D30): `fade` (brightness to and from black), `wipe`
 * (an alpha mask on the upper of the two clips) and `blur` (a growing blur on the outgoing clip, a fading one on the
 * incoming clip). The compositor half (alpha, position, scale) lives in [ComposeCompositorSettings]. One instance
 * serves one clip: its time is the clip's own, counted from the first frame the effect sees, so the export's time base
 * does not matter. The shader is GLSL ES 1.00, which every GLES 2.0 GPU compiles; when a GPU still fails, the export
 * falls back (see [ComposeOperation]).
 * zh-CN: 路线图 P3.2 (D30) 着色器转场中属于单个片段的部分: `fade` (亮度渐变到黑与从黑渐变), `wipe` (两段中上层一段的透明遮罩) 与
 * `blur` (离场片段逐渐模糊, 入场片段模糊逐渐消退). 合成器部分 (透明度, 位置, 缩放) 在 [ComposeCompositorSettings]. 每个实例只服务
 * 一个片段: 其时间为片段自身时间, 从效果看到的第一帧起算, 因此与导出的时间基准无关. 着色器为 GLSL ES 1.00, 所有 GLES 2.0 GPU
 * 都能编译; 仍然失败时导出回退 (见 [ComposeOperation]).
 */
internal class TransitionEffect(
    /** The clip length on the timeline. zh-CN: 片段在时间线上的长度. */
    private val lengthUs: Long,
    private val into: Part?,
    private val out: Part?,
    /**
     * The blur runs in two passes, horizontal then vertical: the first instance of a clip does everything else and
     * the horizontal pass, a second one with [verticalBlur] only the vertical pass.
     * zh-CN: 模糊分两遍, 先水平后垂直: 片段的第一个实例完成其他效果与水平一遍, [verticalBlur] 为 true 的第二个实例只做垂直一遍.
     */
    private val verticalBlur: Boolean = false,
) : GlEffect {

    /**
     * One end of the clip: [type] lasts [sideUs] inside this clip; [masked] says this clip is the upper one, which
     * carries the wipe mask; [direction] is the direction of the transition.
     * zh-CN: 片段的一端: [type] 在本段内持续 [sideUs]; [masked] 表示本段在上层, 由它承载擦除遮罩; [direction] 为转场方向.
     */
    data class Part(val type: TransitionType, val sideUs: Long, val direction: TransitionDirection, val masked: Boolean)

    /** Uniform values at one moment. zh-CN: 某一时刻的 uniform 值. */
    data class State(
        val brightness: Float = 1f,
        val blur: Float = 0f,
        val wipe: Boolean = false,
        val wipeAxisY: Boolean = false,
        val wipeFlip: Boolean = false,
        val wipeThreshold: Float = 0f,
        val wipeKeepBelow: Boolean = true,
    )

    /** Set by [ComposeOperation.failShadersForTest]. zh-CN: 由 [ComposeOperation.failShadersForTest] 设置. */
    @Volatile
    var failForTest: Boolean = false

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        if (failForTest) throw VideoFrameProcessingException("the transition shader was made to fail for a test")
        return Program(this)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        if (verticalBlur) into?.type != TransitionType.BLUR && out?.type != TransitionType.BLUR else into == null && out == null

    /**
     * The uniforms at clip time [localUs]; [maxBlur] is the blur radius at full strength in pixels.
     * zh-CN: 片段时间 [localUs] 处的 uniform; [maxBlur] 为最强模糊的半径 (像素).
     */
    fun stateAt(localUs: Long): State {
        var state = State()
        if (verticalBlur) {
            into?.let { part -> if (part.type == TransitionType.BLUR && localUs < part.sideUs) state = state.copy(blur = 1f - progress(localUs, part.sideUs)) }
            out?.let { part ->
                val begin = lengthUs - part.sideUs
                if (part.type == TransitionType.BLUR && localUs >= begin) state = state.copy(blur = maxOf(state.blur, progress(localUs - begin, part.sideUs)))
            }
            return state
        }
        into?.let { part ->
            if (localUs < part.sideUs) state = apply(state, part, progress(localUs, part.sideUs), incoming = true)
        }
        out?.let { part ->
            val begin = lengthUs - part.sideUs
            if (localUs >= begin) state = apply(state, part, progress(localUs - begin, part.sideUs), incoming = false)
        }
        return state
    }

    private fun apply(state: State, part: Part, progress: Float, incoming: Boolean): State = when (part.type) {
        TransitionType.FADE -> state.copy(brightness = state.brightness * if (incoming) progress else 1f - progress)
        TransitionType.BLUR -> state.copy(blur = maxOf(state.blur, if (incoming) 1f - progress else progress))
        TransitionType.WIPE -> if (!part.masked) state else {
            val (axisY, flip) = wipeAxis(part.direction)
            // The incoming clip shows where the wipe coordinate is below the progress. zh-CN: 入场片段在擦除坐标小于进度处可见.
            state.copy(wipe = true, wipeAxisY = axisY, wipeFlip = flip, wipeThreshold = progress, wipeKeepBelow = incoming)
        }
        else -> state
    }

    private class Program(private val effect: TransitionEffect) : BaseGlShaderProgram(false, 1) {

        private val program: GlProgram = try {
            GlProgram(VERTEX_SHADER, FRAGMENT_SHADER).apply {
                setBufferAttribute("aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE)
            }
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException("the transition shader does not compile on this GPU", e)
        }

        private var width = 0

        private var height = 0

        private var firstUs: Long? = null

        override fun configure(inputWidth: Int, inputHeight: Int): Size {
            width = inputWidth
            height = inputHeight
            return Size(inputWidth, inputHeight)
        }

        override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
            val first = firstUs ?: presentationTimeUs.also { firstUs = it }
            val state = effect.stateAt(presentationTimeUs - first)
            try {
                program.use()
                program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
                program.setFloatsUniform("uTexelSize", floatArrayOf(1f / width.coerceAtLeast(1), 1f / height.coerceAtLeast(1)))
                program.setFloatUniform("uBrightness", state.brightness)
                program.setFloatUniform("uBlur", state.blur * maxBlurPixels(width, height))
                program.setFloatsUniform("uBlurDirection", if (effect.verticalBlur) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f))
                program.setFloatsUniform(
                    "uWipe",
                    floatArrayOf(
                        if (state.wipe) 1f else 0f,
                        if (state.wipeAxisY) 1f else 0f,
                        if (state.wipeFlip) 1f else 0f,
                        state.wipeThreshold,
                    ),
                )
                program.setFloatUniform("uWipeKeepBelow", if (state.wipeKeepBelow) 1f else 0f)
                program.bindAttributesAndUniforms()
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GlUtil.checkGlError()
            } catch (e: GlUtil.GlException) {
                throw VideoFrameProcessingException(e, presentationTimeUs)
            }
        }

        override fun release() {
            super.release()
            try {
                program.delete()
            } catch (e: GlUtil.GlException) {
                throw VideoFrameProcessingException(e)
            }
        }

    }

    companion object {

        /** Blur radius at full strength: 5 % of the shorter side. zh-CN: 最强模糊半径: 短边的 5 %. */
        const val MAX_BLUR_FRACTION = 0.05f

        fun maxBlurPixels(width: Int, height: Int): Float = minOf(width, height) * MAX_BLUR_FRACTION

        fun progress(elapsedUs: Long, spanUs: Long): Float = (elapsedUs.toDouble() / spanUs.coerceAtLeast(1L)).coerceIn(0.0, 1.0).toFloat()

        /**
         * The texture axis a wipe runs along and whether it runs against it: texture x grows to the right, texture y
         * grows upwards. `left` reveals from the right edge, `up` from the bottom edge.
         * zh-CN: 擦除沿哪条纹理轴进行以及是否反向: 纹理 x 向右增大, 纹理 y 向上增大. `left` 从右边缘开始显露, `up` 从下边缘开始.
         */
        fun wipeAxis(direction: TransitionDirection): Pair<Boolean, Boolean> = when (direction) {
            TransitionDirection.LEFT -> false to true
            TransitionDirection.RIGHT -> false to false
            TransitionDirection.UP -> true to false
            TransitionDirection.DOWN -> true to true
        }

        private const val VERTEX_SHADER = """#version 100
attribute vec4 aFramePosition;
varying vec2 vTexSamplingCoord;
void main() {
  gl_Position = aFramePosition;
  vTexSamplingCoord = aFramePosition.xy * 0.5 + 0.5;
}
"""

        /**
         * One pass of a 15 tap Gaussian blur along [uBlurDirection] (spacing radius / 7), brightness and a hard wipe mask.
         * zh-CN: 沿 uBlurDirection 的一遍 15 点高斯模糊 (间距为半径 / 7), 亮度与硬边擦除遮罩.
         */
        private const val FRAGMENT_SHADER = """#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform vec2 uTexelSize;
uniform float uBrightness;
uniform float uBlur;
uniform vec2 uBlurDirection;
uniform vec4 uWipe;
uniform float uWipeKeepBelow;
varying vec2 vTexSamplingCoord;
void main() {
  vec4 color;
  if (uBlur > 0.5) {
    vec4 sum = vec4(0.0);
    float total = 0.0;
    vec2 delta = uTexelSize * uBlurDirection * (uBlur / 7.0);
    for (int i = -7; i <= 7; i++) {
      float weight = exp(-float(i * i) / 24.5);
      sum += texture2D(uTexSampler, vTexSamplingCoord + float(i) * delta) * weight;
      total += weight;
    }
    color = sum / total;
  } else {
    color = texture2D(uTexSampler, vTexSamplingCoord);
  }
  color.rgb *= uBrightness;
  if (uWipe.x > 0.5) {
    float c = uWipe.y > 0.5 ? vTexSamplingCoord.y : vTexSamplingCoord.x;
    if (uWipe.z > 0.5) {
      c = 1.0 - c;
    }
    float below = c < uWipe.w ? 1.0 : 0.0;
    float keep = uWipeKeepBelow > 0.5 ? below : 1.0 - below;
    color *= keep;
  }
  gl_FragColor = color;
}
"""

    }

}
