package com.example.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VoiceMappingEntity
import com.example.data.model.CartesiaVoice
import com.example.ui.theme.*
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Clear error message via Toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CartesiaBackground)
    ) {
        if (!isLoggedIn) {
            ApiKeyFirstLaunchScreen(viewModel = viewModel)
        } else {
            WorkspaceScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyFirstLaunchScreen(viewModel: MainViewModel) {
    var keyInput by remember { mutableStateOf("") }
    var aliasInput by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "Key Icon",
            tint = CartesiaPurpleLight,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to VoiceFlow",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please enter your Cartesia API Key to authenticate and access your cloned voices.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = {
                keyInput = it
                localError = null
            },
            label = { Text("Cartesia API Key") },
            placeholder = { Text("e.g. ca_...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CartesiaPurplePrimary,
                unfocusedBorderColor = CartesiaBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = aliasInput,
            onValueChange = { aliasInput = it },
            label = { Text("Key Alias / Name") },
            placeholder = { Text("e.g. My Personal Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("api_key_alias"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = CartesiaPurplePrimary,
                unfocusedBorderColor = CartesiaBorder
            )
        )

        if (localError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = localError!!,
                color = DangerRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (keyInput.isBlank()) {
                    localError = "API Key cannot be empty"
                    return@Button
                }
                val alias = if (aliasInput.isBlank()) "Default Key" else aliasInput
                isChecking = true
                localError = null
                viewModel.validateAndAddKey(
                    key = keyInput,
                    alias = alias,
                    onSuccess = {
                        isChecking = false
                        Toast.makeText(context, "API Key validated & saved securely!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        isChecking = false
                        localError = error
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("api_key_submit"),
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(containerColor = CartesiaPurplePrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Validate & Authenticate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val voices by viewModel.myVoices.collectAsState()
    val isLoadingVoices by viewModel.isLoadingVoices.collectAsState()
    val mappings by viewModel.voiceMappings.collectAsState()
    val selectedAudio by viewModel.selectedAudioFile.collectAsState()
    val audioSourceType by viewModel.audioSourceType.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()

    var selectedVoice by remember { mutableStateOf<CartesiaVoice?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Mapping setup dialog state
    val promptNewMapping by viewModel.promptNewMapping.collectAsState()

    // File Upload Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.selectUploadedAudio(it) }
    }

    // Settings Sheet Content or standard modal dialog
    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { isSettingsOpen = false }
        )
    }

    // Mapping initialization Dialog (Requirement 2: When a new voice appears for the first time, ask)
    promptNewMapping?.let { voice ->
        VoiceMappingSetupDialog(
            voiceName = voice.name,
            onSave = { outputName -> viewModel.setVoiceMapping(voice.id, voice.name, outputName) },
            onDismiss = { viewModel.dismissMappingPrompt() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VoiceFlow",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (viewModel.isDemoMode()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = CartesiaPurpleDark.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, CartesiaPurpleLight.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "DEMO",
                                    color = CartesiaPurpleLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshVoices() },
                        modifier = Modifier.testTag("refresh_voices_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync My Voices",
                            tint = CartesiaPurpleLight
                        )
                    }
                    IconButton(
                        onClick = { isSettingsOpen = true },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CartesiaBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Top Section: Voices & Mapping list
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Voices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (isLoadingVoices) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CartesiaPurplePrimary)
                    }
                } else if (voices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, CartesiaBorder, RoundedCornerShape(12.dp))
                            .background(CartesiaSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "No voices",
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No personal voices found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Create custom voice mappings in Settings or sync again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(voices) { voice ->
                            val mapping = mappings.find { it.voiceId == voice.id }
                            val outputName = mapping?.outputName ?: "(Not mapped yet)"

                            VoiceRowItem(
                                voice = voice,
                                outputName = outputName,
                                isSelected = selectedVoice?.id == voice.id,
                                onSelect = { selectedVoice = voice },
                                onEditMapping = {
                                    viewModel.setVoiceMapping(voice.id, voice.name, outputName)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Source selection and action panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CartesiaBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CartesiaSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Audio Source",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedAudio == null) {
                        // Requirements 3: Two buttons only: Record Audio, Upload Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isRecordingFlow by viewModel.isRecording.collectAsState()
                            
                            // RECORD AUDIO
                            Button(
                                onClick = {
                                    if (isRecordingFlow) {
                                        viewModel.stopRecording()
                                    } else {
                                        viewModel.startRecording()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("record_audio_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecordingFlow) DangerRed else CartesiaSurfaceVariant,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isRecordingFlow) DangerRed else CartesiaBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRecordingFlow) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Record icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRecordingFlow) "Stop Rec" else "Record Audio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // UPLOAD AUDIO
                            Button(
                                onClick = {
                                    filePickerLauncher.launch("audio/*")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("upload_audio_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CartesiaSurfaceVariant,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, CartesiaBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload Audio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Recording wave visualizer feedback
                        val isRecordingFlow by viewModel.isRecording.collectAsState()
                        if (isRecordingFlow) {
                            val duration by viewModel.recordingDuration.collectAsState()
                            val amplitude by viewModel.recordingAmplitude.collectAsState()
                            RecordingIndicator(duration = duration, amplitude = amplitude)
                        }

                    } else {
                        // Display selected source with clear button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CartesiaBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, CartesiaBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (audioSourceType == "recorded") Icons.Default.Mic else Icons.Default.GraphicEq,
                                    contentDescription = "Audio source icon",
                                    tint = CartesiaPurpleLight
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (audioSourceType == "recorded") "Recorded Capture" else "Uploaded File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = selectedAudio!!.name,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.clearSelectedAudio() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove source",
                                    tint = DangerRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LARGE CONVERT BUTTON (Requirement 4)
                    val activeVoice = selectedVoice
                    val isAudioReady = selectedAudio != null
                    val canConvert = isAudioReady && activeVoice != null

                    if (conversionState is ConversionState.Progress) {
                        val phase = (conversionState as ConversionState.Progress).phase
                        ConversionProgressWidget(phase = phase)
                    } else {
                        Button(
                            onClick = {
                                if (activeVoice != null) {
                                    viewModel.convert(activeVoice.id, activeVoice.name)
                                }
                            },
                            enabled = canConvert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("convert_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CartesiaPurplePrimary,
                                contentColor = TextPrimary,
                                disabledContainerColor = CartesiaBorder,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Conversion icon"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeVoice == null) "Select a Target Voice" else "Change Voice to ${activeVoice.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Result Area (Requirement 5)
                    if (conversionState is ConversionState.Success) {
                        val resultFile = (conversionState as ConversionState.Success).file
                        Spacer(modifier = Modifier.height(16.dp))
                        ResultPlaybackWidget(
                            resultFile = resultFile,
                            viewModel = viewModel
                        )
                    } else if (conversionState is ConversionState.Error) {
                        val err = (conversionState as ConversionState.Error).message
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Error: $err",
                            color = DangerRed,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VoiceRowItem(
    voice: CartesiaVoice,
    outputName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEditMapping: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CartesiaPurpleDark.copy(alpha = 0.3f) else CartesiaSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) CartesiaPurplePrimary else CartesiaBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CartesiaPurplePrimary,
                    unselectedColor = TextMuted
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = voice.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Output file: ",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Text(
                        text = if (outputName.endsWith(".wav")) outputName else "$outputName.wav",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CartesiaPurpleLight
                    )
                }
            }
        }

        IconButton(onClick = onEditMapping) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Mapping",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RecordingIndicator(duration: Long, amplitude: Float) {
    val seconds = (duration / 1000) % 60
    val minutes = (duration / 60000) % 60
    val ms = (duration / 10) % 100

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(CartesiaBackground, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(DangerRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recording mic...",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Pulse wave visually
        Row(
            modifier = Modifier.width(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..7) {
                val height = (amplitude * 24 * (1f + sin(i.toFloat() + System.currentTimeMillis() / 200f))).coerceAtLeast(3f)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(height.dp)
                        .background(CartesiaPurplePrimary, RoundedCornerShape(2.dp))
                )
            }
        }

        Text(
            text = String.format("%02d:%02d.%02d", minutes, seconds, ms),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = DangerRed,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConversionProgressWidget(phase: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Spinner",
                tint = CartesiaPurplePrimary,
                modifier = Modifier
                    .size(24.dp)
                    .drawBehind {
                        // Use rotation in draw scope or modifier
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = phase,
                fontWeight = FontWeight.SemiBold,
                color = CartesiaPurpleLight,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            color = CartesiaPurplePrimary,
            trackColor = CartesiaBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun ResultPlaybackWidget(
    resultFile: File,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlayingResult.collectAsState()
    val progress by viewModel.resultPlaybackPosition.collectAsState()
    val duration by viewModel.resultPlaybackDuration.collectAsState()
    val activePlayingFile by viewModel.activePlayingFile.collectAsState()

    val isActive = activePlayingFile == resultFile

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CartesiaPurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CartesiaPurpleDark.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = CartesiaAccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice change successful!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${(resultFile.length() / 1024)} KB",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Output file: ${resultFile.name}",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Playback progress slider
            if (isActive && duration > 0) {
                Slider(
                    value = progress.toFloat() / duration.toFloat(),
                    onValueChange = {}, // read-only progress display for simplicity
                    colors = SliderDefaults.colors(
                        thumbColor = CartesiaPurplePrimary,
                        activeTrackColor = CartesiaPurpleLight,
                        inactiveTrackColor = CartesiaBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause controls (Requirement 5: Play Result)
                Row {
                    Button(
                        onClick = {
                            if (isPlaying && isActive) {
                                viewModel.pauseResult()
                            } else {
                                viewModel.playResult(resultFile)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CartesiaPurplePrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying && isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying && isActive) "Pause Result" else "Play Result",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying && isActive) "Pause" else "Play Result",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Requirement 5: Save Result (Show folder location path)
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Result auto-saved and overwritten: ${resultFile.name} in Downloads",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CartesiaSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CartesiaBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Result",
                            modifier = Modifier.size(16.dp),
                            tint = CartesiaAccentTeal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Location",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Show time progress
                if (isActive) {
                    val currentSec = (progress / 1000) % 60
                    val currentMin = (progress / 60000) % 60
                    val totalSec = (duration / 1000) % 60
                    val totalMin = (duration / 60000) % 60
                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMappingSetupDialog(
    voiceName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var outputNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Voice Detected!",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Configure output file mapping mapping forever:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Voice Name:",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Text(
                    text = voiceName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = outputNameInput,
                    onValueChange = { outputNameInput = it },
                    label = { Text("Output File Name") },
                    placeholder = { Text("e.g. MissDo3aa") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CartesiaPurplePrimary,
                        unfocusedBorderColor = CartesiaBorder
                    )
                )
                Text(
                    text = "WAV format will be appended automatically.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (outputNameInput.isNotBlank()) {
                        onSave(outputNameInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CartesiaPurplePrimary),
                enabled = outputNameInput.isNotBlank()
            ) {
                Text("Save Forever")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = TextSecondary)
            }
        },
        containerColor = CartesiaSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val mappings by viewModel.voiceMappings.collectAsState()
    val downloadFolder by viewModel.downloadFolder.collectAsState()
    val savedKeys by viewModel.savedApiKeys.collectAsState()
    val activeKeyId by viewModel.activeApiKeyId.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var editOutputMapping by remember { mutableStateOf<VoiceMappingEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CartesiaPurpleLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VoiceFlow Settings",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                // Section 1: Folder selection
                Text(
                    text = "Storage & Downloads",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CartesiaPurpleLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CartesiaBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, CartesiaBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Folder",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = downloadFolder,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = {
                        // Choose default or standard paths
                        val downloadDir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
                        viewModel.setDownloadFolder(downloadDir)
                        Toast.makeText(context, "Download folder reset to app storage", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Reset folder",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Cartesia API Keys
                Text(
                    text = "Cartesia API Keys",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CartesiaPurpleLight
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (savedKeys.isEmpty()) {
                    Text(
                        text = "No API keys saved.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    savedKeys.forEach { keyEntry ->
                        val isActive = keyEntry.id == activeKeyId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(if (isActive) CartesiaPurplePrimary.copy(alpha = 0.1f) else CartesiaBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isActive) CartesiaPurplePrimary else CartesiaBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.switchKey(keyEntry.id) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Select key",
                                    tint = if (isActive) CartesiaPurpleLight else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = keyEntry.alias,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "ca_***" + keyEntry.key.takeLast(4),
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteKey(keyEntry.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete key",
                                    tint = DangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var showAddKeyForm by remember { mutableStateOf(false) }
                if (!showAddKeyForm) {
                    TextButton(
                        onClick = { showAddKeyForm = true },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add key icon", modifier = Modifier.size(16.dp), tint = CartesiaPurpleLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Another API Key", color = CartesiaPurpleLight, fontSize = 12.sp)
                    }
                } else {
                    var newKeyVal by remember { mutableStateOf("") }
                    var newKeyAlias by remember { mutableStateOf("") }
                    var inlineError by remember { mutableStateOf<String?>(null) }
                    var isInlineChecking by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CartesiaSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newKeyVal,
                            onValueChange = { newKeyVal = it; inlineError = null },
                            label = { Text("New Cartesia API Key", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newKeyAlias,
                            onValueChange = { newKeyAlias = it },
                            label = { Text("Alias / Label", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (inlineError != null) {
                            Text(text = inlineError!!, color = DangerRed, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showAddKeyForm = false }) {
                                Text("Cancel", fontSize = 11.sp, color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (newKeyVal.isBlank()) {
                                        inlineError = "Key is empty"
                                        return@Button
                                    }
                                    isInlineChecking = true
                                    inlineError = null
                                    val alias = if (newKeyAlias.isBlank()) "Added Key" else newKeyAlias
                                    viewModel.validateAndAddKey(
                                        key = newKeyVal,
                                        alias = alias,
                                        onSuccess = {
                                            isInlineChecking = false
                                            showAddKeyForm = false
                                            Toast.makeText(context, "API Key added & switched successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            isInlineChecking = false
                                            inlineError = err
                                        }
                                    )
                                },
                                enabled = !isInlineChecking,
                                colors = ButtonDefaults.buttonColors(containerColor = CartesiaPurplePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isInlineChecking) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("Add & Switch", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Manage Output Mappings
                Text(
                    text = "Manage Output Names",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CartesiaPurpleLight
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (mappings.isEmpty()) {
                    Text(
                        text = "No custom mappings saved yet.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CartesiaBorder, RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            items(mappings) { map ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = map.voiceName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Maps to: ${map.outputName}.wav",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { editOutputMapping = map }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { coroutineScope.launch { viewModel.setVoiceMapping(map.voiceId, map.voiceName, "") } }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = DangerRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit inline mapping dialog launcher
                editOutputMapping?.let { currentMap ->
                    var newName by remember { mutableStateOf(currentMap.outputName) }
                    AlertDialog(
                        onDismissRequest = { editOutputMapping = null },
                        title = { Text("Edit Output Name", color = TextPrimary) },
                        text = {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("New Output Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.setVoiceMapping(currentMap.voiceId, currentMap.voiceName, newName)
                                    editOutputMapping = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CartesiaPurplePrimary)
                            ) {
                                Text("Update")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editOutputMapping = null }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        containerColor = CartesiaSurface
                    )
                }

                // Section 3: Utility actions (Clear Cache, Logout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearCache()
                            Toast.makeText(context, "VoiceFlow cache cleared!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CartesiaSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear Cache", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.logout()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, DangerRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out icon",
                            modifier = Modifier.size(16.dp),
                            tint = DangerRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch Account", fontSize = 12.sp, color = DangerRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CartesiaPurpleLight)
            }
        },
        containerColor = CartesiaSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
