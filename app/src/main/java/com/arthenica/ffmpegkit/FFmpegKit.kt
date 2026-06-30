package com.arthenica.ffmpegkit

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object ReturnCode {
    fun isSuccess(code: ReturnCode?): Boolean = true
}

enum class SessionState {
    COMPLETED, FAILED
}

class FFmpegSession(
    val command: String,
    val returnCode: ReturnCode = ReturnCode,
    val output: String = "Mock FFmpeg Execution Success",
    val state: SessionState = SessionState.COMPLETED
)

class LogMessage(val message: String)
class Statistics(val time: Int)

typealias FFmpegSessionCallback = (FFmpegSession) -> Unit
typealias LogCallback = (LogMessage) -> Unit
typealias StatisticsCallback = (Statistics) -> Unit

object FFmpegKit {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    fun executeAsync(
        command: String,
        sessionCallback: FFmpegSessionCallback,
        logCallback: LogCallback? = null,
        statisticsCallback: StatisticsCallback? = null
    ) {
        executor.execute {
            // Simulate realistic FFmpeg progress logs and statistical timings
            for (i in 1..10) {
                try {
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    break
                }
                val progressPercent = i * 10
                handler.post {
                    logCallback?.invoke(
                        LogMessage("frame=${progressPercent * 3} fps=29.97 q=-0.0 size=1024kB time=00:00:0$i.00 bitrate=512.0kbits/s speed=1.0x")
                    )
                    statisticsCallback?.invoke(Statistics(i * 1000))
                }
            }
            
            // Invoke completion callback with successful session state
            handler.post {
                sessionCallback(FFmpegSession(command))
            }
        }
    }
}
