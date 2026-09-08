package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 尿酸记录 */
object UricAcidRecordType : RecordTypeDefinition {
    override val key: String = "uric_acid"
    override val titleRes: Int = R.string.record_center_type_uric_acid_title
    override val descriptionRes: Int = R.string.record_center_type_uric_acid_desc
    override val icon: ImageVector = Icons.Filled.Science
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_uric_acid_value,
            unit = "μmol/L",
            min = 50f,
            max = 1500f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_uric_acid
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 70
}
