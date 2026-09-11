package com.shifenmiao.database.decision_wheel.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 转盘配置实体
 */
@Entity(tableName = "decision_wheels")
data class WheelEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastUsedAt: Long = 0L,
    val useCount: Int = 0
)

/**
 * 转盘选项实体
 */
@Entity(
    tableName = "wheel_options",
    indices = [Index(value = ["wheelId"])]
)
data class WheelOptionEntity(
    @PrimaryKey
    val id: String,
    val wheelId: String,
    val name: String,
    val colorHex: String, // 颜色的十六进制字符串
    val position: Int, // 选项在转盘中的位置
    val weight: Float = 1f, // 抽中权重，1 为等概率
    val enabled: Boolean = true // 抽后移除用；false 的扇区不参与抽取
)

/**
 * 转盘使用历史记录
 */
@Entity(
    tableName = "wheel_history",
    indices = [Index(value = ["wheelId"])]
)
data class WheelHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wheelId: String,
    val wheelTitle: String = "", // 冗余存标题：转盘被删除后历史仍有归属
    val selectedOptionId: String,
    val selectedOptionName: String,
    val timestamp: Long
)

