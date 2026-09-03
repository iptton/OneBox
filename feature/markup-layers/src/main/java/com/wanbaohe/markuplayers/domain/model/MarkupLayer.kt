package com.wanbaohe.markuplayers.domain.model

import com.t8rin.imagetoolbox.core.filters.presentation.model.UiFilter
import com.wanbaohe.markuplayers.presentation.tools.adjust.BaseAdjustments
import java.util.UUID

/**
 * 一个图层 = 内容(type) + 变换(transform)。
 * 列表顺序即 z 序:索引越大越靠上。
 *
 * @param id 稳定唯一 id,用于选中态/历史快照定位
 * @param name 图层面板显示名,空串时按类型给默认名
 * @param adjustments 图层自身的调色(亮度/对比度/饱和度),中立值 = 无调节;
 * 随图层列表进 undo 历史,预览经 colorFilter 实时生效,导出时按图层烘焙
 * @param filter 图层自身的位图滤镜,仅位图类图层(图片/贴纸/涂鸦)可非空
 * ([LayerType.isBitmapFilterable]);文字/形状需先光栅化。变更进 undo 历史
 */
data class MarkupLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: LayerType,
    val transform: LayerTransform = LayerTransform(),
    val adjustments: BaseAdjustments = BaseAdjustments(),
    val filter: UiFilter<*>? = null,
)
