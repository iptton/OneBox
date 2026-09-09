package com.wanbaohe.schedule.ai.tool

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日程工具的时间解析/格式化纯函数。
 *
 * 解析容错顺序:带时区偏移的 ISO → 本地 ISO 日期时间 → 纯日期(当天 00:00)。
 * 全部按系统默认时区换算为 UTC 毫秒。
 */

/** 把 LLM 传入的 ISO 日期/日期时间字符串解析为 UTC 毫秒,无法解析时返回 null */
internal fun parseIsoToUtcMillis(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val zone = ZoneId.systemDefault()
    runCatching { return OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }
    runCatching { return LocalDateTime.parse(trimmed).atZone(zone).toInstant().toEpochMilli() }
    runCatching { return LocalDate.parse(trimmed).atStartOfDay(zone).toInstant().toEpochMilli() }
    return null
}

/** 纯日期(yyyy-MM-dd)解析为 LocalDate,无法解析时返回 null */
internal fun parseIsoDate(text: String): LocalDate? {
    return runCatching { LocalDate.parse(text.trim()) }.getOrNull()
}

/** UTC 毫秒格式化为 `yyyy-MM-dd HH:mm`(系统默认时区) */
internal fun formatUtcMillis(utcMillis: Long): String {
    return Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneId.systemDefault())
        .format(DATE_TIME_FORMATTER)
}

private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
