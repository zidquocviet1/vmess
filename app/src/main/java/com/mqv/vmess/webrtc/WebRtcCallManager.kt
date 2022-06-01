package com.mqv.vmess.webrtc

import android.content.Context
import android.content.Intent
import com.mqv.vmess.activity.WebRtcCallActivity
import com.mqv.vmess.activity.br.DeclineReceiver
import com.mqv.vmess.activity.service.CallNotificationService
import com.mqv.vmess.network.exception.FirebaseUnauthorizedException
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Logging
import okio.ByteString.Companion.toByteString
import org.webrtc.*
import java.nio.charset.Charset

class WebRtcCallManager(private val context: Context) :
    PeerConnection.Observer, SdpObserver, DataChannel.Observer {
    private val callManager: CallManager = CallManager.createCallManager(this, this, this)
    private val pendingCandidate: MutableList<IceCandidate> = mutableListOf()

    private lateinit var serviceModel: WebRtcServiceModel
    private lateinit var currentType: SessionDescription.Type

    private var lifecycle: RtcCallLifecycle? = null
    private var rtcCallOption: RtcCallOption? = null
    private var localSink: VideoSink? = null
    private var remoteSink: VideoSink? = null
    private var isIdle: Boolean = true
    private var remoteAudioTrack: List<AudioTrack> = listOf()

    val shouldShowNotification: Boolean get() = isIdle
    val eglContext: EglBase.Context get() = callManager.eglContext

    fun registerLifecycle(lifecycle: RtcCallLifecycle) {
        this.lifecycle = lifecycle
    }

    fun registerRtcCallOption(rtcCallOption: RtcCallOption) {
        this.rtcCallOption = rtcCallOption
    }

    fun unregisterRtcCallOption() {
        this.rtcCallOption = null
    }

    fun unregisterLifecycle() {
        this.lifecycle = null
    }

    fun setServiceModel(serviceModel: WebRtcServiceModel) {
        this.serviceModel = serviceModel
    }

    fun createConnection(isVideoEnabled: Boolean) {
        callManager.createPeerConnection(isVideoEnabled)
    }

    fun startVideoCapture(isFrontCamera: Boolean) {
        callManager.startVideoCapture(context, isFrontCamera)
    }

    fun sendOffer() {
        lifecycle?.onStart()
        isIdle = false
        callManager.sendSessionDescriptionOffer()
    }

    fun sendAnswer(offerSdp: SessionDescription) {
        currentType = SessionDescription.Type.OFFER

        callManager.setRemoteSdp(offerSdp)
        isIdle = false
//        callManager.sendSessionDescriptionAnswer()
    }

    fun setRemoteSdp(sdp: SessionDescription) {
        callManager.setRemoteSdp(sdp)
    }

    fun setSinks(remoteSink: VideoSink?, localSink: VideoSink?) {
        this.remoteSink = remoteSink
        this.localSink = localSink

        remoteSink?.let { callManager.setSinkForLocalVideoTrack(it) }
    }

    fun addIceCandidate(candidate: IceCandidate) {
        callManager.addIceCandidate(candidate)
    }

    fun setCameraEnable(isVideoEnabled: Boolean) {
        callManager.setCameraEnable(isVideoEnabled)
    }

    fun flipCameraDirection(isFrontCamera: Boolean) {
        callManager.flipCameraDirection(context, isFrontCamera)
    }

    fun handleMicStatus(isMicEnabled: Boolean) {
        callManager.changeLocalAudioStatus(isMicEnabled)
    }

    fun handleSpeakerStatus(isSpeakerEnabled: Boolean) {
        remoteAudioTrack.forEach { track ->
            track.setEnabled(isSpeakerEnabled)
        }
    }

    fun stopCall() {
        context.stopService(Intent(context, CallNotificationService::class.java))

        serviceModel.compositeDisposable.add(
            serviceModel.rtcRepository.stopCall(serviceModel.recipient)
                .compose(RxHelper.applyObservableSchedulers())
                .doOnError { Logging.debug(WebRtcCallActivity.TAG, "Can't make a stop call") }
                .onErrorComplete()
                .subscribe {}
        )
    }

    fun denyCall(recipient: String) {
        val intent = Intent(context, DeclineReceiver::class.java).apply {
            putExtra(DeclineReceiver.EXTRA_RECIPIENT_ID, recipient)
        }
        context.sendBroadcast(intent)
    }

    fun dispose() {
        isIdle = true
        callManager.dispose()
    }

    private fun sendIceCandidateToSignalingServer(candidate: IceCandidate) {
        Logging.debug(TAG, "Add remote ice candidate")

        serviceModel.compositeDisposable.add(
            serviceModel.rtcRepository.addIceCandidate(
                serviceModel.recipient,
                WebRtcCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
            )
                .compose(RxHelper.applyObservableSchedulers())
                .doOnError {
                    Logging.debug(
                        WebRtcCallActivity.TAG,
                        "Can't send ice candidate: ${it.message}"
                    )
                }
                .onErrorComplete()
                .subscribe()
        )
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        super.onConnectionChange(newState)

        when (newState) {
            PeerConnection.PeerConnectionState.FAILED -> {
                isIdle = true
                lifecycle?.onFailed()
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                isIdle = true
                lifecycle?.onDisconnected()
            }
            else -> {}
        }
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Logging.debug(TAG, "onSignalingChange: ${p0.toString()}")
    }

    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
        Logging.debug(TAG, "onIceConnectionChange: ${state.name}")

        when (state) {
            PeerConnection.IceConnectionState.CONNECTED -> {
                isIdle = false
                lifecycle?.onConnected()
                remoteSink?.let { callManager.removeSinkForLocalVideoTrack(it) }
                localSink?.let { callManager.setSinkForLocalVideoTrack(it) }
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                isIdle = true
                lifecycle?.onDisconnected()
            }
            PeerConnection.IceConnectionState.CHECKING -> {
                isIdle = false
                lifecycle?.onConnecting()
            }
            else -> {}
        }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Logging.debug(TAG, "onIceConnectionReceivingChange")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Logging.debug(TAG, "onIceGatheringChange: ${p0.toString()}")
        if (p0 == PeerConnection.IceGatheringState.GATHERING) {
            lifecycle?.onConnecting()
        }
    }

    override fun onIceCandidate(p0: IceCandidate) {
        val id = p0.sdpMid
        val index = p0.sdpMLineIndex
        val sdp = p0.sdp

        Logging.debug(TAG, "onIceCandidate: {$id%$index%$sdp}")

        if (serviceModel.isIncomingCall) {
            sendIceCandidateToSignalingServer(p0)
        } else {
            pendingCandidate.add(p0)
        }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {
        Logging.debug(TAG, "onIceCandidatesRemoved")
    }

    override fun onAddStream(stream: MediaStream) {
        Logging.debug(TAG, "onAddStream: $stream")

        if (stream.videoTracks == null) {
            Logging.debug(TAG, "Remote media stream contains no video track")
        } else if (stream.audioTracks == null) {
            Logging.debug(TAG, "Remote media stream contains no audio track")
        } else {
            remoteAudioTrack = stream.audioTracks

            for (audio in stream.audioTracks) {
                audio.setEnabled(true)
            }

            if (stream.videoTracks.size > 1) {
                Logging.debug(
                    TAG,
                    "Remote media stream receive unexpected video tracks: ${stream.videoTracks.size}"
                )
            } else {
                if (stream.videoTracks.isNotEmpty() && remoteSink != null) {
                    stream.videoTracks[0].run {
                        setEnabled(true)
                        addSink(remoteSink)
                    }
                } else {
                    Logging.debug(TAG, "Remote media stream don't have any video tracks")
                }
            }
        }
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Logging.debug(TAG, "onRemoveStream")
    }

    override fun onDataChannel(p0: DataChannel) {
        Logging.debug(TAG, "onDataChannel: $p0")

        p0.registerObserver(this)
    }

    override fun onRenegotiationNeeded() {
        Logging.debug(TAG, "onRenegotiationNeeded")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Logging.debug(TAG, "onAddTrack")
    }

    override fun onCreateSuccess(sdp: SessionDescription) {
        callManager.setLocalSdp(sdp)

        val rtcRepository = serviceModel.rtcRepository
        val participantId = serviceModel.recipient
        val isVideoEnabled = serviceModel.isVideoEnabled

        val observable = if (sdp.type == SessionDescription.Type.OFFER) {
            currentType = SessionDescription.Type.OFFER

            Logging.debug(TAG, "Make remote to send OFFER sdp description")

            rtcRepository.makeCall(
                participantId,
                "\"${sdp.description}\"",
                isVideoEnabled
            )
        } else {
            currentType = SessionDescription.Type.ANSWER

            Logging.debug(TAG, "Make remote to send ANSWER sdp description")

            rtcRepository.responseCall(participantId, "\"${sdp.description}\"")
        }

        serviceModel.compositeDisposable.add(
            observable
                .compose(RxHelper.applyObservableSchedulers())
                .subscribe({
                }, { t ->
                    if (t is FirebaseUnauthorizedException) {
                    } else {
                    }
                })
        )
    }

    override fun onSetSuccess() {
        Logging.debug(TAG, "Set ${currentType.name} sdp success")

        if (currentType == SessionDescription.Type.OFFER) {
            callManager.sendSessionDescriptionAnswer()
        } else {
            if (!serviceModel.isIncomingCall) {
                pendingCandidate.forEach { sendIceCandidateToSignalingServer(it) }
            }
        }
    }

    override fun onCreateFailure(p0: String?) {
    }

    override fun onSetFailure(p0: String?) {
    }

    companion object {
        val TAG: String = WebRtcCallManager::class.java.simpleName
    }

    override fun onBufferedAmountChange(p0: Long) {
    }

    override fun onStateChange() {
    }

    override fun onMessage(p0: DataChannel.Buffer) {
        Logging.debug(TAG, "Got message from data channel")

        when (p0.data.toByteString().string(Charset.defaultCharset())) {
            CallManager.CHANNEL_DATA_CAMERA_FRONT -> {
                Logging.debug(TAG, "Remote peer notify change camera to front")

                rtcCallOption?.onCameraDirectionChanged(true)
            }
            CallManager.CHANNEL_DATA_CAMERA_REAR -> {
                Logging.debug(TAG, "Remote peer notify change camera to rear")

                rtcCallOption?.onCameraDirectionChanged(false)
            }
            else -> {
                Logging.debug(TAG, "Cannot realize the message from remote peer")
            }
        }
    }
}

interface RtcCallLifecycle {
    fun onStart()
    fun onConnecting()
    fun onConnected()
    fun onFailed()
    fun onDisconnected()
}

interface RtcCallOption {
    fun onCameraDirectionChanged(isFrontCamera: Boolean)
}
