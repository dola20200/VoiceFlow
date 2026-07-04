package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_mappings ORDER BY lastUsed DESC")
    fun getAllMappings(): Flow<List<VoiceMappingEntity>>

    @Query("SELECT * FROM voice_mappings WHERE voiceId = :voiceId LIMIT 1")
    suspend fun getMappingById(voiceId: String): VoiceMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: VoiceMappingEntity)

    @Query("DELETE FROM voice_mappings WHERE voiceId = :voiceId")
    suspend fun deleteMapping(voiceId: String)

    @Query("DELETE FROM voice_mappings")
    suspend fun clearAll()
}
