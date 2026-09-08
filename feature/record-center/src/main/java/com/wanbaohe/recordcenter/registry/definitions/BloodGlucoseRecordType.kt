package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 血糖记录 */
object BloodGlucoseRecordType : RecordTypeDefinition {
    override val key: String = "blood_glucose"
    override val titleRes: Int = R.string.record_center_type_blood_glucose_title
    override val descriptionRes: Int = R.string.record_center_type_blood_glucose_desc
    override val icon: ImageVector = Icons.Filled.WaterDrop
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_glucose_value,
            unit = "mmol/L",
            min = 1.0f,
            max = 35.0f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_blood_glucose
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 30
}
