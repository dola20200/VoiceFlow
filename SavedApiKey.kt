package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedApiKey(
    @Json(name = "id") val id: String,
    @Json(name = "alias") val alias: String,
    @Json(name = "key") val key: String
)
