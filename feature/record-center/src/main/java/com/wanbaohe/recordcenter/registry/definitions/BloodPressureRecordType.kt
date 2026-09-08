package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 血压记录 */
object BloodPressureRecordType : RecordTypeDefinition {
    override val key: String = "blood_pressure"
    override val titleRes: Int = R.string.record_center_type_blood_pressure_title
    override val descriptionRes: Int = R.string.record_center_type_blood_pressure_desc
    override val icon: ImageVector = Icons.Filled.Favorite
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "systolic",
            labelRes = R.string.record_center_field_bp_systolic,
            unit = "mmHg",
            min = 60f,
            max = 260f,
        ),
        RecordField(
            key = "diastolic",
            labelRes = R.string.record_center_field_bp_diastolic,
            unit = "mmHg",
            min = 30f,
            max = 180f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_blood_pressure
    override val chartFieldKeys: List<String> = listOf("systolic", "diastolic")
    override val sortOrder: Int = 20
}
