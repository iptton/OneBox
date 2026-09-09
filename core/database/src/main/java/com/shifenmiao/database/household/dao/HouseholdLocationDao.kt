package com.shifenmiao.database.household.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shifenmiao.database.household.entity.HouseholdLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: HouseholdLocationEntity)

    @Query("SELECT * FROM household_location ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<HouseholdLocationEntity>>

    @Query(
        """
        SELECT * FROM household_location
        WHERE (:parentId IS NULL AND parent_id IS NULL) OR parent_id = :parentId
        ORDER BY sort_order ASC, created_at ASC
        """
    )
    fun observeChildren(parentId: String?): Flow<List<HouseholdLocationEntity>>

    @Query("SELECT * FROM household_location WHERE id = :id")
    suspend fun getById(id: String): HouseholdLocationEntity?

    @Query(
        """
        SELECT * FROM household_location
        WHERE name = :name AND ((:parentId IS NULL AND parent_id IS NULL) OR parent_id = :parentId)
        LIMIT 1
        """
    )
    suspend fun getByNameAndParent(name: String, parentId: String?): HouseholdLocationEntity?

    @Query("SELECT COUNT(*) FROM household_location WHERE parent_id = :id")
    suspend fun countChildren(id: String): Int

    @Query("DELETE FROM household_location WHERE id = :id")
    suspend fun delete(id: String)
}
