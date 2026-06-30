package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class OutputType {
    REEL,      // 9:16
    SHORTS,    // 9:16
    LANDSCAPE  // 16:9
}

@Serializable
enum class Resolution {
    P720, P1080
}

@Serializable
enum class LogoPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Serializable
data class MediaClip(
    val id: String,
    val type: String, // "VIDEO" or "IMAGE"
    val filePath: String,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val cropRatio: String = "16:9", // or "9:16"
    val speed: Float = 1.0f,
    val transitionType: String = "NONE" // "NONE", "WIPE", "FADE", "SPIN_LOGO", "SLIDE"
)

@Serializable
data class Voiceover(
    val id: String,
    val type: String, // "RECORDING", "TTS_EN", "TTS_BN"
    val filePath: String,
    val durationMs: Long,
    val timelineStartMs: Long
)

@Serializable
data class Subtitle(
    val id: String,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val fontSizeSp: Int = 18,
    val outlineColor: String = "BLACK",
    val shadowEnabled: Boolean = true
)

@Serializable
data class OverlayGraphics(
    val type: String = "NONE", // "BREAKING_NEWS", "LIVE", "EXCLUSIVE", "TOP_STORY"
    val headline: String = "",
    val scrollingTicker: String = "",
    val reporterName: String = "",
    val location: String = "",
    val quoteText: String = "",
    val showDate: Boolean = true,
    val showTime: Boolean = true,
    val lowerThirdName: String = "",
    val lowerThirdTitle: String = "",
    val lowerThirdStyle: String = "RED_SLEEK", // "RED_SLEEK", "GLASSMORPHISM", "DARK_NEON", "CLASSIC_BLUE"
    val lowerThirdEnabled: Boolean = false
)

@Entity(tableName = "projects")
@Serializable
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val outputType: OutputType,
    val resolution: Resolution,
    val fps: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Serialized timeline assets
    val clipsJson: String = "[]",
    val voiceoversJson: String = "[]",
    val subtitlesJson: String = "[]",
    val graphicsJson: String = "{}",
    
    // Watermark Logo
    val logoPath: String? = null,
    val logoPosition: LogoPosition = LogoPosition.TOP_RIGHT,
    val logoOpacity: Float = 0.8f,
    val logoAlwaysShow: Boolean = true,
    
    // Background Music
    val musicPath: String? = null,
    val musicVolume: Float = 0.5f,
    val musicFadeIn: Boolean = false,
    val musicFadeOut: Boolean = false,
    val musicDuck: Boolean = false,
    
    // Template Style
    val templateCategory: String = "NORMAL" // "BREAKING", "POLITICS", "SPORTS", "WEATHER", "INTERNATIONAL"
)
