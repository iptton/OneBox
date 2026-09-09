package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 睡眠记录 */
object SleepDurationRecordType : RecordTypeDefinition {
    override val key: String = "sleep_duration"
    override val titleRes: Int = R.string.record_center_type_sleep_duration_title
    override val descriptionRes: Int = R.string.record_center_type_sleep_duration_desc
    override val icon: ImageVector = Icons.Filled.Bedtime
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_sleep_duration_value,
            unit = "h",
            min = 0f,
            max = 24f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_sleep_duration
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 110
}
