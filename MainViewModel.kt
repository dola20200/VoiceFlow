package com.example.ui.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.VoiceMappingEntity
import com.example.data.model.CartesiaVoice
import com.example.data.model.SavedApiKey
import com.example.data.repository.CartesiaRepository
import com.example.service.AudioPlayer
import com.example.service.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface ConversionState {
    object Idle : ConversionState
    data class Progress(val phase: String) : ConversionState
    data class Success(val file: File) : ConversionState
    data class Error(val message: String) : ConversionState
}

class MainViewModel(
    private val context: Context,
    private val repository: CartesiaRepository
) : ViewModel() {

    private val audioRecorder = AudioRecorder(context)
    private val audioPlayer = AudioPlayer(context)

    // Auth
    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _apiKey = MutableStateFlow(repository.getApiKey())
    val apiKey = _apiKey.asStateFlow()

    private val _savedApiKeys = MutableStateFlow(repository.getSavedApiKeys())
    val savedApiKeys = _savedApiKeys.asStateFlow()

    private val _activeApiKeyId = MutableStateFlow(repository.getActiveKeyId())
    val activeApiKeyId = _activeApiKeyId.asStateFlow()

    // Voices & Mappings
    private val _myVoices = MutableStateFlow<List<CartesiaVoice>>(emptyList())
    val myVoices = _myVoices.asStateFlow()

    private val _isLoadingVoices = MutableStateFlow(false)
    val isLoadingVoices = _isLoadingVoices.asStateFlow()

    val voiceMappings: StateFlow<List<VoiceMappingEntity>> = repository.allMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected audio source
    private val _selectedAudioFile = MutableStateFlow<File?>(null)
    val selectedAudioFile = _selectedAudioFile.asStateFlow()

    private val _audioSourceType = MutableStateFlow<String?>(null) // "recorded" or "uploaded"
    val audioSourceType = _audioSourceType.asStateFlow()

    // Recorder State
    val isRecording = audioRecorder.isRecording
    val recordingDuration = audioRecorder.recordingDuration
    val recordingAmplitude = audioRecorder.amplitude

    // Conversion Status
    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState = _conversionState.asStateFlow()

    // Playback State
    val isPlayingResult = audioPlayer.isPlaying
    val resultPlaybackPosition = audioPlayer.currentPosition
    val resultPlaybackDuration = audioPlayer.duration
    val activePlayingFile = audioPlayer.activeFile

    // Settings
    private val _downloadFolder = MutableStateFlow(repository.getDownloadFolder())
    val downloadFolder = _downloadFolder.asStateFlow()

    // Dialog state for mapping setup
    private val _promptNewMapping = MutableStateFlow<CartesiaVoice?>(null)
    val promptNewMapping = _promptNewMapping.asStateFlow()

    // Screen errors
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        if (_isLoggedIn.value) {
            refreshVoices()
        }
    }

    fun isDemoMode(): Boolean {
        return false
    }

    fun validateAndAddKey(key: String, alias: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = repository.validateAndAddApiKey(key, alias)
            if (result.isSuccess) {
                _savedApiKeys.value = repository.getSavedApiKeys()
                _activeApiKeyId.value = repository.getActiveKeyId()
                _apiKey.value = repository.getApiKey()
                _isLoggedIn.value = repository.isLoggedIn()
                refreshVoices()
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Invalid API Key"
                onFailure(error)
            }
        }
    }

    fun switchKey(keyId: String) {
        viewModelScope.launch {
            if (repository.switchApiKey(keyId)) {
                _activeApiKeyId.value = repository.getActiveKeyId()
                _apiKey.value = repository.getApiKey()
                _isLoggedIn.value = repository.isLoggedIn()
                refreshVoices()
            }
        }
    }

    fun deleteKey(keyId: String) {
        viewModelScope.launch {
            if (repository.deleteApiKey(keyId)) {
                _savedApiKeys.value = repository.getSavedApiKeys()
                _activeApiKeyId.value = repository.getActiveKeyId()
                _apiKey.value = repository.getApiKey()
                _isLoggedIn.value = repository.isLoggedIn()
                if (repository.isLoggedIn()) {
                    refreshVoices()
                } else {
                    _myVoices.value = emptyList()
                }
            }
        }
    }

    fun logout() {
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        repository.logout()
        _apiKey.value = null
        _savedApiKeys.value = emptyList()
        _activeApiKeyId.value = null
        _isLoggedIn.value = false
        _myVoices.value = emptyList()
        _selectedAudioFile.value = null
        _conversionState.value = ConversionState.Idle
    }

    fun refreshVoices() {
        viewModelScope.launch {
            _isLoadingVoices.value = true
            _errorMessage.value = null
            try {
                val voices = repository.fetchMyVoices()
                _myVoices.value = voices

                // Check for newly synced voices that have no mappings yet
                checkNewVoicesForMappings(voices)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to sync voices"
                Log.e("MainViewModel", "Sync error", e)
            } finally {
                _isLoadingVoices.value = false
            }
        }
    }

    private fun checkNewVoicesForMappings(voices: List<CartesiaVoice>) {
        viewModelScope.launch {
            val currentMappings = voiceMappings.value.map { it.voiceId }.toSet()
            // Find the first voice that has no mapped output name
            val unmappedVoice = voices.firstOrNull { it.id !in currentMappings }
            if (unmappedVoice != null) {
                _promptNewMapping.value = unmappedVoice
            }
        }
    }

    fun setVoiceMapping(voiceId: String, voiceName: String, outputName: String) {
        viewModelScope.launch {
            repository.saveMapping(voiceId, voiceName, outputName)
            _promptNewMapping.value = null
            // Check if there are any other unmapped voices
            checkNewVoicesForMappings(_myVoices.value)
        }
    }

    fun dismissMappingPrompt() {
        _promptNewMapping.value = null
    }

    fun startRecording() {
        audioPlayer.stop()
        audioRecorder.startRecording()
    }

    fun stopRecording() {
        val file = audioRecorder.stopRecording()
        if (file != null && file.exists()) {
            _selectedAudioFile.value = file
            _audioSourceType.value = "recorded"
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
    }

    fun selectUploadedAudio(uri: Uri) {
        audioPlayer.stop()
        viewModelScope.launch {
            try {
                // Copy selected stream to a local cache file to feed Cartesia API
                val contentResolver = context.contentResolver
                val extension = "wav" // Default, we support standard mime
                val tempFile = File(context.cacheDir, "uploaded_source_${System.currentTimeMillis()}.$extension")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    _selectedAudioFile.value = tempFile
                    _audioSourceType.value = "uploaded"
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to open or load selected audio file."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error picking file: ${e.localizedMessage}"
            }
        }
    }

    fun clearSelectedAudio() {
        audioPlayer.stop()
        _selectedAudioFile.value?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        _selectedAudioFile.value = null
        _audioSourceType.value = null
        _conversionState.value = ConversionState.Idle
    }

    fun convert(voiceId: String, voiceName: String) {
        val audio = _selectedAudioFile.value
        if (audio == null || !audio.exists()) {
            _errorMessage.value = "Please record or upload an audio file first"
            return
        }

        viewModelScope.launch {
            _errorMessage.value = null
            
            // Get mapping for this voice
            val mapped = voiceMappings.value.firstOrNull { it.voiceId == voiceId }
            val outputName = mapped?.outputName ?: "voice_output_${voiceId}"

            _conversionState.value = ConversionState.Progress("Uploading...")

            try {
                val result = repository.convertVoice(
                    audioFile = audio,
                    voiceId = voiceId,
                    outputFileName = outputName
                ) { phase ->
                    _conversionState.value = ConversionState.Progress(phase)
                }
                _conversionState.value = ConversionState.Success(result)
            } catch (e: Exception) {
                _conversionState.value = ConversionState.Error(e.localizedMessage ?: "Conversion failed")
                _errorMessage.value = e.localizedMessage ?: "Conversion failed"
            }
        }
    }

    fun playResult(file: File) {
        audioPlayer.play(file)
    }

    fun pauseResult() {
        audioPlayer.pause()
    }

    fun clearCache() {
        viewModelScope.launch {
            audioPlayer.stop()
            try {
                val cacheDir = context.cacheDir
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("recorded_") || file.name.startsWith("uploaded_")) {
                        file.delete()
                    }
                }
                _selectedAudioFile.value = null
                _audioSourceType.value = null
                _conversionState.value = ConversionState.Idle
                _errorMessage.value = null
                Log.d("MainViewModel", "Cache cleared successfully")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clear cache", e)
            }
        }
    }

    fun setDownloadFolder(path: String) {
        repository.setDownloadFolder(path)
        _downloadFolder.value = path
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.cancelRecording()
    }
}

class MainViewModelFactory(
    private val context: Context,
    private val repository: CartesiaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
