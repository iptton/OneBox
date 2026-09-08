package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 心率记录 */
object HeartRateRecordType : RecordTypeDefinition {
    override val key: String = "heart_rate"
    override val titleRes: Int = R.string.record_center_type_heart_rate_title
    override val descriptionRes: Int = R.string.record_center_type_heart_rate_desc
    override val icon: ImageVector = Icons.Filled.MonitorHeart
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_heart_rate_value,
            unit = "bpm",
            min = 30f,
            max = 220f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_heart_rate
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 10
}
