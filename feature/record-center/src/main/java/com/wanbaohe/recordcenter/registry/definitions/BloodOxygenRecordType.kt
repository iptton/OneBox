package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 血氧记录 */
object BloodOxygenRecordType : RecordTypeDefinition {
    override val key: String = "blood_oxygen"
    override val titleRes: Int = R.string.record_center_type_blood_oxygen_title
    override val descriptionRes: Int = R.string.record_center_type_blood_oxygen_desc
    override val icon: ImageVector = Icons.Filled.Air
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_blood_oxygen_value,
            unit = "%",
            min = 70f,
            max = 100f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_blood_oxygen
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 90
}
