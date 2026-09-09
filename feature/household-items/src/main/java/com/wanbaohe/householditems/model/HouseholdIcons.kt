package com.wanbaohe.householditems.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.icons.line.LineCatBulky
import com.t8rin.imagetoolbox.core.resources.icons.line.LineCatMedicine
import com.t8rin.imagetoolbox.core.resources.icons.line.LineCatSmallStuff
import com.t8rin.imagetoolbox.core.resources.icons.line.LineFolderCustom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocBalcony
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocBathroom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocHome
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocKitchen
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocLivingRoom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocMasterBedroom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocSecondBedroom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocStorageRoom
import com.t8rin.imagetoolbox.core.resources.icons.line.LineLocStudy
import com.wanbaohe.householditems.R

/**
 * 位置图标映射:9 个预置 id → 专用图标,用户自建位置 → [LineFolderCustom]。
 */
fun locationIcon(locationId: String): ImageVector = when (locationId) {
    "loc_home" -> Icons.Outlined.LineLocHome
    "loc_living_room" -> Icons.Outlined.LineLocLivingRoom
    "loc_master_bedroom" -> Icons.Outlined.LineLocMasterBedroom
    "loc_second_bedroom" -> Icons.Outlined.LineLocSecondBedroom
    "loc_kitchen" -> Icons.Outlined.LineLocKitchen
    "loc_bathroom" -> Icons.Outlined.LineLocBathroom
    "loc_study" -> Icons.Outlined.LineLocStudy
    "loc_balcony" -> Icons.Outlined.LineLocBalcony
    "loc_storage_room" -> Icons.Outlined.LineLocStorageRoom
    else -> Icons.Outlined.LineFolderCustom
}

/**
 * 物品分类图标:分类存的是本地化文本(见 ItemEditSheet 预设 chips),
 * 与当前 locale 的三个预设值比较,匹配不上返回 null(调用方回退首字占位)。
 */
@Composable
fun categoryIconOrNull(category: String): ImageVector? {
    if (category.isBlank()) return null
    val medicine = stringResource(R.string.household_category_medicine)
    val small = stringResource(R.string.household_category_small)
    val big = stringResource(R.string.household_category_big)
    return when (category) {
        medicine -> Icons.Outlined.LineCatMedicine
        small -> Icons.Outlined.LineCatSmallStuff
        big -> Icons.Outlined.LineCatBulky
        else -> null
    }
}
