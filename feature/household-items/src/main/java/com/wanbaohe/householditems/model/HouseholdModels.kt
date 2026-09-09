package com.wanbaohe.householditems.model

import java.time.LocalDate

/** 保质期状态 */
enum class ExpiryStatus {
    /** 已过期 */
    EXPIRED,

    /** 30 天内到期 */
    EXPIRING_SOON,

    /** 正常(超过 30 天才到期) */
    FRESH,

    /** 无保质期 */
    NONE,
}

data class HouseholdItemUi(
    val id: String,
    val name: String,
    val category: String,
    val locationId: String?,
    val locationPath: String,
    val expireDate: LocalDate?,
    val photoPath: String?,
    val note: String,
    val expiryStatus: ExpiryStatus,
    /** 距到期天数(负数 = 已过期天数),无保质期为 null */
    val daysToExpire: Long?,
)

data class HouseholdLocationUi(
    val id: String,
    val name: String,
    val parentId: String?,
)

/** 位置树节点 */
data class LocationTreeNode(
    val location: HouseholdLocationUi,
    val depth: Int,
    val children: List<LocationTreeNode>,
)

/** 把扁平位置列表组装成树(按 sortOrder/创建序已排好) */
fun buildLocationTree(locations: List<HouseholdLocationUi>): List<LocationTreeNode> {
    fun childrenOf(parentId: String?, depth: Int): List<LocationTreeNode> {
        return locations.filter { it.parentId == parentId }.map { location ->
            LocationTreeNode(
                location = location,
                depth = depth,
                children = childrenOf(location.id, depth + 1),
            )
        }
    }
    return childrenOf(null, 0)
}

/** 树 → 带缩进深度的扁平列表(位置选择器/管理用) */
fun flattenLocationTree(tree: List<LocationTreeNode>): List<LocationTreeNode> {
    val result = mutableListOf<LocationTreeNode>()
    fun walk(nodes: List<LocationTreeNode>) {
        nodes.forEach { node ->
            result += node
            walk(node.children)
        }
    }
    walk(tree)
    return result
}

/** 沿 parentId 拼出从根到目标位置的路径 */
fun locationPathOf(locations: List<HouseholdLocationUi>, locationId: String?): List<HouseholdLocationUi> {
    if (locationId == null) return emptyList()
    val segments = ArrayDeque<HouseholdLocationUi>()
    val visited = mutableSetOf<String>()
    var current = locations.firstOrNull { it.id == locationId }
    while (current != null && visited.add(current!!.id)) {
        segments.addFirst(current!!)
        current = locations.firstOrNull { it.id == current!!.parentId }
    }
    return segments.toList()
}

data class HouseholdItemsUiState(
    val searchQuery: String = "",
    val locations: List<HouseholdLocationUi> = emptyList(),
    val locationTree: List<LocationTreeNode> = emptyList(),
    /** 当前搜索/全部物品(带位置路径与保质期状态) */
    val items: List<HouseholdItemUi> = emptyList(),
    val expiredItems: List<HouseholdItemUi> = emptyList(),
    val expiringSoonItems: List<HouseholdItemUi> = emptyList(),
    /** 当前浏览的位置层级,null = 根 */
    val currentLocationId: String? = null,

    // ── 物品编辑表单 ──
    val showItemEditor: Boolean = false,
    val editingItemId: String? = null,
    val editorName: String = "",
    val editorCategory: String = "",
    val editorLocationId: String? = null,
    val editorExpireDate: LocalDate? = null,
    /** 新选的待导入图片(content uri 字符串) */
    val editorPhotoUri: String? = null,
    /** 已有的图片文件路径;编辑时移除图片后置 null */
    val editorPhotoPath: String? = null,
    val editorNote: String = "",

    // ── 位置管理 ──
    val showLocationManager: Boolean = false,
    /** 位置删除被阻止(有子级/物品)时置 true,由对话框消费后清除 */
    val locationDeleteBlocked: Boolean = false,
)
