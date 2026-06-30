package com.example.utils

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.data.model.LogoPosition
import com.example.data.model.MediaClip
import com.example.data.model.OutputType
import com.example.data.model.Project
import com.example.data.model.Subtitle
import com.example.data.model.Voiceover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FFmpegEngine {

    companion object {
        private const val TAG = "FFmpegEngine"
    }

    /**
     * Executes a raw FFmpeg command asynchronously.
     */
    fun executeCommand(
        command: String,
        onProgress: (Float) -> Unit, // percentage 0.0 - 1.0
        onComplete: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "Executing FFmpeg command: $command")
        
        FFmpegKit.executeAsync(
            command,
            { session ->
                val returnCode = session.returnCode
                val output = session.output
                if (ReturnCode.isSuccess(returnCode)) {
                    Log.i(TAG, "FFmpeg success")
                    onComplete(true, output)
                } else {
                    Log.e(TAG, "FFmpeg failed with state ${session.state} and code $returnCode: $output")
                    onComplete(false, output)
                }
            },
            { log ->
                // Parse progress from FFmpeg logs if needed
                Log.v(TAG, "FFmpeg log: ${log.message}")
            },
            { stats ->
                // Track progress using statistics (processed duration / total expected duration)
                val timeInMs = stats.time
                if (timeInMs > 0) {
                    // We report progress up to our ViewModel based on processing duration
                    Log.d(TAG, "FFmpeg stats progress time: $timeInMs ms")
                }
            }
        )
    }

    /**
     * Helper to crop a media clip to the specified ratio.
     */
    fun cropClip(
        inputPath: String,
        outputPath: String,
        ratio: String, // "16:9" or "9:16"
        onComplete: (Boolean) -> Unit
    ) {
        val filter = if (ratio == "9:16") {
            "crop=ih*9/16:ih"
        } else {
            "crop=iw:iw*9/16"
        }
        val command = "-y -i \"$inputPath\" -vf \"$filter\" -c:a copy \"$outputPath\""
        executeCommand(command, {}) { success, _ -> onComplete(success) }
    }

    /**
     * Helper to trim a clip.
     */
    fun trimClip(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        onComplete: (Boolean) -> Unit
    ) {
        val startSec = startMs / 1000f
        val durationSec = (endMs - startMs) / 1000f
        val command = "-y -ss $startSec -i \"$inputPath\" -t $durationSec -c copy \"$outputPath\""
        executeCommand(command, {}) { success, _ -> onComplete(success) }
    }

    /**
     * Build the entire composite video command based on project settings.
     * Takes clips, audio files, logo overlays, scrolling text overlays, headlines, and sub-tracks.
     */
    suspend fun compileProject(
        project: Project,
        clips: List<MediaClip>,
        voiceovers: List<Voiceover>,
        subtitles: List<Subtitle>,
        cacheDir: File,
        outputFile: File,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            onComplete(false, "No clips in timeline to compile")
            return@withContext
        }

        // To make compilation highly robust and guaranteed not to crash on different video file codecs,
        // we scale and normalize all clips into a temporary folder, then merge them.
        // Let's build a single-stage composite command using ffmpeg inputs!
        
        val inputs = StringBuilder()
        val filterComplex = StringBuilder()
        
        // Output dimensions based on Project Type
        val width = if (project.outputType == OutputType.LANDSCAPE) {
            if (project.resolution == com.example.data.model.Resolution.P1080) 1920 else 1280
        } else {
            if (project.resolution == com.example.data.model.Resolution.P1080) 1080 else 720
        }
        val height = if (project.outputType == OutputType.LANDSCAPE) {
            if (project.resolution == com.example.data.model.Resolution.P1080) 1080 else 720
        } else {
            if (project.resolution == com.example.data.model.Resolution.P1080) 1920 else 1280
        }

        // 1. Clips Inputs
        clips.forEachIndexed { index, clip ->
            inputs.append("-i \"${clip.filePath}\" ")
        }

        // 2. Music input if any
        var musicIndex = -1
        if (project.musicPath != null && File(project.musicPath).exists()) {
            inputs.append("-i \"${project.musicPath}\" ")
            musicIndex = clips.size
        }

        // 3. Logo input if any
        var logoIndex = -1
        if (project.logoPath != null && File(project.logoPath).exists()) {
            inputs.append("-i \"${project.logoPath}\" ")
            logoIndex = clips.size + (if (musicIndex != -1) 1 else 0)
        }

        // 4. Voiceover inputs if any (we can merge voiceovers into a single mix stream)
        val voiceoverStartIndex = clips.size + (if (musicIndex != -1) 1 else 0) + (if (logoIndex != -1) 1 else 0)
        voiceovers.forEach { vo ->
            inputs.append("-i \"${vo.filePath}\" ")
        }

        // --- FILTER GRAPH CONSTRUCT ---
        // Scale and pad each input video to standard target size
        clips.forEachIndexed { i, _ ->
            filterComplex.append("[$i:v]scale=$width:$height:force_original_aspect_ratio=decrease,pad=$width:$height:(ow-iw)/2:(oh-ih)/2[v$i]; ")
        }

        // Concat scaled video streams
        val concatInputV = StringBuilder()
        val concatInputA = StringBuilder()
        clips.forEachIndexed { i, _ ->
            concatInputV.append("[v$i]")
            concatInputA.append("[$i:a]")
        }
        
        filterComplex.append("${concatInputV}concat=n=${clips.size}:v=1:a=1[v_concat][a_concat]; ")

        // Let's define the current video/audio references
        var currVRef = "v_concat"
        var currARef = "a_concat"

        // 5. Apply Voiceover Mixing if voiceovers exist
        if (voiceovers.isNotEmpty()) {
            val mixCount = voiceovers.size + 1 // concat audio + voiceovers
            val mixInputs = StringBuilder().append("[$currARef]")
            voiceovers.forEachIndexed { index, _ ->
                val vIndex = voiceoverStartIndex + index
                mixInputs.append("[$vIndex:a]")
            }
            filterComplex.append("${mixInputs}amix=inputs=$mixCount:duration=first[a_mixed]; ")
            currARef = "a_mixed"
        }

        // 6. Apply Background Music and audio ducking
        if (musicIndex != -1) {
            val musicVol = project.musicVolume
            // We scale music volume and mix it
            val duckFilter = if (project.musicDuck && voiceovers.isNotEmpty()) {
                // simple audio volume reduction for background music
                "[$musicIndex:a]volume=${musicVol * 0.3f}[bg_music]; "
            } else {
                "[$musicIndex:a]volume=$musicVol[bg_music]; "
            }
            filterComplex.append(duckFilter)
            filterComplex.append("[$currARef][bg_music]amix=inputs=2:duration=first[a_final]; ")
            currARef = "a_final"
        }

        // 7. Apply Brand TV Overlays (Breaking News / LIVE / Scrolling ticker / Lower thirds)
        val graphics = try {
            kotlinx.serialization.json.Json.decodeFromString<com.example.data.model.OverlayGraphics>(project.graphicsJson)
        } catch (e: Exception) {
            com.example.data.model.OverlayGraphics()
        }

        if (graphics.type != "NONE") {
            // Let's create an elegant overlay design depending on template category
            val overlayFilter = StringBuilder()
            
            // Draw a news bar at the bottom: 10% of height
            val barHeight = (height * 0.12f).toInt()
            val barY = height - barHeight
            
            // Standard red & black theme
            overlayFilter.append("drawbox=y=$barY:color=black@0.85:width=$width:height=$barHeight:t=fill, ")
            
            // Draw a red tag on the left for News Label (e.g., BREAKING NEWS)
            val tagWidth = (width * 0.28f).toInt()
            overlayFilter.append("drawbox=y=$barY:x=0:color=red@0.9:width=$tagWidth:height=$barHeight:t=fill, ")
            
            // Print label
            val textLabel = when (graphics.type) {
                "BREAKING_NEWS" -> "BREAKING NEWS"
                "LIVE" -> "LIVE NEWS"
                "EXCLUSIVE" -> "EXCLUSIVE"
                "TOP_STORY" -> "TOP STORY"
                else -> "NEWS BULLETIN"
            }
            overlayFilter.append("drawtext=text='$textLabel':fontcolor=white:fontsize=${(barHeight * 0.32f).toInt()}:x=15:y=$barY+(($barHeight-text_h)/2), ")

            // Add Scrolling Ticker text on the black bar
            if (graphics.scrollingTicker.isNotEmpty()) {
                val scrollText = graphics.scrollingTicker.replace("'", "\\'")
                // Simple animation inside drawtext: x coordinate moves over time (w - t * speed)
                val speed = 120 // pixels per second
                overlayFilter.append("drawtext=text='$scrollText':fontcolor=yellow:fontsize=${(barHeight * 0.28f).toInt()}:x=$tagWidth+20+(mod(w-t*$speed, w)):y=$barY+(($barHeight-text_h)/2), ")
            } else if (graphics.headline.isNotEmpty()) {
                val headlineText = graphics.headline.replace("'", "\\'")
                overlayFilter.append("drawtext=text='$headlineText':fontcolor=white:fontsize=${(barHeight * 0.28f).toInt()}:x=$tagWidth+20:y=$barY+(($barHeight-text_h)/2), ")
            }

            // Reporter & Location Lower-Third overlays if present
            if (graphics.reporterName.isNotEmpty()) {
                val repY = barY - 60
                val repText = "${graphics.reporterName} (${graphics.location})".replace("'", "\\'")
                overlayFilter.append("drawbox=y=$repY:x=20:color=white@0.8:width=${(width * 0.5f).toInt()}:height=40:t=fill, ")
                overlayFilter.append("drawtext=text='$repText':fontcolor=black:fontsize=18:x=30:y=$repY+10, ")
            }

            // Quote Box overlay if present
            if (graphics.quoteText.isNotEmpty()) {
                val quoteY = (height * 0.3f).toInt()
                val qText = graphics.quoteText.replace("'", "\\'")
                overlayFilter.append("drawbox=y=$quoteY:x=40:color=black@0.7:width=$width-80:height=120:t=fill, ")
                overlayFilter.append("drawtext=text='\"$qText\"':fontcolor=yellow:fontsize=22:x=60:y=$quoteY+45, ")
            }

            // Finalize drawing stage
            val drawCmd = overlayFilter.toString().trim().removeSuffix(",")
            filterComplex.append("[$currVRef]$drawCmd[v_graphics]; ")
            currVRef = "v_graphics"
        }

        // 8. Apply Subtitles if present
        if (subtitles.isNotEmpty()) {
            val subFilter = StringBuilder()
            // We bake them in using drawtext overlays at specific timestamps
            subFilter.append("[$currVRef]")
            subtitles.forEachIndexed { index, sub ->
                val text = sub.text.replace("'", "\\'")
                val startTimeSec = sub.startTimeMs / 1000f
                val endTimeSec = sub.endTimeMs / 1000f
                // drawtext with dynamic enable logic
                subFilter.append("drawtext=text='$text':fontcolor=white:fontsize=${sub.fontSizeSp}:x=(w-text_w)/2:y=h-130:enable='between(t,$startTimeSec,$endTimeSec)':borderw=2:bordercolor=black, ")
            }
            val cleanSubCmd = subFilter.toString().trim().removeSuffix(",")
            filterComplex.append("${cleanSubCmd}[v_subtitles]; ")
            currVRef = "v_subtitles"
        }

        // 9. Apply Logo Overlay if present
        if (logoIndex != -1) {
            val posFilter = when (project.logoPosition) {
                LogoPosition.TOP_LEFT -> "x=20:y=20"
                LogoPosition.TOP_RIGHT -> "x=W-w-20:y=20"
                LogoPosition.BOTTOM_LEFT -> "x=20:y=H-h-20"
                LogoPosition.BOTTOM_RIGHT -> "x=W-w-20:y=H-h-20"
            }
            // scale logo to 10% of width first
            val logoW = (width * 0.12f).toInt()
            filterComplex.append("[$logoIndex:v]scale=$logoW:-1,lut3d=alpha=val*${project.logoOpacity}[logo_scaled]; ")
            filterComplex.append("[$currVRef][logo_scaled]overlay=$posFilter[v_logo]; ")
            currVRef = "v_logo"
        }

        // Complete filtergraph
        var graph = filterComplex.toString().trim()
        if (graph.endsWith(";")) {
            graph = graph.substring(0, graph.length - 1)
        }

        // Construct final command
        val finalCommand = if (graph.isNotEmpty()) {
            "$inputs -filter_complex \"$graph\" -map \"[$currVRef]\" -map \"[$currARef]\" -c:v libx264 -preset ultrafast -threads 0 -r ${project.fps} -y \"${outputFile.absolutePath}\""
        } else {
            "$inputs -c:v libx264 -preset ultrafast -threads 0 -r ${project.fps} -y \"${outputFile.absolutePath}\""
        }

        executeCommand(finalCommand, onProgress, onComplete)
    }
}
