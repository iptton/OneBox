package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** BMI 记录 */
object BmiRecordType : RecordTypeDefinition {
    override val key: String = "bmi"
    override val titleRes: Int = R.string.record_center_type_bmi_title
    override val descriptionRes: Int = R.string.record_center_type_bmi_desc
    override val icon: ImageVector = Icons.Filled.Straighten
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_bmi_value,
            unit = "",
            min = 10f,
            max = 80f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_bmi
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 50
}
