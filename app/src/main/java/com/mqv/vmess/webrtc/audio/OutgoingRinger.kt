package com.mqv.vmess.webrtc.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.mqv.vmess.R
import com.mqv.vmess.util.Logging
import java.io.IOException

class OutgoingRinger(private val context: Context) {
    enum class Type {
        RINGING,
        BUSY,
        DISCONNECTED,
        COMPLETED
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun checkForAlreadyStarted() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) return
    }

    fun start(type: Type) {
        checkForAlreadyStarted()

        val soundId: Int = when (type) {
            Type.RINGING -> R.raw.redphone_outring
            Type.BUSY -> R.raw.redphone_busy
            Type.DISCONNECTED -> R.raw.webrtc_disconnected
            Type.COMPLETED -> R.raw.webrtc_completed
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build()
            )
            isLooping = true
        }

        val packageName = context.packageName
        val dataUri = Uri.parse("android.resource://$packageName/$soundId")

        try {
            mediaPlayer!!.setDataSource(context, dataUri)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e: IllegalArgumentException) {
            Logging.debug(TAG, e.message)
        } catch (e: SecurityException) {
            Logging.debug(TAG, e.message)
        } catch (e: IllegalStateException) {
            Logging.debug(TAG, e.message)
        } catch (e: IOException) {
            Logging.debug(TAG, e.message)
        }
    }

    fun stop() {
        if (mediaPlayer == null) return
        mediaPlayer!!.release()
        mediaPlayer = null
    }

    companion object {
        private val TAG: String = OutgoingRinger::class.java.simpleName
    }
}