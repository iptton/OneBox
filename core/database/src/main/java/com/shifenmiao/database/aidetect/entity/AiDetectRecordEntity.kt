package com.shifenmiao.database.aidetect.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** AI 检测历史记录数据库实体 */
@Entity(
    tableName = "ai_detect_record",
    indices = [Index("createdAt")]
)
data class AiDetectRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 检测类型:"text" / "image" */
    val type: String,
    /** 输入摘要:文本取前 200 字;图片为本地缩略图文件路径 */
    val inputSummary: String,
    /** AI 生成概率(0-1) */
    val probability: Float,
    /** 判定结果,如 "likely_ai" / "maybe_ai" / "likely_human" */
    val verdict: String,
    /** 原始结果 JSON(文本为 segment_labels 等,图片为置信度详情) */
    val detailJson: String = "",
    /** 记录时间戳 (ms) */
    val createdAt: Long = System.currentTimeMillis(),
)
