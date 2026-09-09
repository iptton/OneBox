package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 腰围记录 */
object WaistCircumferenceRecordType : RecordTypeDefinition {
    override val key: String = "waist_circumference"
    override val titleRes: Int = R.string.record_center_type_waist_circumference_title
    override val descriptionRes: Int = R.string.record_center_type_waist_circumference_desc
    override val icon: ImageVector = Icons.Filled.AccessibilityNew
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_waist_circumference_value,
            unit = "cm",
            min = 30f,
            max = 300f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_waist_circumference
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 120
}
