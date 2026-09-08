package com.shifenmiao.database.recordcenter.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_record",
    indices = [
        Index(value = ["happened_at"]),
        Index(value = ["type"])
    ]
)
data class HealthRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "happened_at") val happenedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "fields_json") val fieldsJson: String = "{}",
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
