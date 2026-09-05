package com.jarvis.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(message: ConversationEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
