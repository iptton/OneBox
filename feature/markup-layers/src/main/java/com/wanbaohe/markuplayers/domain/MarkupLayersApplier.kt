package com.wanbaohe.markuplayers.domain

import com.wanbaohe.markuplayers.domain.model.MarkupLayer

/**
 * 把图层列表按 z 序逐层重绘到原图的可变副本上,输出与原图同分辨率的位图。
 * 图层自带滤镜([MarkupLayer.filter])时,过滤后位图由调用方预算并经
 * [filteredLayers] 传入(layerId → 位图),未提供的图层按无滤镜绘制。
 */
interface MarkupLayersApplier<I> {

    suspend fun applyToImage(
        image: I,
        layers: List<MarkupLayer>,
        filteredLayers: Map<String, I> = emptyMap(),
    ): I
}
