package com.example.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

interface CartesiaApiService {

    @GET("v1/voices")
    suspend fun getVoices(
        @Header("X-API-Key") apiKey: String,
        @Header("Cartesia-Version") version: String = "2024-06-10"
    ): Response<ResponseBody>

    @Multipart
    @POST("v1/audio/speech-to-speech")
    @Streaming
    suspend fun speechToSpeech(
        @Header("X-API-Key") apiKey: String,
        @Header("Cartesia-Version") version: String = "2024-06-10",
        @Part audio: MultipartBody.Part,
        @Part("model_id") modelId: RequestBody,
        @Part("voice_id") voiceId: RequestBody,
        @Part("output_format") outputFormat: RequestBody
    ): Response<ResponseBody>
}
