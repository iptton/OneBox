package com.wanbaohe.markuplayers.presentation.render

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import com.wanbaohe.markuplayers.domain.model.LayerType
import com.wanbaohe.markuplayers.domain.model.MarkupLayer
import kotlin.reflect.KClass

/**
 * 图层预览渲染器:在编辑画布内按基础尺寸渲染图层内容。
 * 位置/缩放/旋转/不透明度由外层 EditBox 的 graphicsLayer 统一应用,
 * 图层调节(adjustments)由调用方包一层 graphicsLayer colorFilter,
 * 渲染器只关心内容本身。
 */
interface LayerPreviewRenderer {

    /** 本渲染器负责的图层类型 */
    val supportedType: KClass<out LayerType>

    /**
     * 按基础尺寸渲染图层内容。
     * [canvasWidthPx]/[canvasHeightPx] 为预览画布(底图显示区域)像素尺寸,
     * 归一化参数(如 fontSizeRatio)按它换算。
     * [filteredBitmap] 为图层带滤镜时组件预算的「内容过滤镜后」位图,
     * 非空时渲染它替代原始内容;为空按无滤镜渲染。
     */
    @Composable
    fun Content(
        layer: MarkupLayer,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        filteredBitmap: Bitmap? = null,
    )
}

/** 预览渲染注册表:按 [LayerPreviewRenderer.supportedType] 分发 */
object LayerPreviewRenderers {

    val renderers: List<LayerPreviewRenderer> = listOf(
        TextLayerPreviewRenderer,
        StickerLayerPreviewRenderer,
        ImageLayerPreviewRenderer,
        DrawLayerPreviewRenderer,
        ShapeLayerPreviewRenderer,
    )

    fun find(type: LayerType): LayerPreviewRenderer? = renderers.firstOrNull {
        it.supportedType.isInstance(type)
    }

    @Composable
    fun Content(
        layer: MarkupLayer,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        filteredBitmap: Bitmap? = null,
    ) {
        find(layer.type)?.Content(layer, canvasWidthPx, canvasHeightPx, filteredBitmap)
    }
}
