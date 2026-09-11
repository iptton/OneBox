package com.shifenmiao.database.decision_wheel.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 转盘 + 选项的关联查询结果。
 *
 * 存在的唯一理由：**让 Room 同时监听 `decision_wheels` 和 `wheel_options` 两张表。**
 *
 * 之前 `getAllWheels()` 只查 `decision_wheels`，再在 map 里用挂起函数单独取选项。
 * Room 的失效追踪只认 SQL 里出现的表，所以 `UPDATE wheel_options SET enabled = ...`
 * （抽后移除 / 全部恢复）不会触发重新发射 —— 表现就是：抽完确实移除了（因为紧随其后的
 * `updateWheelUsage` 改了主表，顺带刷了一次），但"全部恢复"点了完全没反应。
 * 带 `@Relation` 的 `@Transaction` 查询会被 Room 展开成对父子表的联合监听，这里才能收得到。
 */
data class WheelWithOptions(
    @Embedded
    val wheel: WheelEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "wheelId"
    )
    val options: List<WheelOptionEntity>
)
