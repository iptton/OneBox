package com.wanbaohe.dsh.wire.model

import kotlinx.serialization.Serializable

/**
 * 模型选择面 wire 模型(DSH 0.1.5-rc.2 `session/modelCatalog` / `session/selectModel`)。
 *
 * 与旧版(<= 0.1.1)的差异:旧版是 `session.models` 回 `{current, routable, groups, failures}`,
 * 新版合并成"目录 + 默认选择 + 可路由 provider 列表":
 * [ModelCatalog.default] 取代 current,[ModelCatalog.routableProviders] 取代单个 routable 布尔。
 * 选择仍可与目录成员无关(服务端语义,目录只是展示面);provider 不在 routableProviders 里时,
 * prompt 前服务端会回 `session/model-unavailable`,UI 只警示不拦截。
 */

/** 当前选择(provider + model + 可选推理力度) */
@Serializable
data class ModelSelection(
    val provider: String,
    val model: String,
    val reasoningEffort: String? = null
)

/** 推理力度档(如 low/high) */
@Serializable
data class ModelReasoningEffort(
    val id: String,
    val name: String = "",
    val description: String? = null
)

/** 模型的推理能力声明;[defaultEffort] 为主机推荐默认档 */
@Serializable
data class ModelReasoning(
    val efforts: List<ModelReasoningEffort> = emptyList(),
    val defaultEffort: String? = null
)

/** 目录中的单个模型 */
@Serializable
data class ModelCatalogModel(
    val id: String,
    val name: String = "",
    val description: String? = null,
    val reasoning: ModelReasoning? = null
)

/** provider 分组的目录块 */
@Serializable
data class ModelProviderGroup(
    val id: String,
    val name: String = "",
    val models: List<ModelCatalogModel> = emptyList()
)

/** 单个 provider 目录拉取失败(不拖垮整个目录) */
@Serializable
data class ModelCatalogFailure(
    val id: String,
    val name: String = "",
    val message: String = ""
)

/** session/modelCatalog 的响应 value:默认选择 + 可路由 provider + 目录 + 失败项 */
@Serializable
data class ModelCatalog(
    val default: ModelSelection? = null,
    val routableProviders: List<String> = emptyList(),
    val groups: List<ModelProviderGroup> = emptyList(),
    val failures: List<ModelCatalogFailure> = emptyList()
)

/** session/selectModel 的响应 value:回带规范化后的选择 */
@Serializable
data class SessionSelectModelValue(
    val selected: ModelSelection? = null
)

/** 在目录中查找某选择对应的模型条目(选择可与目录成员无关,找不到返回 null) */
fun ModelCatalog.catalogEntryOf(selection: ModelSelection): ModelCatalogModel? {
    for (group in groups) {
        if (group.id != selection.provider) continue
        return group.models.firstOrNull { it.id == selection.model }
    }
    return null
}

/** 某 provider 当前是否可路由(不在名单里 = prompt 前会 model-unavailable,UI 只警示) */
fun ModelCatalog.isRoutable(provider: String?): Boolean =
    provider != null && provider in routableProviders
