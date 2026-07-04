package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_mappings")
data class VoiceMappingEntity(
    @PrimaryKey val voiceId: String,
    val voiceName: String,
    val outputName: String,
    val lastUsed: Long = System.currentTimeMillis()
)
