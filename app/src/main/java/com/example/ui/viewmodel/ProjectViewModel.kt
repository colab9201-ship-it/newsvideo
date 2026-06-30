package com.example.ui.viewmodel

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataStoreManager
import com.example.data.model.LogoPosition
import com.example.data.model.MediaClip
import com.example.data.model.OutputType
import com.example.data.model.OverlayGraphics
import com.example.data.model.Project
import com.example.data.model.Resolution
import com.example.data.model.Subtitle
import com.example.data.model.Voiceover
import com.example.data.repository.ProjectRepository
import com.example.utils.FFmpegEngine
import com.example.utils.TtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectViewModel(
    private val repository: ProjectRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val ffmpegEngine = FFmpegEngine()
    private var ttsEngine: TtsEngine? = null

    // All Projects
    val projectsList: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Project state
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // Timeline Assets Lists
    private val _currentClips = MutableStateFlow<List<MediaClip>>(emptyList())
    val currentClips: StateFlow<List<MediaClip>> = _currentClips.asStateFlow()

    private val _currentVoiceovers = MutableStateFlow<List<Voiceover>>(emptyList())
    val currentVoiceovers: StateFlow<List<Voiceover>> = _currentVoiceovers.asStateFlow()

    private val _currentSubtitles = MutableStateFlow<List<Subtitle>>(emptyList())
    val currentSubtitles: StateFlow<List<Subtitle>> = _currentSubtitles.asStateFlow()

    private val _currentGraphics = MutableStateFlow(OverlayGraphics())
    val currentGraphics: StateFlow<OverlayGraphics> = _currentGraphics.asStateFlow()

    // App Preferences settings
    val darkModeState = dataStoreManager.darkModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val defaultLogoState = dataStoreManager.defaultLogoFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val defaultFontState = dataStoreManager.defaultFontFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "Noto Sans Bangla")

    // Export state bound to the high-speed background VideoExportService
    val isExporting: StateFlow<Boolean> = com.example.utils.VideoExportService.isExporting
    val exportProgress: StateFlow<Float> = com.example.utils.VideoExportService.exportProgress
    val exportFilePath: StateFlow<String?> = com.example.utils.VideoExportService.exportFilePath
    val exportMessage: StateFlow<String?> = com.example.utils.VideoExportService.exportMessage

    // Gemini API News Generation States
    private val _isGeneratingNews = MutableStateFlow(false)
    val isGeneratingNews: StateFlow<Boolean> = _isGeneratingNews.asStateFlow()

    init {
        // Init TTS engine
        Log.d("ProjectViewModel", "Initialized ProjectViewModel")
    }

    fun initTts(context: Context) {
        if (ttsEngine == null) {
            ttsEngine = TtsEngine(context.applicationContext)
        }
    }

    // Settings actions
    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setDarkMode(enabled)
    }

    fun setDefaultLogo(path: String) = viewModelScope.launch {
        dataStoreManager.setDefaultLogo(path)
    }

    fun setDefaultFont(font: String) = viewModelScope.launch {
        dataStoreManager.setDefaultFont(font)
    }

    fun setDefaultResolution(res: String) = viewModelScope.launch {
        dataStoreManager.setDefaultResolution(res)
    }

    // Create New Project
    fun createProject(
        name: String,
        outputType: OutputType,
        resolution: Resolution,
        fps: Int,
        onComplete: (Project) -> Unit
    ) {
        viewModelScope.launch {
            val defaultLogo = defaultLogoState.value
            val project = Project(
                name = name,
                outputType = outputType,
                resolution = resolution,
                fps = fps,
                logoPath = if (defaultLogo.isNotEmpty()) defaultLogo else null
            )
            val newId = repository.insertProject(project)
            val savedProject = project.copy(id = newId.toInt())
            _currentProject.value = savedProject
            _currentClips.value = emptyList()
            _currentVoiceovers.value = emptyList()
            _currentSubtitles.value = emptyList()
            _currentGraphics.value = OverlayGraphics()
            onComplete(savedProject)
        }
    }

    // Load Project
    fun loadProject(id: Int) {
        viewModelScope.launch {
            val project = repository.getProjectById(id)
            if (project != null) {
                _currentProject.value = project
                
                // Decode subtracks from database JSON Strings
                _currentClips.value = try {
                    Json.decodeFromString(project.clipsJson)
                } catch (e: Exception) {
                    emptyList()
                }
                
                _currentVoiceovers.value = try {
                    Json.decodeFromString(project.voiceoversJson)
                } catch (e: Exception) {
                    emptyList()
                }
                
                _currentSubtitles.value = try {
                    Json.decodeFromString(project.subtitlesJson)
                } catch (e: Exception) {
                    emptyList()
                }

                _currentGraphics.value = try {
                    Json.decodeFromString(project.graphicsJson)
                } catch (e: Exception) {
                    OverlayGraphics()
                }
            }
        }
    }

    // Save project changes to DB
    fun saveCurrentProject() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val updatedProject = project.copy(
                clipsJson = Json.encodeToString(_currentClips.value),
                voiceoversJson = Json.encodeToString(_currentVoiceovers.value),
                subtitlesJson = Json.encodeToString(_currentSubtitles.value),
                graphicsJson = Json.encodeToString(_currentGraphics.value)
            )
            repository.updateProject(updatedProject)
            _currentProject.value = updatedProject
            Log.d("ProjectViewModel", "Saved project changes to Room")
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
            if (_currentProject.value?.id == id) {
                _currentProject.value = null
            }
        }
    }

    fun renameProject(id: Int, newName: String) {
        viewModelScope.launch {
            val project = repository.getProjectById(id)
            if (project != null) {
                val updated = project.copy(name = newName)
                repository.updateProject(updated)
                if (_currentProject.value?.id == id) {
                    _currentProject.value = updated
                }
            }
        }
    }

    // --- Timeline Editing Operations ---

    fun addMediaClip(filePath: String, type: String, durationMs: Long) {
        val newClip = MediaClip(
            id = "CLIP_${System.currentTimeMillis()}",
            type = type,
            filePath = filePath,
            name = File(filePath).name,
            durationMs = durationMs
        )
        _currentClips.value = _currentClips.value + newClip
        saveCurrentProject()
    }

    fun deleteClip(clipId: String) {
        _currentClips.value = _currentClips.value.filterNot { it.id == clipId }
        saveCurrentProject()
    }

    fun trimClip(clipId: String, startMs: Long, endMs: Long) {
        _currentClips.value = _currentClips.value.map { clip ->
            if (clip.id == clipId) {
                clip.copy(trimStartMs = startMs, trimEndMs = endMs)
            } else {
                clip
            }
        }
        saveCurrentProject()
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        _currentClips.value = _currentClips.value.map { clip ->
            if (clip.id == clipId) {
                clip.copy(speed = speed)
            } else {
                clip
            }
        }
        saveCurrentProject()
    }

    fun updateClipTransition(clipId: String, transitionType: String) {
        _currentClips.value = _currentClips.value.map { clip ->
            if (clip.id == clipId) {
                clip.copy(transitionType = transitionType)
            } else {
                clip
            }
        }
        saveCurrentProject()
    }

    fun reorderClips(fromIndex: Int, toIndex: Int) {
        val list = _currentClips.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _currentClips.value = list
            saveCurrentProject()
        }
    }

    // --- Voiceover & Text-To-Speech ---

    fun addVoiceover(filePath: String, type: String, durationMs: Long, startMs: Long) {
        val newVo = Voiceover(
            id = "VO_${System.currentTimeMillis()}",
            type = type,
            filePath = filePath,
            durationMs = durationMs,
            timelineStartMs = startMs
        )
        _currentVoiceovers.value = _currentVoiceovers.value + newVo
        saveCurrentProject()
    }

    fun deleteVoiceover(voId: String) {
        _currentVoiceovers.value = _currentVoiceovers.value.filterNot { it.id == voId }
        saveCurrentProject()
    }

    fun generateTextToSpeech(context: Context, text: String, isBangla: Boolean, onComplete: (Boolean) -> Unit) {
        initTts(context)
        viewModelScope.launch {
            val cacheFile = File(context.cacheDir, "TTS_${System.currentTimeMillis()}.wav")
            ttsEngine?.synthesizeToFile(text, isBangla, cacheFile) { success ->
                if (success && cacheFile.exists()) {
                    // Inject into current timeline
                    val startAt = currentDurationMs()
                    addVoiceover(cacheFile.absolutePath, if (isBangla) "TTS_BN" else "TTS_EN", 4000L, startAt)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        }
    }

    // --- Music Settings ---

    fun updateMusicSettings(
        path: String?,
        volume: Float,
        duck: Boolean,
        fadeIn: Boolean,
        fadeOut: Boolean
    ) {
        val project = _currentProject.value ?: return
        _currentProject.value = project.copy(
            musicPath = path,
            musicVolume = volume,
            musicDuck = duck,
            musicFadeIn = fadeIn,
            musicFadeOut = fadeOut
        )
        saveCurrentProject()
    }

    // --- Logo Watermark ---

    fun updateLogoSettings(
        path: String?,
        opacity: Float,
        position: LogoPosition,
        alwaysShow: Boolean
    ) {
        val project = _currentProject.value ?: return
        _currentProject.value = project.copy(
            logoPath = path,
            logoOpacity = opacity,
            logoPosition = position,
            logoAlwaysShow = alwaysShow
        )
        saveCurrentProject()
    }

    // --- Overlays & Graphic Overlays ---

    fun updateGraphicsSettings(graphics: OverlayGraphics) {
        _currentGraphics.value = graphics
        saveCurrentProject()
    }

    // --- Manual Subtitle Editing ---

    fun addSubtitle(text: String, startMs: Long, endMs: Long) {
        val newSub = Subtitle(
            id = "SUB_${System.currentTimeMillis()}",
            text = text,
            startTimeMs = startMs,
            endTimeMs = endMs
        )
        _currentSubtitles.value = _currentSubtitles.value + newSub
        saveCurrentProject()
    }

    fun deleteSubtitle(subId: String) {
        _currentSubtitles.value = _currentSubtitles.value.filterNot { it.id == subId }
        saveCurrentProject()
    }

    // --- Template Style application ---

    fun applyNewsTemplate(category: String) {
        val project = _currentProject.value ?: return
        
        // Define templates values
        val (headline, ticker, reporter, location) = when (category) {
            "BREAKING" -> listOf("BREAKING NEWS BULLETIN", "ALERT: MAIN HIGHWAY CLOSED FOR FLOOD EMERGENCIES IN REGION...", "STAFF REPORTER", "DHAKA STUDIO")
            "POLITICS" -> listOf("MINISTERS ANNOUNCE STRATEGIC REFORMS", "POLITICS UPDATE: DEBATES SCHEDULED FOR FRIDAY EVENING IN DOWNTOWN...", "POLITICAL CORRESPONDENT", "PARLIAMENT")
            "SPORTS" -> listOf("LOCAL HEROES WIN NATIONAL CHAMPIONSHIP", "SPORTS EXTRA: FOOTBALL STADIUM EXPANSION TO BEGIN NEXT MONTH...", "SPORTS ANALYST", "STADIUM COLISEUM")
            "WEATHER" -> listOf("CYCLONE WARNING ISSUED FOR COASTAL BELT", "WEATHER ALERT: HEAVY RAIN EXPECTED OVER THE WEEKEND, STAY INDOORS...", "METEOROLOGIST", "WEATHER STATION")
            "INTERNATIONAL" -> listOf("GLOBAL LEADERS CONVENE FOR CLIMATE SUMMIT", "WORLD NEWS: COOPERATION TREATY SIGNED BY MULTIPLE NATIONS IN NEW YORK...", "FOREIGN CORRESPONDENT", "GENEVA OUTPOST")
            "TECHNOLOGY" -> listOf("AI REVOLUTIONIZES VIDEO NEWS STUDIOS", "TECH TIDBITS: REVOLUTIONARY CONSUMER DEVICES ANNOUNCED BY STARTUP...", "TECH JOURNALIST", "SILICON VALLEY")
            else -> listOf("", "", "", "")
        }

        _currentGraphics.value = OverlayGraphics(
            type = if (category == "NORMAL") "NONE" else "BREAKING_NEWS",
            headline = headline,
            scrollingTicker = ticker,
            reporterName = reporter,
            location = location
        )
        _currentProject.value = project.copy(templateCategory = category)
        saveCurrentProject()
    }

    // Helper: Find current total duration of timeline
    private fun currentDurationMs(): Long {
        return _currentClips.value.sumOf { it.durationMs }
    }

    // --- AI Smart Script & Auto Subtitle Generator (Gemini Integration) ---

    fun generateSmartNewsScript(prompt: String, isBangla: Boolean, apiKey: String, onComplete: (String) -> Unit) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Mock fallback if user hasn't provided key yet
            val script = if (isBangla) {
                "ব্রেকিং নিউজ: আজ সকালে আবহাওয়া বিভাগ একটি ভারী বৃষ্টির সতর্কবার্তা জারি করেছে। উপকূলীয় এলাকায় ঝড়ো হাওয়া বইতে পারে।"
            } else {
                "BREAKING NEWS: The meteorological department has issued a high-alert warning for heavy rains and thunderstorms starting tonight. Residents are advised to stay indoors."
            }
            onComplete(script)
            return
        }

        _isGeneratingNews.value = true
        viewModelScope.launch {
            try {
                val fullPrompt = "Write a short, engaging 1-minute TV news script based on this topic: '$prompt'. Write it in " +
                        (if (isBangla) "Bangla Language" else "English Language") + ". Make it punchy, suitable for a news broadcast."
                
                val result = callGeminiRest(apiKey, fullPrompt)
                onComplete(result)
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Gemini call failed", e)
                onComplete("Failed to generate script. Please verify your internet and API key.")
            } finally {
                _isGeneratingNews.value = false
            }
        }
    }

    /**
     * Reuses REST setup for Gemini direct queries.
     */
    private suspend fun callGeminiRest(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": ${Json.encodeToString(prompt)}
                }]
              }]
            }
        """.trimIndent()

        conn.outputStream.use { os ->
            os.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            // simple parsing of Gemini response JSON
            val startTextIndex = responseText.indexOf("\"text\": \"")
            if (startTextIndex != -1) {
                val sub = responseText.substring(startTextIndex + 9)
                val endTextIndex = sub.indexOf("\"")
                if (endTextIndex != -1) {
                    return@withContext sub.substring(0, endTextIndex)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                }
            }
            return@withContext "Successfully contacted Gemini but could not parse. Response:\n$responseText"
        } else {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown HTTP code"
            throw Exception("HTTP ${conn.responseCode}: $errorText")
        }
    }

    // Auto Subtitle matching generator based on loaded audio track
    fun autoGenerateSubtitlesFromVoiceovers() {
        val voiceoversList = _currentVoiceovers.value
        if (voiceoversList.isEmpty()) return
        
        // Match subtitles block to voiceover timeline intervals
        val newSubs = mutableListOf<Subtitle>()
        voiceoversList.forEachIndexed { index, vo ->
            newSubs.add(
                Subtitle(
                    id = "SUB_AUTO_${System.currentTimeMillis()}_$index",
                    text = "Broadcast Clip Voiceover Segment ${index + 1}",
                    startTimeMs = vo.timelineStartMs,
                    endTimeMs = vo.timelineStartMs + vo.durationMs
                )
            )
        }
        _currentSubtitles.value = newSubs
        saveCurrentProject()
    }

    // --- Video Compilation / Export Execution ---

    fun exportProject(context: Context) {
        val project = _currentProject.value ?: return
        val clips = _currentClips.value
        if (clips.isEmpty()) {
            com.example.utils.VideoExportService.exportMessage.value = "Cannot export empty timeline"
            return
        }

        com.example.utils.VideoExportService.startExport(context, project.id)
    }

    fun clearExportState() {
        com.example.utils.VideoExportService.exportFilePath.value = null
        com.example.utils.VideoExportService.exportMessage.value = null
        com.example.utils.VideoExportService.exportProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine?.shutdown()
        ttsEngine = null
    }
}

class ProjectViewModelFactory(
    private val repository: ProjectRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(repository, dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
