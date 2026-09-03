package com.wanbaohe.markuplayers.domain.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.wanbaohe.markuplayers.domain.model.LayerType
import com.wanbaohe.markuplayers.domain.model.MarkupLayer
import kotlin.reflect.KClass

/**
 * 图层导出渲染器:把图层按原图尺寸绘制到 [Canvas]。
 * 每种 [LayerType] 对应一个实现,在 data 层的调度器中注册。
 * 新增图层类型时:实现本接口 → 在调度器构造参数里加一行。
 */
interface LayerExportRenderer {

    /** 本渲染器负责的图层类型 */
    val supportedType: KClass<out LayerType>

    /**
     * 将图层绘制到画布。
     * 实现内部负责:按 [imageWidth]/[imageHeight] 把归一化坐标换算为像素,
     * 并应用 transform(中心点/缩放/旋转/不透明度/可见性由调度器统一处理,
     * 渲染器只需按基础尺寸绘制内容;图层的调色也由调度器统一处理)。
     *
     * @param filtered 图层带滤镜时由调用方预算的「内容过滤镜后」位图
     * (图片/贴纸 = 源图过滤镜;涂鸦 = 笔画位图过滤镜);非空时直接绘制它,
     * 为空则按无滤镜绘制原始内容。非位图类图层(文字/形状)恒为 null。
     */
    fun draw(
        canvas: Canvas,
        layer: MarkupLayer,
        imageWidth: Int,
        imageHeight: Int,
        filtered: Bitmap? = null,
    )
}
