package com.wanbaohe.recordcenter.screen.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanbaohe.recordcenter.R
import com.wanbaohe.recordcenter.model.RecordFieldsCodec
import com.wanbaohe.recordcenter.registry.RecordTypeDefinition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")
private val chartDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

private fun millisToDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * 记录数值的紧凑展示,如血压 "120/80 mmHg"、血脂 "5.2 mmol/L …"。
 * 全部字段单位一致时合并为一个单位后缀,否则逐字段带单位。
 */
@Composable
fun formatRecordValues(definition: RecordTypeDefinition, fieldsJson: String): String {
    val values = RecordFieldsCodec.decode(fieldsJson)
    val present = definition.fields.filter { values.containsKey(it.key) }
    if (present.isEmpty()) return ""
    val units = present.map { it.unit }.distinct()
    return if (units.size == 1) {
        val joined = present.joinToString("/") { RecordFieldsCodec.formatValue(values.getValue(it.key)) }
        if (units.first().isEmpty()) joined else "$joined ${units.first()}"
    } else {
        present.joinToString("  ") { field ->
            val value = RecordFieldsCodec.formatValue(values.getValue(field.key))
            if (field.unit.isEmpty()) value else "$value ${field.unit}"
        }
    }
}

/** 相对日期:今天 / 昨天 / N天前 / 具体日期 */
@Composable
fun formatRelativeDate(millis: Long): String {
    val date = millisToDate(millis)
    val days = ChronoUnit.DAYS.between(date, LocalDate.now()).toInt()
    return when {
        days <= 0 -> stringResource(R.string.record_center_today)
        days == 1 -> stringResource(R.string.record_center_yesterday)
        days < 30 -> stringResource(R.string.record_center_days_ago, days)
        else -> date.format(dateFormatter)
    }
}

/** 图表 X 轴标签:M/d */
fun formatChartDate(millis: Long): String = millisToDate(millis).format(chartDateFormatter)

/** 列表行时间:yyyy/M/d HH:mm */
fun formatRecordDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
