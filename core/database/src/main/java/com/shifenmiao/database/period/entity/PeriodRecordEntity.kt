package com.shifenmiao.database.period.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "period_record",
    indices = [
        Index(value = ["record_date"]),
        Index(value = ["is_period_start"]),
    ]
)
data class PeriodRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    /** LocalDate 存 epochDay */
    @ColumnInfo(name = "record_date") val recordDate: Long,
    @ColumnInfo(name = "is_period_start") val isPeriodStart: Boolean = false,
    @ColumnInfo(name = "is_period_end") val isPeriodEnd: Boolean = false,
    /** NONE / LIGHT / MEDIUM / HEAVY */
    @ColumnInfo(name = "flow_intensity") val flowIntensity: String = "NONE",
    /** 逗号分隔的症状枚举名，空串 = 无症状 */
    @ColumnInfo(name = "symptoms") val symptoms: String = "",
    /** VERY_HAPPY / HAPPY / NORMAL / SAD / VERY_SAD */
    @ColumnInfo(name = "mood") val mood: String = "NORMAL",
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
