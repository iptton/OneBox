package com.wanbaohe.recordcenter.registry.definitions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.registry.RecordField
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition

/** 体温记录 */
object BodyTemperatureRecordType : RecordTypeDefinition {
    override val key: String = "body_temperature"
    override val titleRes: Int = R.string.record_center_type_body_temperature_title
    override val descriptionRes: Int = R.string.record_center_type_body_temperature_desc
    override val icon: ImageVector = Icons.Filled.Thermostat
    override val fields: List<RecordField> = listOf(
        RecordField(
            key = "value",
            labelRes = R.string.record_center_field_body_temperature_value,
            unit = "°C",
            min = 34f,
            max = 43f,
        ),
    )
    override val referenceRangeRes: Int = R.string.record_center_ref_body_temperature
    override val chartFieldKeys: List<String> = listOf("value")
    override val sortOrder: Int = 100
}
