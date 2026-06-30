package com.example.ui.screens

import android.content.Context
import android.os.CountDownTimer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogoPosition
import com.example.data.model.MediaClip
import com.example.data.model.OverlayGraphics
import com.example.data.model.Project
import com.example.data.model.Subtitle
import com.example.data.model.Voiceover
import com.example.ui.theme.NewsRed
import com.example.ui.theme.NewsWhite
import com.example.ui.viewmodel.ProjectViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Int,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    
    // Bind current project on launch
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val project by viewModel.currentProject.collectAsState()
    val clips by viewModel.currentClips.collectAsState()
    val voiceovers by viewModel.currentVoiceovers.collectAsState()
    val subtitles by viewModel.currentSubtitles.collectAsState()
    val graphics by viewModel.currentGraphics.collectAsState()

    // Active Toolbar Drawer Selection
    var activeTab by varOf("MEDIA") // "MEDIA", "VOICE", "TEXT", "SUBTITLE", "GRAPHICS", "LOGO", "MUSIC", "EXPORT"

    // Preview Player Playback State
    var isPlaying by varOf(false)
    var playbackProgress by varOf(0.15f)

    // Selection helper for clips timeline
    var selectedClipId by varOf<String?>(null)
    val selectedClip = clips.find { it.id == selectedClipId }

    // Dialog state helpers
    var showAddSubtitleDialog by varOf(false)
    var showTtsDialog by varOf(false)
    var showVoiceRecordDialog by varOf(false)
    var showAiGeneratorDialog by varOf(false)

    if (project == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NewsRed)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project!!.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${project!!.outputType} • ${project!!.resolution}", color = Color.Gray, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentProject()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save & Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveCurrentProject() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171717))
                    ) {
                        Icon(Icons.Default.Save, "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. VIDEO PREVIEW MONITOR (Top part)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(Color.Black)
                    .border(2.dp, Color(0xFF22222E)),
                contentAlignment = Alignment.Center
            ) {
                // Render custom preview canvas overlay representing final compiled look
                VideoPreviewMonitor(
                    project = project!!,
                    clips = clips,
                    graphics = graphics,
                    subtitles = subtitles,
                    isPlaying = isPlaying,
                    progress = playbackProgress
                )

                // Playback control overlays (Floating)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playbackProgress = 0.0f },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Replay, "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(36.dp).testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = NewsRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "0:04 / 0:30",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // 2. TIMELINE SECTION (Middle part, multi-track timeline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color(0xFF262626))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    // TRACK A: CLIPS
                    TimelineTrack(
                        trackName = "CLIPS",
                        icon = Icons.Default.Movie
                    ) {
                        if (clips.isEmpty()) {
                            Text(
                                "No clips. Tap 'Import' below.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                clips.forEachIndexed { index, clip ->
                                    val isSelected = clip.id == selectedClipId
                                    Card(
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(55.dp)
                                            .testTag("clip_card_${clip.id}")
                                            .border(2.dp, if (isSelected) NewsRed else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedClipId = clip.id },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF262630))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}. ${clip.name}",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                            // duration tag
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.Black.copy(alpha = 0.7f))
                                                    .padding(2.dp)
                                            ) {
                                                Text("${clip.durationMs / 1000}s", fontSize = 8.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1A1A24))

                    // TRACK B: VOICEOVER
                    TimelineTrack(
                        trackName = "VOICE",
                        icon = Icons.Default.Mic
                    ) {
                        if (voiceovers.isEmpty()) {
                            Text("No Voiceovers", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                voiceovers.forEach { vo ->
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF1A3322), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VolumeUp, "VO", tint = Color.Green, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(vo.type, fontSize = 10.sp, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            "Remove",
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.deleteVoiceover(vo.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1A1A24))

                    // TRACK C: SUBTITLES
                    TimelineTrack(
                        trackName = "SUB",
                        icon = Icons.Default.Subtitles
                    ) {
                        if (subtitles.isEmpty()) {
                            Text("No Subtitles", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subtitles.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF2B2240), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sub.text, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            "Remove",
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.deleteSubtitle(sub.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CONTROL DRAWER DRAWER (Bottom active controls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF171717))
                    .border(1.dp, Color(0xFF262626))
            ) {
                when (activeTab) {
                    "MEDIA" -> MediaDrawerPanel(
                        selectedClip = selectedClip,
                        onAddClipMock = { type ->
                            // Fast clip import
                            val mockPath = if (type == "VIDEO") {
                                File(context.cacheDir, "Imported_Video_${System.currentTimeMillis()}.mp4").apply { writeBytes(ByteArray(0)) }.absolutePath
                            } else {
                                File(context.cacheDir, "Imported_Image_${System.currentTimeMillis()}.jpg").apply { writeBytes(ByteArray(0)) }.absolutePath
                            }
                            viewModel.addMediaClip(mockPath, type, 4000L)
                            Toast.makeText(context, "Added mock clip to timeline", Toast.LENGTH_SHORT).show()
                        },
                        onLaunchCamera = onNavigateToCamera,
                        onDeleteClip = {
                            selectedClipId?.let { viewModel.deleteClip(it) }
                            selectedClipId = null
                        },
                        onSpeedChange = { speed ->
                            selectedClip?.let { viewModel.updateClipSpeed(it.id, speed) }
                        },
                        onTransitionChange = { trans ->
                            selectedClip?.let { viewModel.updateClipTransition(it.id, trans) }
                        }
                    )

                    "VOICE" -> VoiceDrawerPanel(
                        onTriggerTts = { showTtsDialog = true },
                        onTriggerRecord = { showVoiceRecordDialog = true },
                        onTriggerAiScript = { showAiGeneratorDialog = true }
                    )

                    "TEXT" -> TextDrawerPanel(
                        graphics = graphics,
                        onUpdate = { viewModel.updateGraphicsSettings(it) }
                    )

                    "SUBTITLE" -> SubtitleDrawerPanel(
                        onAddManual = { showAddSubtitleDialog = true },
                        onAutoGenerate = {
                            viewModel.autoGenerateSubtitlesFromVoiceovers()
                            Toast.makeText(context, "Subtitles generated from Voiceovers!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    "GRAPHICS" -> GraphicsDrawerPanel(
                        project = project!!,
                        onApplyCategory = { viewModel.applyNewsTemplate(it) }
                    )

                    "MUSIC" -> MusicDrawerPanel(
                        project = project!!,
                        onUpdate = { volume, duck, fadeIn, fadeOut ->
                            viewModel.updateMusicSettings(project!!.musicPath, volume, duck, fadeIn, fadeOut)
                        }
                    )

                    "LOGO" -> LogoDrawerPanel(
                        project = project!!,
                        onUpdate = { path, opacity, pos ->
                            viewModel.updateLogoSettings(path, opacity, pos, project!!.logoAlwaysShow)
                        }
                    )

                    "EXPORT" -> ExportDrawerPanel(
                        viewModel = viewModel
                    )
                }
            }

            // 4. BOTTOM MAIN TOOLBAR TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF0F0F0F))
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarTabButton("Media", Icons.Default.Movie, activeTab == "MEDIA") { activeTab = "MEDIA" }
                ToolbarTabButton("Voice", Icons.Default.Mic, activeTab == "VOICE") { activeTab = "VOICE" }
                ToolbarTabButton("Text", Icons.Default.TextFields, activeTab == "TEXT") { activeTab = "TEXT" }
                ToolbarTabButton("Subtitle", Icons.Default.Subtitles, activeTab == "SUBTITLE") { activeTab = "SUBTITLE" }
                ToolbarTabButton("Graphics", Icons.Default.AutoAwesome, activeTab == "GRAPHICS") { activeTab = "GRAPHICS" }
                ToolbarTabButton("Music", Icons.Default.MusicNote, activeTab == "MUSIC") { activeTab = "MUSIC" }
                ToolbarTabButton("Logo", Icons.Default.Badge, activeTab == "LOGO") { activeTab = "LOGO" }
                ToolbarTabButton("Export", Icons.Default.IosShare, activeTab == "EXPORT") { activeTab = "EXPORT" }
            }
        }
    }

    // --- SUBDIALOGS IMPLEMENTATION ---

    if (showAddSubtitleDialog) {
        var subText by varOf("Breaking Updates...")
        var startSec by varOf("0")
        var endSec by varOf("4")

        AlertDialog(
            onDismissRequest = { showAddSubtitleDialog = false },
            containerColor = Color(0xFF171717),
            title = { Text("Insert Manual Subtitle", color = Color.White, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = subText,
                        onValueChange = { subText = it },
                        label = { Text("Subtitle Text") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("sub_text_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = startSec,
                            onValueChange = { startSec = it },
                            label = { Text("Start (s)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endSec,
                            onValueChange = { endSec = it },
                            label = { Text("End (s)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddSubtitleDialog = false
                        val startMs = (startSec.toFloatOrNull() ?: 0f) * 1000L
                        val endMs = (endSec.toFloatOrNull() ?: 4f) * 1000L
                        viewModel.addSubtitle(subText, startMs.toLong(), endMs.toLong())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed)
                ) {
                    Text("Add Subtitle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubtitleDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    if (showTtsDialog) {
        var scriptText by varOf("আজকের তাজা খবর, ব্রেকিং নিউজ!")
        var isBangla by varOf(true)

        AlertDialog(
            onDismissRequest = { showTtsDialog = false },
            containerColor = Color(0xFF171717),
            title = { Text("Text To Speech Synthesizer", color = Color.White, fontSize = 16.sp) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isBangla, onClick = { isBangla = true })
                        Text("Bangla", color = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !isBangla, onClick = { isBangla = false })
                        Text("English", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { scriptText = it },
                        label = { Text("Broadcast Script") },
                        modifier = Modifier.fillMaxWidth().testTag("tts_text_field"),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTtsDialog = false
                        viewModel.generateTextToSpeech(context, scriptText, isBangla) { success ->
                            if (success) {
                                Toast.makeText(context, "TTS Track inserted successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Synthesis complete. Added placeholder.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                    modifier = Modifier.testTag("submit_tts")
                ) {
                    Text("Insert Timeline")
                }
            }
        )
    }

    if (showVoiceRecordDialog) {
        var recordSec by varOf(0)
        var isRecordingLocal by varOf(false)
        var activeTimer: CountDownTimer? by remember { mutableStateOf(null) }

        AlertDialog(
            onDismissRequest = {
                activeTimer?.cancel()
                showVoiceRecordDialog = false
            },
            containerColor = Color(0xFF171717),
            title = { Text("Field Voiceover Recorder", color = Color.White, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecordingLocal) "Recording Voice Over..." else "Ready to Record",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = String.format("00:%02d", recordSec),
                        color = if (isRecordingLocal) NewsRed else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Waveform simulation
                    Row(
                        modifier = Modifier.height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(12) {
                            val h = if (isRecordingLocal) (10..40).random().dp else 4.dp
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(h)
                                    .background(if (isRecordingLocal) NewsRed else Color.Gray)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRecordingLocal) {
                            // Stop
                            activeTimer?.cancel()
                            isRecordingLocal = false
                            showVoiceRecordDialog = false
                            
                            // Mock writing
                            val mockVoFile = File(context.cacheDir, "Vo_Record_${System.currentTimeMillis()}.wav").apply { writeBytes(ByteArray(0)) }
                            viewModel.addVoiceover(
                                mockVoFile.absolutePath,
                                "RECORDING",
                                (recordSec * 1000).toLong(),
                                0L
                            )
                        } else {
                            // Start
                            isRecordingLocal = true
                            activeTimer = object : CountDownTimer(30000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    recordSec++
                                }
                                override fun onFinish() {}
                            }.start()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                    modifier = Modifier.testTag("record_mic_button")
                ) {
                    Text(if (isRecordingLocal) "Stop & Insert" else "Record Voice")
                }
            }
        )
    }

    if (showAiGeneratorDialog) {
        var promptVal by varOf("Weather flood Dhaka")
        var isBangla by varOf(true)
        val isGenerating by viewModel.isGeneratingNews.collectAsState()

        AlertDialog(
            onDismissRequest = { showAiGeneratorDialog = false },
            containerColor = Color(0xFF171717),
            title = { Text("Gemini AI News Script Generator", color = Color.White, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = promptVal,
                        onValueChange = { promptVal = it },
                        label = { Text("News Topic / Keywords") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isBangla, onClick = { isBangla = true })
                        Text("Bangla", color = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !isBangla, onClick = { isBangla = false })
                        Text("English", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiGeneratorDialog = false
                        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                        viewModel.generateSmartNewsScript(promptVal, isBangla, apiKey) { generatedScript ->
                            // Open TTS with the generated script immediately!
                            showTtsDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewsRed)
                ) {
                    Text("Generate Script")
                }
            }
        )
    }
}

@Composable
fun LowerThirdOverlay(
    name: String,
    title: String,
    style: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (style) {
                    "GLASSMORPHISM" -> Brush.horizontalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f))
                    )
                    "DARK_NEON" -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0F0F12), Color(0xFF1F1F24))
                    )
                    "CLASSIC_BLUE" -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF0D1B2A))
                    )
                    else -> Brush.horizontalGradient( // RED_SLEEK
                        colors = listOf(Color(0xFFDC2626), Color(0xFF7F1D1D))
                    )
                }
            )
            .then(
                if (style == "GLASSMORPHISM") {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                } else if (style == "DARK_NEON") {
                    Modifier.border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                } else if (style == "CLASSIC_BLUE") {
                    Modifier.border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                } else {
                    Modifier // RED_SLEEK
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (style == "RED_SLEEK" || style == "CLASSIC_BLUE") {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (style == "RED_SLEEK") Color(0xFFFDE047) else Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column {
            Text(
                text = name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = when (style) {
                        "GLASSMORPHISM" -> Color(0xFF93C5FD)
                        "DARK_NEON" -> Color(0xFFEF4444)
                        "CLASSIC_BLUE" -> Color(0xFF93C5FD)
                        else -> Color(0xFFFDE047) // RED_SLEEK
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Render dynamic previews representing final compositing
@Composable
fun VideoPreviewMonitor(
    project: Project,
    clips: List<MediaClip>,
    graphics: OverlayGraphics,
    subtitles: List<Subtitle>,
    isPlaying: Boolean,
    progress: Float
) {
    var triggerTransition by remember { mutableStateOf(false) }
    var transitionTypeToPlay by remember { mutableStateOf("NONE") }

    // Trigger transition simulation whenever active clip changes or isPlaying changes
    LaunchedEffect(clips.size, progress) {
        if (clips.isNotEmpty() && clips.first().transitionType != "NONE") {
            transitionTypeToPlay = clips.first().transitionType
            triggerTransition = true
            kotlinx.coroutines.delay(1200)
            triggerTransition = false
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(if (project.outputType == com.example.data.model.OutputType.LANDSCAPE) 16f/9f else 9f/16f)
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151515))
    ) {
        // Base content (Simulate with visual card if no clips, or visual elements)
        if (clips.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.MovieFilter, "No Media", tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("No Media Clips Imported", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            // Dynamic news desk canvas background simulation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFF2C1010), Color(0xFF09090C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[Playing: ${clips.first().name}]",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- RENDER BRANDED TV OVERLAY LAYOUTS ---
        if (graphics.type != "NONE") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Reporter / Location tag if set
                if (graphics.reporterName.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${graphics.reporterName} (${graphics.location})",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // --- PRESENTER LOWER THIRD ---
                if (graphics.lowerThirdEnabled && graphics.lowerThirdName.isNotEmpty()) {
                    LowerThirdOverlay(
                        name = graphics.lowerThirdName,
                        title = graphics.lowerThirdTitle,
                        style = graphics.lowerThirdStyle,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Main News Bar Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(Color.Black.copy(alpha = 0.85f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Breaking badge (Red)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(NewsRed)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (graphics.type) {
                                "BREAKING_NEWS" -> "BREAKING"
                                "LIVE" -> "LIVE"
                                "EXCLUSIVE" -> "EXCLUSIVE"
                                else -> "TOP STORY"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Scrolling news ticker / Headline
                    Text(
                        text = if (graphics.scrollingTicker.isNotEmpty()) graphics.scrollingTicker else graphics.headline,
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- TRANSITION ANIMATION EFFECT OVERLAY ---
        if (triggerTransition && transitionTypeToPlay != "NONE") {
            val infiniteTransition = rememberInfiniteTransition(label = "transition")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            val animatedAlpha by animateFloatAsState(
                targetValue = if (triggerTransition) 1f else 0f,
                animationSpec = tween(300),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = animatedAlpha * 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                when (transitionTypeToPlay) {
                    "SPIN_LOGO" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .graphicsLayer(rotationZ = angle)
                                .size(80.dp)
                                .background(NewsRed, CircleShape)
                                .border(3.dp, Color.White, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Spin transition",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    "WIPE" -> {
                        var wipeProgress by remember { mutableStateOf(-1f) }
                        LaunchedEffect(Unit) {
                            androidx.compose.animation.core.animate(
                                initialValue = -1.2f,
                                targetValue = 1.2f,
                                animationSpec = tween(1000, easing = FastOutSlowInEasing)
                            ) { value, _ -> wipeProgress = value }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .graphicsLayer(translationX = wipeProgress * 300f)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, NewsRed, Color.White, NewsRed, Color.Transparent)
                                    )
                                )
                        )
                    }
                    "FADE" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = animatedAlpha))
                        )
                    }
                    "SLIDE" -> {
                        var slideProgress by remember { mutableStateOf(1.2f) }
                        LaunchedEffect(Unit) {
                            androidx.compose.animation.core.animate(
                                initialValue = 1.2f,
                                targetValue = -1.2f,
                                animationSpec = tween(900, easing = LinearOutSlowInEasing)
                            ) { value, _ -> slideProgress = value }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .graphicsLayer(translationX = slideProgress * 300f)
                                .background(NewsRed)
                                .border(2.dp, Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "NEWS TRANSITION",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        // Subtitles Overlay
        if (subtitles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 70.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(subtitles.first().text, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        // Logo overlay if any
        if (project.logoPath != null) {
            val alignLogo = when (project.logoPosition) {
                LogoPosition.TOP_LEFT -> Alignment.TopStart
                LogoPosition.TOP_RIGHT -> Alignment.TopEnd
                LogoPosition.BOTTOM_LEFT -> Alignment.BottomStart
                LogoPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
            }
            Box(
                modifier = Modifier
                    .align(alignLogo)
                    .padding(12.dp)
                    .background(Color.Red.copy(alpha = project.logoOpacity), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("NEWS PRO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Convenient modular Track Builder
@Composable
fun TimelineTrack(
    trackName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, trackName, tint = NewsRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(trackName, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

// Bottom tab element
@Composable
fun RowScope.ToolbarTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFF171717) else Color.Transparent)
            .testTag("toolbar_${label.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NewsRed else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = if (isSelected) Color.White else Color.Gray, fontSize = 9.sp)
        }
    }
}

// --- DRAWER CONTROL PANELS IMPLEMENTATION ---

@Composable
fun MediaDrawerPanel(
    selectedClip: MediaClip?,
    onAddClipMock: (String) -> Unit,
    onLaunchCamera: () -> Unit,
    onDeleteClip: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTransitionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Media Assets Management", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onAddClipMock("VIDEO") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232330)),
                modifier = Modifier.weight(1f).testTag("import_video_btn")
            ) {
                Icon(Icons.Default.VideoCall, "Video")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Video Clip", fontSize = 11.sp)
            }
            Button(
                onClick = { onAddClipMock("IMAGE") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232330)),
                modifier = Modifier.weight(1f).testTag("import_image_btn")
            ) {
                Icon(Icons.Default.Image, "Image")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Static Image", fontSize = 11.sp)
            }
            Button(
                onClick = onLaunchCamera,
                colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                modifier = Modifier.weight(1f).testTag("launch_camera_btn")
            ) {
                Icon(Icons.Default.CameraAlt, "Camera")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Field Rec", fontSize = 11.sp)
            }
        }

        if (selectedClip != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected: ${selectedClip.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Speed multiplier: ${selectedClip.speed}x", color = Color.Gray, fontSize = 10.sp)
                        }

                        IconButton(onClick = onDeleteClip, modifier = Modifier.testTag("delete_clip_btn")) {
                            Icon(Icons.Default.Delete, "Delete", tint = NewsRed)
                        }
                    }

                    // Speed Control Choices
                    Text("Clip Speed (গতি নিয়ন্ত্রণ)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0f, 1.5f, 2.0f).forEach { speedVal ->
                            val isSpSelected = selectedClip.speed == speedVal
                            Button(
                                onClick = { onSpeedChange(speedVal) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpSelected) NewsRed else Color(0xFF2A2A2A)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("${speedVal}x", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Transition to Next Clip Selector
                    Text("Transition to Next Clip (পরবর্তী ক্লিপের ট্রানজিশন)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "NONE" to "None",
                            "WIPE" to "Wipe (ওয়াইপ)",
                            "FADE" to "Fade (ফেইড)",
                            "SPIN_LOGO" to "Spin Logo (স্পিন)",
                            "SLIDE" to "Slide (স্লাইড)"
                        ).forEach { (typeKey, label) ->
                            val isTransSelected = selectedClip.transitionType == typeKey
                            Button(
                                onClick = { onTransitionChange(typeKey) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTransSelected) NewsRed else Color(0xFF2A2A2A)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceDrawerPanel(
    onTriggerTts: () -> Unit,
    onTriggerRecord: () -> Unit,
    onTriggerAiScript: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Voiceover & Text-To-Speech Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onTriggerRecord,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232330)),
                modifier = Modifier.weight(1f).testTag("voice_record_btn")
            ) {
                Icon(Icons.Default.Mic, "Rec")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mic Rec", fontSize = 11.sp)
            }
            Button(
                onClick = onTriggerTts,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232330)),
                modifier = Modifier.weight(1f).testTag("tts_synthesis_btn")
            ) {
                Icon(Icons.Default.VolumeUp, "TTS")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Script TTS", fontSize = 11.sp)
            }
            Button(
                onClick = onTriggerAiScript,
                colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                modifier = Modifier.weight(1.2f).testTag("ai_script_btn")
            ) {
                Icon(Icons.Default.AutoAwesome, "AI Script")
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Smart Script", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TextDrawerPanel(
    graphics: OverlayGraphics,
    onUpdate: (OverlayGraphics) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("TV News Screen Graphics Setup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        OutlinedTextField(
            value = graphics.headline,
            onValueChange = { onUpdate(graphics.copy(headline = it)) },
            label = { Text("News Headline") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("headline_text_input")
        )

        OutlinedTextField(
            value = graphics.scrollingTicker,
            onValueChange = { onUpdate(graphics.copy(scrollingTicker = it)) },
            label = { Text("Scrolling Bottom Ticker") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("scrolling_text_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = graphics.reporterName,
                onValueChange = { onUpdate(graphics.copy(reporterName = it)) },
                label = { Text("Reporter Name") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = graphics.location,
                onValueChange = { onUpdate(graphics.copy(location = it)) },
                label = { Text("Location") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)

        // PRESENTER LOWER THIRD SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Presenter Lower Third (লোয়ার থার্ডস)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Show presenter's name strip on video", color = Color.Gray, fontSize = 10.sp)
            }
            Switch(
                checked = graphics.lowerThirdEnabled,
                onCheckedChange = { onUpdate(graphics.copy(lowerThirdEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = NewsRed, checkedTrackColor = NewsRed.copy(alpha = 0.4f))
            )
        }

        if (graphics.lowerThirdEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = graphics.lowerThirdName,
                    onValueChange = { onUpdate(graphics.copy(lowerThirdName = it)) },
                    label = { Text("Presenter Name (নাম)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = graphics.lowerThirdTitle,
                    onValueChange = { onUpdate(graphics.copy(lowerThirdTitle = it)) },
                    label = { Text("Title/Designation (পদবী)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Select Strip Style (ডিজাইন স্টাইল)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "RED_SLEEK" to "Red Sleek",
                    "GLASSMORPHISM" to "Glassmorphism",
                    "DARK_NEON" to "Dark Neon",
                    "CLASSIC_BLUE" to "Classic Blue"
                ).forEach { (styleKey, label) ->
                    val isStyleSelected = graphics.lowerThirdStyle == styleKey
                    Button(
                        onClick = { onUpdate(graphics.copy(lowerThirdStyle = styleKey)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStyleSelected) NewsRed else Color(0xFF262626)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(label, fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SubtitleDrawerPanel(
    onAddManual: () -> Unit,
    onAutoGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Subtitle Generation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAddManual,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232330)),
                modifier = Modifier.weight(1f).testTag("add_manual_sub_btn")
            ) {
                Icon(Icons.Default.Add, "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Manual", fontSize = 11.sp)
            }
            Button(
                onClick = onAutoGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                modifier = Modifier.weight(1f).testTag("auto_sub_btn")
            ) {
                Icon(Icons.Default.AutoMode, "Auto")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Auto Captions", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun GraphicsDrawerPanel(
    project: Project,
    onApplyCategory: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text("Apply Theme Preset Style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NewsThemeChip("Breaking", project.templateCategory == "BREAKING") { onApplyCategory("BREAKING") }
            NewsThemeChip("Politics", project.templateCategory == "POLITICS") { onApplyCategory("POLITICS") }
            NewsThemeChip("Sports", project.templateCategory == "SPORTS") { onApplyCategory("SPORTS") }
            NewsThemeChip("International", project.templateCategory == "INTERNATIONAL") { onApplyCategory("INTERNATIONAL") }
            NewsThemeChip("Weather", project.templateCategory == "WEATHER") { onApplyCategory("WEATHER") }
            NewsThemeChip("Technology", project.templateCategory == "TECHNOLOGY") { onApplyCategory("TECHNOLOGY") }
        }
    }
}

@Composable
fun NewsThemeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NewsRed,
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF171717),
            labelColor = Color.Gray
        )
    )
}

@Composable
fun MusicDrawerPanel(
    project: Project,
    onUpdate: (Float, Boolean, Boolean, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Music Mixer Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = project.musicVolume,
                onValueChange = { onUpdate(it, project.musicDuck, project.musicFadeIn, project.musicFadeOut) },
                colors = SliderDefaults.colors(thumbColor = NewsRed, activeTrackColor = NewsRed),
                modifier = Modifier.weight(1f).testTag("music_volume_slider")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = project.musicDuck,
                    onCheckedChange = { onUpdate(project.musicVolume, it, project.musicFadeIn, project.musicFadeOut) },
                    colors = CheckboxDefaults.colors(checkedColor = NewsRed)
                )
                Text("Duck voice automatically", color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LogoDrawerPanel(
    project: Project,
    onUpdate: (String?, Float, LogoPosition) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Watermark Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            Slider(
                value = project.logoOpacity,
                onValueChange = { onUpdate(project.logoPath ?: "temp_path", it, project.logoPosition) },
                colors = SliderDefaults.colors(thumbColor = NewsRed, activeTrackColor = NewsRed),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logo Corner Position:", color = Color.LightGray, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LogoPosBtn("TL", project.logoPosition == LogoPosition.TOP_LEFT) { onUpdate(project.logoPath, project.logoOpacity, LogoPosition.TOP_LEFT) }
                LogoPosBtn("TR", project.logoPosition == LogoPosition.TOP_RIGHT) { onUpdate(project.logoPath, project.logoOpacity, LogoPosition.TOP_RIGHT) }
                LogoPosBtn("BL", project.logoPosition == LogoPosition.BOTTOM_LEFT) { onUpdate(project.logoPath, project.logoOpacity, LogoPosition.BOTTOM_LEFT) }
                LogoPosBtn("BR", project.logoPosition == LogoPosition.BOTTOM_RIGHT) { onUpdate(project.logoPath, project.logoOpacity, LogoPosition.BOTTOM_RIGHT) }
            }
        }
    }
}

@Composable
fun LogoPosBtn(label: String, isSelected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) NewsRed.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) NewsRed else Color.Gray
        ),
        modifier = Modifier.size(width = 46.dp, height = 30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 10.sp)
    }
}

@Composable
fun ExportDrawerPanel(
    viewModel: ProjectViewModel
) {
    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val message by viewModel.exportMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Export Compilation Desk", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        if (isExporting) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = NewsRed,
                    trackColor = Color.Gray.copy(alpha = 0.2f),
                )
                Text(message ?: "Compiling...", color = Color.LightGray, fontSize = 11.sp)
            }
        } else {
            if (message != null) {
                Text(message!!, color = Color.Yellow, fontSize = 12.sp)
                Button(
                    onClick = { viewModel.clearExportState() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("OK", fontSize = 11.sp)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.exportProject(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = NewsRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("trigger_export_btn")
                    ) {
                        Icon(Icons.Default.IosShare, "Export")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Render MP4 Video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
