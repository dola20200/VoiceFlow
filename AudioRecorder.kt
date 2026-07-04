package com.example.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDuration = MutableStateFlow(0L) // milliseconds
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private val _amplitude = MutableStateFlow(0f) // 0.0 to 1.0
    val amplitude: StateFlow<Float> = _amplitude

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!_isRecording.value) return
            val elapsed = System.currentTimeMillis() - startTime
            _recordingDuration.value = elapsed

            // Get max amplitude
            mediaRecorder?.let {
                try {
                    val maxAmp = it.maxAmplitude
                    // Normalize amplitude roughly (max is typically 32767)
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    _amplitude.value = normalized
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Error getting amplitude: ${e.localizedMessage}")
                }
            }

            handler.postDelayed(this, 100)
        }
    }

    @Suppress("DEPRECATION")
    fun startRecording(): File? {
        if (_isRecording.value) return null

        val cacheDir = context.externalCacheDir ?: context.cacheDir
        outputFile = File(cacheDir, "recorded_voice_input_${System.currentTimeMillis()}.m4a")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile!!.absolutePath)

            try {
                prepare()
                start()
                _isRecording.value = true
                startTime = System.currentTimeMillis()
                handler.post(updateRunnable)
                Log.d("AudioRecorder", "Recording started: ${outputFile?.absolutePath}")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "Failed to start recording", e)
                outputFile = null
            }
        }

        return outputFile
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null

        handler.removeCallbacks(updateRunnable)
        _isRecording.value = false
        _amplitude.value = 0f
        _recordingDuration.value = 0L

        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
        }

        Log.d("AudioRecorder", "Recording stopped: ${outputFile?.absolutePath}")
        return outputFile
    }

    fun cancelRecording() {
        if (!_isRecording.value) return
        stopRecording()
        outputFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        outputFile = null
    }
}
