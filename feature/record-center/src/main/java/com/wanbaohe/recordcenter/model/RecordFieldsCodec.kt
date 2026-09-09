package com.wanbaohe.recordcenter.model

import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

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

    /**
     * 数值展示:去掉多余的尾零(120.0 → "120",6.25 → "6.25")。
     *
     * 必须经 [Float.toString] 拿最短十进制表示再转 BigDecimal;
     * 直接 BigDecimal(value.toDouble()) 会把 Float 拓宽成 Double,
     * 暴露二进制尾数(5.6f → "5.599999904632568359375")。
     */
    fun formatValue(value: Float): String {
        return BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
    }

    /**
     * 统计值展示(平均/最高/最低等):先按 2 位小数 HALF_UP 舍入,再去尾零。
     * 例如 5.6 与 5.7 的平均显示 "5.65",1/3 类除不尽的平均显示 "1.67"。
     */
    fun formatStatsValue(value: Float): String {
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}
