package com.wanbaohe.recordcenter.model

import kotlinx.serialization.json.Json
import java.math.BigDecimal

/**
 * fieldsJson 编解码 — HealthRecordEntity.fieldsJson 是 Map<String, Float> 的 JSON。
 *
 * 解析失败一律回退空 map,不让脏数据弄崩读取链路。
 */
object RecordFieldsCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(values: Map<String, Float>): String {
        return json.encodeToString(values)
    }

    fun decode(fieldsJson: String): Map<String, Float> {
        return runCatching {
            json.decodeFromString<Map<String, Float>>(fieldsJson)
        }.getOrDefault(emptyMap())
    }

    /** 数值展示:去掉多余的尾零(120.0 → "120",6.25 → "6.25") */
    fun formatValue(value: Float): String {
        return BigDecimal(value.toDouble()).stripTrailingZeros().toPlainString()
    }
}
