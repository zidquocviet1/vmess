package com.mqv.vmess.webrtc

import android.content.Context
import org.webrtc.Camera2Enumerator

data class WebRtcControl(
    val isVideoEnable: Boolean,
    val isMicEnable: Boolean,
    val callState: CallState,
    val callType: CallType
) {
    fun displayCameraToggleButton(): Boolean =
        (isPreConnected() || isAtLeastOutgoing()) && isVideoCall()

    fun displayMicToggleButton(): Boolean = isPreConnected() || isAtLeastOutgoing()

    fun displaySpeakerToggleButton(): Boolean = isPreConnected() || isAtLeastOutgoing()

    fun displayFlipToggleButton(context: Context): Boolean =
        (isPreConnected() || isAtLeastOutgoing()) && WebRtcUtil.isDeviceHasFrontAndRearCamera(Camera2Enumerator(context)) && isVideoCall()

    fun displayIncomingCall(): Boolean = isIncoming()

    fun displayHangUpButton(): Boolean = isPreConnected() || isOutgoing() || isReconnecting() || isOngoing()

    fun displayOngoingCall(): Boolean = isOngoing()

    private fun isVideoCall(): Boolean = callType == CallType.VIDEO

    private fun isAudioCall(): Boolean = callType == CallType.AUDIO

    private fun isPreConnected(): Boolean = callState == CallState.PRE_CONNECTED

    private fun isReconnecting(): Boolean = callState == CallState.RECONNECTING

    private fun isIncoming(): Boolean = callState == CallState.INCOMING

    private fun isOutgoing(): Boolean = callState == CallState.OUTGOING

    private fun isEnding(): Boolean = callState == CallState.ENDING

    private fun isOngoing(): Boolean = callState == CallState.ONGOING

    private fun isAtLeastOutgoing(): Boolean = callState.isAtLeast(CallState.OUTGOING)

    companion object {
        val NONE = WebRtcControl(
            isVideoEnable = false,
            isMicEnable = false,
            callState = CallState.NONE,
            callType = CallType.NONE
        )
    }
}

enum class CallType {
    NONE,
    VIDEO,
    AUDIO
}

enum class CallState {
    NONE,
    PRE_CONNECTED,
    RECONNECTING,
    INCOMING,
    OUTGOING,
    ONGOING,
    ENDING;

    fun isAtLeast(other: CallState): Boolean = compareTo(other) >= 0
}
