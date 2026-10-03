package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EmailGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailGroupDao {
    @Query("SELECT * FROM email_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<EmailGroup>>

    @Query("SELECT * FROM email_groups WHERE id = :id LIMIT 1")
    fun getGroupById(id: Long): Flow<EmailGroup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: EmailGroup): Long

    @Update
    suspend fun updateGroup(group: EmailGroup)

    @Delete
    suspend fun deleteGroup(group: EmailGroup)

    @Query("DELETE FROM email_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}
