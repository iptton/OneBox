package com.wanbaohe.lifetime.ai.tool

import java.time.LocalDate

/**
 * 人生时间线工具的日期解析纯函数。
 *
 * 只接受纯日期(yyyy-MM-dd):倒数日与里程碑都以「天」为粒度,不需要时间部分。
 */

/** 纯日期(yyyy-MM-dd)解析为 LocalDate,无法解析时返回 null */
internal fun parseIsoDate(text: String): LocalDate? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}
