package com.wanbaohe.period.model

/** 流量强度 */
enum class PeriodFlow {
    NONE,
    LIGHT,
    MEDIUM,
    HEAVY;

    companion object {
        fun fromName(name: String?): PeriodFlow =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
    }
}

/** 心情五档 */
enum class PeriodMood {
    VERY_HAPPY,
    HAPPY,
    NORMAL,
    SAD,
    VERY_SAD;

    companion object {
        fun fromName(name: String?): PeriodMood =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
}

/** 常见症状（与设计稿 chips 一致） */
enum class PeriodSymptom {
    ABDOMINAL_PAIN,
    HEADACHE,
    BACK_PAIN,
    MOOD_SWINGS,
    BREAST_TENDERNESS,
    FATIGUE,
    NAUSEA,
    INSOMNIA,
    APPETITE_CHANGE,
    ACNE,
    EDEMA;

    companion object {
        fun fromName(name: String?): PeriodSymptom? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

        fun parse(csv: String?): List<PeriodSymptom> {
            if (csv.isNullOrBlank()) return emptyList()
            return csv.split(',')
                .mapNotNull { fromName(it.trim()) }
                .distinct()
        }

        fun toCsv(items: Collection<PeriodSymptom>): String =
            items.joinToString(",") { it.name }
    }
}

/** 列表页底部 tab */
enum class PeriodTab {
    RECORDS,
    STATS,
}

/** 统计页时间范围 */
enum class PeriodStatsRange {
    SIX_MONTHS,
    DAY,
    WEEK,
    MONTH,
}

/** 记录卡片右侧状态 */
enum class PeriodStatus {
    PERIOD,
    OVULATION,
    NORMAL,
}
