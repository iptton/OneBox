package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 体重记录 */
object WeightRecordType : RecordTypeDefinition {
    override val key: String = "weight"
    override val titleRes: Int = R.string.record_center_type_weight_title
    override val descriptionRes: Int = R.string.record_center_type_weight_desc
    override val icon: ImageVector = Icons.Filled.MonitorWeight
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_weight_value,
            unit = "kg",
            min = 10f,
            max = 500f,
        ),
    )
    override val referenceRangeRes: Int? = null
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 40
}
