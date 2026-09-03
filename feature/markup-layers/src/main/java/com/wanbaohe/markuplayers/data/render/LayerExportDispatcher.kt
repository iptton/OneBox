package com.wanbaohe.markuplayers.data.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.wanbaohe.markuplayers.domain.model.MarkupLayer
import com.wanbaohe.markuplayers.domain.render.LayerExportRenderer
import com.wanbaohe.markuplayers.presentation.tools.adjust.toColorMatrixValues
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 导出渲染调度器:统一处理图层的定位/缩放/旋转/不透明度/可见性与调色,
 * 再把"以内容中心为原点"的画布交给具体渲染器绘制内容。
 * 图层自带滤镜([MarkupLayer.filter])时,过滤后位图由调用方预算并经
 * [filteredLayers] 传入(layerId → 位图),调度器只负责透传给渲染器。
 * 新增图层类型时在构造参数里加一行对应渲染器即可。
 */
@Singleton
class LayerExportDispatcher @Inject constructor(
    textRenderer: TextLayerExportRenderer,
    stickerRenderer: StickerLayerExportRenderer,
    imageRenderer: ImageLayerExportRenderer,
    drawRenderer: DrawLayerExportRenderer,
    shapeRenderer: ShapeLayerExportRenderer,
) {

    private val renderers: List<LayerExportRenderer> = listOf(
        textRenderer,
        stickerRenderer,
        imageRenderer,
        drawRenderer,
        shapeRenderer,
    )

    fun draw(
        canvas: Canvas,
        layer: MarkupLayer,
        imageWidth: Int,
        imageHeight: Int,
        filteredLayers: Map<String, Bitmap> = emptyMap(),
    ) {
        val transform = layer.transform
        if (!transform.visible) return
        val renderer = renderers.firstOrNull {
            it.supportedType.isInstance(layer.type)
        } ?: return

        canvas.save()
        canvas.translate(
            transform.centerX * imageWidth,
            transform.centerY * imageHeight
        )
        if (transform.rotation != 0f) canvas.rotate(transform.rotation)
        if (transform.scale != 1f) canvas.scale(transform.scale, transform.scale)

        // 不透明度与调色(图层自身 adjustments)合成一个 saveLayer 的 Paint:
        // 内容先绘进离屏层,restore 时统一带 alpha/colorFilter 合成,与预览侧
        // graphicsLayer(alpha + colorFilter) 的观感一致
        val layerPaint = Paint().apply {
            alpha = (transform.alpha.coerceIn(0f, 1f) * 255).roundToInt()
            if (!layer.adjustments.isNeutral) {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix(layer.adjustments.toColorMatrixValues())
                )
            }
        }
        val filtered = filteredLayers[layer.id]
        if (transform.alpha < 1f || !layer.adjustments.isNeutral) {
            canvas.saveLayer(null, layerPaint)
            renderer.draw(canvas, layer, imageWidth, imageHeight, filtered)
            canvas.restore()
        } else {
            renderer.draw(canvas, layer, imageWidth, imageHeight, filtered)
        }

        canvas.restore()
    }
}
