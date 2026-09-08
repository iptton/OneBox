package com.wanbaohe.recordcenter.registry

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记录类型目录 — 聚合 Hilt @IntoMap 注册的全部 [RecordTypeDefinition]。
 *
 * UI 列表、表单、图表与 Agent 工具(动态 enum / 描述)统一从这里取类型信息。
 */
@Singleton
class RecordTypeCatalog @Inject constructor(
    private val definitions: Map<String, @JvmSuppressWildcards RecordTypeDefinition>,
) {

    fun byKey(key: String): RecordTypeDefinition? = definitions[key]

    /** 全部类型,按 sortOrder 升序 */
    fun all(): List<RecordTypeDefinition> = definitions.values.sortedBy { it.sortOrder }
}
