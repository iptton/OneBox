package com.shifenmiao.database.ai

/**
 * 技能使用频次与清单展示策略（feature/ai 与 feature/settings 共用）。
 */
object SkillUsagePolicy {

    /** prompt 清单最多列出的技能数 */
    const val MAX_LISTED_SKILLS = 20

    /** use_count 超过该值时触发全体归一化到 0–100 */
    const val NORMALIZE_THRESHOLD = 1000.0

    /** 清单遴选时"近期更新"的时间窗 */
    const val RECENT_UPDATE_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    /** name/description 进 prompt 前的字符上限（防 `</available_skills>` 逃逸） */
    const val MAX_META_CHARS = 200

    /** 技能清单占 prompt 预算的比例上限 */
    const val LIST_BUDGET_FRACTION = 0.10

    /** 使用频次分档（阈值照 OpenMinis：20 / 60） */
    enum class UsageFrequency { NEVER, LOW, REGULAR, HIGH }

    fun frequencyOf(useCount: Double): UsageFrequency = when {
        useCount <= 0.0 -> UsageFrequency.NEVER
        useCount < 20.0 -> UsageFrequency.LOW
        useCount < 60.0 -> UsageFrequency.REGULAR
        else -> UsageFrequency.HIGH
    }
}
