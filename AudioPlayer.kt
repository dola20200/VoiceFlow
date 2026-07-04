package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L) // ms
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L) // ms
    val duration: StateFlow<Long> = _duration

    private val _activeFile = MutableStateFlow<File?>(null)
    val activeFile: StateFlow<File?> = _activeFile

    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    _currentPosition.value = it.currentPosition.toLong()
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun play(file: File) {
        if (currentFile == file && mediaPlayer != null) {
            resume()
            return
        }

        stop()
        currentFile = file
        _activeFile.value = file

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                _duration.value = duration.toLong()
                start()
                _isPlaying.value = true
                handler.post(progressRunnable)

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    handler.removeCallbacks(progressRunnable)
                    Log.d("AudioPlayer", "Playback completed")
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to prepare media player", e)
                stop()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                handler.removeCallbacks(progressRunnable)
            }
        }
    }

    private fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                handler.post(progressRunnable)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun stop() {
        handler.removeCallbacks(progressRunnable)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
        currentFile = null
        _activeFile.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }
}
