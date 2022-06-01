package com.mqv.vmess.webrtc

import android.content.Context
import com.mqv.vmess.util.Logging
import org.webrtc.*
import java.nio.ByteBuffer

class CallManager private constructor(
    private val observer: PeerConnection.Observer,
    private val simpleObserver: SdpObserver,
    private val dataChannelObserver: DataChannel.Observer
) {
    private lateinit var factory: PeerConnectionFactory
    private lateinit var connection: PeerConnection
    private lateinit var mediaConstraints: MediaConstraints
    private lateinit var mediaStream: MediaStream

    private var dataChannel: DataChannel? = null
    private val eglBase = EglBase.create()
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var isVideoEnabled: Boolean = false

    val eglContext: EglBase.Context get() = eglBase.eglBaseContext

    private fun createConnectionFactory(eglContext: EglBase.Context) {
        val encoderFactory =
            DefaultVideoEncoderFactory(eglContext, true, true)
        val decoderFactory =
            DefaultVideoDecoderFactory(eglContext)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private fun createRtcConfig(): PeerConnection.RTCConfiguration =
        PeerConnection.RTCConfiguration(listOf(createIceServers())).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.BALANCED
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            enableDtlsSrtp = true
        }

    private fun createIceServers(): PeerConnection.IceServer =
        PeerConnection.IceServer.builder(serverUrls)
            .setPassword(PASSWORD)
            .setUsername(USERNAME)
            .createIceServer()

    fun createPeerConnection(isVideoEnabled: Boolean) {
        createConnectionFactory(eglBase.eglBaseContext)

        Logging.debug(TAG, "Initialize peer connection")

        val connection = factory.createPeerConnection(createRtcConfig(), observer)
        if (connection == null) {
            throw IllegalArgumentException()
        } else {
            this.connection = connection
        }
        val init = DataChannel.Init().apply {
            negotiated = true
            id = 0
        }
        dataChannel = connection.createDataChannel(DATA_CHANNEL_LABEL, init)
        dataChannel?.registerObserver(dataChannelObserver)
        mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair(KEY_OFFER_AUDIO, "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    KEY_OFFER_VIDEO,
                    if (isVideoEnabled) "true" else "false"
                )
            )
        }

        this.isVideoEnabled = isVideoEnabled

//        connection.setAudioPlayout(false)
//        connection.setAudioRecording(false)

        mediaStream = factory.createLocalMediaStream(LOCAL_MEDIA_STREAM_LABEL)

        val audioSource = factory.createAudioSource(mediaConstraints)
        val audioTrack = factory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource)

        audioTrack.setEnabled(true)
        audioTrack.setVolume(100.0)

        mediaStream.addTrack(audioTrack)
        connection.addStream(mediaStream)
    }

    private fun createCameraCapturer(
        enumerator: CameraEnumerator,
        isFrontCamera: Boolean
    ): VideoCapturer? {
        for (deviceName in WebRtcUtil.availableCameraDevices(enumerator)) {
            if (isFrontCamera) {
                Logging.debug(TAG, "Looking for front facing cameras.")

                if (enumerator.isFrontFacing(deviceName)) {
                    Logging.debug(TAG, "Creating front facing camera capturer.")

                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            } else {
                Logging.debug(TAG, "Looking for rear facing cameras.")

                if (!enumerator.isFrontFacing(deviceName)) {
                    Logging.debug(TAG, "Creating other camera capturer.")

                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }
        }
        return null
    }

    fun sendSessionDescriptionOffer() {
        connection.createOffer(simpleObserver, mediaConstraints)
    }

    fun sendSessionDescriptionAnswer() {
        connection.createAnswer(simpleObserver, mediaConstraints)
    }

    fun setLocalSdp(sdp: SessionDescription) {
        connection.setLocalDescription(simpleObserver, sdp)
    }

    fun setRemoteSdp(sdp: SessionDescription) {
        connection.setRemoteDescription(simpleObserver, sdp)
    }

    fun setCameraEnable(isVideoEnabled: Boolean) {
        mediaStream.videoTracks.forEach { track ->
            track.setEnabled(isVideoEnabled)
        }
    }

    fun flipCameraDirection(context: Context, isFrontCamera: Boolean) {
        startVideoCaptureInternal(context, isFrontCamera)
    }

    fun changeLocalAudioStatus(isMicEnabled: Boolean) {
        mediaStream.audioTracks.forEach { audio ->
            audio.setEnabled(isMicEnabled)
        }
    }

    fun startVideoCapture(context: Context, isFrontCamera: Boolean) {
        videoSource = factory.createVideoSource(false)
        videoTrack = factory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, videoSource)
        videoTrack?.setEnabled(true)
        mediaStream.addTrack(videoTrack)

        startVideoCaptureInternal(context, isFrontCamera)
    }

    private fun startVideoCaptureInternal(context: Context, isFrontCamera: Boolean) {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = createCameraCapturer(Camera2Enumerator(context), isFrontCamera)!!
        videoCapturer!!.initialize(
            SurfaceTextureHelper.create(
                VIDEO_THREAD_NAME,
                eglBase.eglBaseContext
            ),
            context,
            videoSource!!.capturerObserver
        )
        videoCapturer!!.startCapture(720, 1280, 30)

        dataChannel?.send(
            DataChannel.Buffer(
                ByteBuffer.wrap(
                    (if (isFrontCamera) CHANNEL_DATA_CAMERA_FRONT else CHANNEL_DATA_CAMERA_REAR).toByteArray()
                ),
                false
            )
        )
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        connection.addIceCandidate(iceCandidate)
    }

    fun setSinkForLocalVideoTrack(sink: VideoSink) {
        videoTrack?.addSink(sink)
    }

    fun removeSinkForLocalVideoTrack(sink: VideoSink) {
        videoTrack?.removeSink(sink)
    }

    fun dispose() {
        videoCapturer?.dispose()
        dataChannel?.unregisterObserver()
        dataChannel?.dispose()

        if (::connection.isInitialized) {
            connection.close()
        }
    }

    companion object {
        private val TAG: String = CallManager::class.java.simpleName
        private const val LOCAL_MEDIA_STREAM_LABEL = "ARDAMS"
        private const val LOCAL_VIDEO_TRACK_ID = "ARDAMSv0"
        private const val LOCAL_AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_THREAD_NAME = "Capture-thread"
        private const val DATA_CHANNEL_LABEL = "ARDAMSc0"
        private const val USERNAME = "1653380987:858576220"
        private const val PASSWORD = "NbPNo96Z3tb+dkO0YO+q+Sgexag="
        private val serverUrls = listOf(
            "stun:stun.l.google.com:19302",
            "turn:ipv6.turn3.voip.signal.org:443?transport=udp",
            "turn:turn3.voip.signal.org",
            "turn:turn3.voip.signal.org:443?transport=udp",
            "turn:turn3.voip.signal.org:80?transport=tcp",
            "turn:ipv6.turn3.voip.signal.org",
            "turn:ipv6.turn3.voip.signal.org:80?transport=tcp"
        )

        const val KEY_OFFER_VIDEO = "OfferToReceiveVideo"
        const val KEY_OFFER_AUDIO = "OfferToReceiveAudio"
        const val CHANNEL_DATA_CAMERA_FRONT = "CAMERA_FRONT"
        const val CHANNEL_DATA_CAMERA_REAR = "CAMERA_REAR"
//        private const val USERNAME = "openrelayproject"
//        private const val PASSWORD = "openrelayproject"
//        private val serverUrls = listOf(
//            "stun:openrelay.metered.ca:80",
//            "turn:openrelay.metered.ca:443",
//            "turn:openrelay.metered.ca:443?transport=tcp"
//        )

        fun createCallManager(
            observer: PeerConnection.Observer,
            simpleObserver: SdpObserver,
            dataChannelObserver: DataChannel.Observer
        ): CallManager {
            return CallManager(observer, simpleObserver, dataChannelObserver)
        }

        @JvmStatic
        fun initialize(context: Context) {
            Logging.debug(TAG, "CallManager.initialize()")

            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
        }
    }
}