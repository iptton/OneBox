package com.shifenmiao.database.household.model

import com.shifenmiao.database.household.entity.HouseholdItemEntity

data class HouseholdItemWithPath(
    val item: HouseholdItemEntity,
    val locationPath: String,
)
