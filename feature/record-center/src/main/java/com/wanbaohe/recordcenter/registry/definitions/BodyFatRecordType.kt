package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 体脂记录 */
object BodyFatRecordType : RecordTypeDefinition {
    override val key: String = "body_fat"
    override val titleRes: Int = R.string.record_center_type_body_fat_title
    override val descriptionRes: Int = R.string.record_center_type_body_fat_desc
    override val icon: ImageVector = Icons.Filled.Percent
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_body_fat_value,
            unit = "%",
            min = 1f,
            max = 70f,
        ),
    )
    override val referenceRangeRes: Int? = null
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 60
}
