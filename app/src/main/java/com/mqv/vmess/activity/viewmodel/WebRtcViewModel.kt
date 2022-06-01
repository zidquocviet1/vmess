package com.mqv.vmess.activity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.mqv.vmess.R
import com.mqv.vmess.activity.WebRtcCallActivity
import com.mqv.vmess.data.DatabaseObserver
import com.mqv.vmess.data.repository.PeopleRepository
import com.mqv.vmess.data.repository.RtcRepository
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.User
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Logging
import com.mqv.vmess.webrtc.*
import com.mqv.vmess.webrtc.WebRtcUtil.formatSdpString
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
import javax.inject.Inject

@HiltViewModel
class WebRtcViewModel @Inject constructor(
    val repository: RtcRepository,
    val peopleRepository: PeopleRepository,
    val savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {
    private val cd = CompositeDisposable()
    private val _cameraDirection = MutableLiveData(Pair(true, true))
    private val _micEnabled = MutableLiveData(true)
    private val _remoteCameraEnabled = MutableLiveData(true)
    private val _webRtcControl = MutableLiveData(WebRtcControl.NONE)
    private val _user = MutableLiveData<User>()
    private val _state = MutableLiveData<WebRtcState>()
    private val _event = MutableLiveData<WebRtcEvent>()
    private val _countRingingTime = MutableLiveData(0)

    // Currently support only in personal conversation
    private val participantId: String =
        savedStateHandle.get<String>(WebRtcCallActivity.EXTRA_PARTICIPANT_ID)
            ?: throw RuntimeException("Can not specific participant")
    private val type = savedStateHandle.get<Int>(WebRtcCallActivity.EXTRA_TYPE)
        ?: throw RuntimeException("Type is not specified")
    private val isAnswerEnableVideo =
        savedStateHandle.get<Boolean>(WebRtcCallActivity.EXTRA_VIDEO_ENABLE) ?: false
    private val serviceModel = WebRtcServiceModel(
        repository,
        cd,
        participantId,
        (type == WebRtcCallActivity.TYPE_VIDEO_CALL) || isAnswerEnableVideo,
        type == WebRtcCallActivity.TYPE_ANSWER
    )
    private var pendingOfferSessionDescription: SessionDescription? = null
    private var hasAnswerCall: Boolean = false

    val user: LiveData<User> get() = _user
    val micEnabled: LiveData<Boolean> get() = _micEnabled
    val remoteCameraState: LiveData<Boolean> get() = _remoteCameraEnabled
    val webRtcControl: LiveData<WebRtcControl> get() = _webRtcControl
    val state: LiveData<WebRtcState> get() = _state
    val cameraDirection: LiveData<Pair<Boolean, Boolean>> get() = _cameraDirection
    val event: LiveData<WebRtcEvent> get() = _event
    val countRinging: LiveData<Int> get() = _countRingingTime
    val recipient: String = participantId

    init {
        AppDependencies.getDatabaseObserver().registerWebRtcCallListener(RtcCallObserver())
        handleTypeAndUpdateWebRtcControl()
        handleUserInformation()
    }

    private fun handleUserInformation() {
        cd.add(
            peopleRepository.getCachedByUid(participantId)
                .compose(RxHelper.applySingleSchedulers())
                .map { people ->
                    with(people) {
                        User.Builder()
                            .setUid(uid)
                            .setDisplayName(displayName)
                            .setPhotoUrl(photoUrl)
                            .create()
                    }
                }
                .onErrorReturnItem(
                    User.Builder()
                        .setUid("-1")
                        .setDisplayName("Unknown")
                        .create()
                )
                .subscribe { user, _ ->
                    _user.postValue(user)
                }
        )
    }

    private fun handleTypeAndUpdateWebRtcControl() {
        when (type) {
            WebRtcCallActivity.TYPE_AUDIO_CALL -> startCallInternal(false)
            WebRtcCallActivity.TYPE_VIDEO_CALL -> startCallInternal(true)
            WebRtcCallActivity.TYPE_ANSWER_FROM_NOTIFICATION -> _event.value = WebRtcEvent.TriggerAnswerFromNotification(isAnswerEnableVideo)
            else -> updateWebRtcControl(
                isVideoEnabled = isAnswerEnableVideo,
                isMicEnabled = true,
                CallState.INCOMING,
                if (isAnswerEnableVideo) CallType.VIDEO else CallType.AUDIO
            )
        }
    }

    private fun initializeConnection(isVideoEnabled: Boolean) {
        AppDependencies.getWebRtcCallManager().createConnection(isVideoEnabled)
        AppDependencies.getWebRtcCallManager().registerLifecycle(RtcCallLifecycleHandler())
        AppDependencies.getWebRtcCallManager().registerRtcCallOption(CallOptionState())
        AppDependencies.getWebRtcCallManager().setServiceModel(serviceModel)
    }

    private fun startCallInternal(isVideoEnabled: Boolean) {
        _event.value =
            WebRtcEvent.StartRinging(if (isVideoEnabled) CallType.VIDEO else CallType.AUDIO, -1)

        updateWebRtcControl(
            isVideoEnabled = isVideoEnabled,
            isMicEnabled = true,
            CallState.PRE_CONNECTED
        )
        initializeConnection(isVideoEnabled)

        if (isVideoEnabled) {
            AppDependencies.getWebRtcCallManager()
                .startVideoCapture(getCurrentCameraDirectionValue())
        }
        AppDependencies.getWebRtcCallManager().sendOffer()
    }

    fun stopCall() {
        AppDependencies.getWebRtcCallManager().stopCall()
    }

    fun answerCall(isVideoEnabled: Boolean) {
        Logging.debug(WebRtcCallActivity.TAG, "Create answer for call")

        hasAnswerCall = true

        initializeConnection(isVideoEnabled)

        if (isVideoEnabled) {
            AppDependencies.getWebRtcCallManager()
                .startVideoCapture(getCurrentCameraDirectionValue())
        }

        AppDependencies.getWebRtcCallManager().sendAnswer(pendingOfferSessionDescription!!)

        updateWebRtcControl(isVideoEnabled, true, CallState.ONGOING)
    }

    fun denyCall() {
        AppDependencies.getWebRtcCallManager().denyCall(participantId)
    }

    fun handleEnableVideoAfterConnected(isVideoEnabled: Boolean) {
        AppDependencies.getWebRtcCallManager().setCameraEnable(isVideoEnabled)
    }

    fun handleFlipCameraDirection() {
        flipCameraDirection()

        AppDependencies.getWebRtcCallManager().flipCameraDirection(getCurrentCameraDirectionValue())
    }

    fun handleMicStatus(isMicEnabled: Boolean) {
        AppDependencies.getWebRtcCallManager().handleMicStatus(isMicEnabled)
    }

    fun handleSpeakerStatus(isSpeakerEnabled: Boolean) {
        AppDependencies.getWebRtcCallManager().handleSpeakerStatus(isSpeakerEnabled)
    }

    fun setSinks(remoteSink: VideoSink, localSink: VideoSink) {
        AppDependencies.getWebRtcCallManager().setSinks(remoteSink, localSink)
    }

    fun increaseElapsedRing() {
        _countRingingTime.postValue((_countRingingTime.value ?: 0) + 1000)
    }

    private fun updateWebRtcControl(
        isVideoEnabled: Boolean,
        isMicEnabled: Boolean,
        state: CallState
    ) {
        updateWebRtcControl(
            isVideoEnabled,
            isMicEnabled,
            state,
            if (isVideoEnabled) CallType.VIDEO else CallType.AUDIO
        )
    }

    private fun updateWebRtcControl(
        isVideoEnabled: Boolean,
        isMicEnabled: Boolean,
        state: CallState,
        callType: CallType
    ) {
        _webRtcControl.postValue(WebRtcControl(isVideoEnabled, isMicEnabled, state, callType))
    }

    private fun getCurrentCameraDirectionValue() =
        _cameraDirection.value?.first ?: true

    private fun flipCameraDirection() {
        _cameraDirection.value =
            Pair(
                _cameraDirection.value?.first?.not() ?: true,
                true
            )
    }

    override fun onCleared() {
        super.onCleared()

        cd.dispose()

        if (type == WebRtcCallActivity.TYPE_ANSWER && hasAnswerCall.not()) {
            denyCall()
        }
        AppDependencies.getDatabaseObserver().unregisterWebRtcCallListener()
        AppDependencies.getWebRtcCallManager().unregisterLifecycle()
        AppDependencies.getWebRtcCallManager().unregisterRtcCallOption()
        AppDependencies.getWebRtcCallManager().dispose()
    }

    inner class RtcCallObserver : DatabaseObserver.WebRtcCallListener {
        override fun onOffer(sdp: String) {
            Logging.debug(WebRtcCallActivity.TAG, "Receive offer sdp from server")

            pendingOfferSessionDescription =
                SessionDescription(SessionDescription.Type.OFFER, sdp.formatSdpString())
        }

        override fun onAnswer(sdp: String) {
            Logging.debug(WebRtcCallActivity.TAG, "Receive answer sdp from server")

            AppDependencies.getWebRtcCallManager().setRemoteSdp(
                SessionDescription(
                    SessionDescription.Type.ANSWER,
                    sdp.formatSdpString()
                )
            )
        }

        override fun onIceCandidate(candidate: IceCandidate) {
            Logging.debug(WebRtcCallActivity.TAG, "Add local ice candidate")
            Logging.debug(
                WebRtcCallActivity.TAG,
                "IceCandidate: {${candidate.sdpMid}%${candidate.sdpMLineIndex}%${candidate.sdp}}"
            )

            AppDependencies.getWebRtcCallManager().addIceCandidate(candidate)
        }

        override fun onClose() {
            _state.postValue(WebRtcState.DISCONNECTED)
        }

        override fun onUserDenyCall() {
            _event.postValue(WebRtcEvent.LocalStopCalling(R.string.msg_webrtc_user_deny_call))
        }

        override fun onUserIsInCall() {
            _event.postValue(WebRtcEvent.LocalStopCalling(R.string.msg_webrtc_user_is_in_call))
        }
    }

    inner class RtcCallLifecycleHandler : RtcCallLifecycle {
        override fun onStart() {
            _state.postValue(WebRtcState.START_CALL)
        }

        override fun onConnecting() {
            _state.postValue(WebRtcState.CONNECTING)

            updateWebRtcControl(
                serviceModel.isVideoEnabled,
                true,
                CallState.RECONNECTING,
                if (serviceModel.isVideoEnabled) CallType.VIDEO else CallType.AUDIO
            )
        }

        override fun onConnected() {
            Logging.debug("WebRtcViewModel", "Handle peer connection onConnected")

            _event.postValue(WebRtcEvent.ConnectedCall())

            updateWebRtcControl(
                serviceModel.isVideoEnabled,
                true,
                CallState.ONGOING,
                if (serviceModel.isVideoEnabled) CallType.VIDEO else CallType.AUDIO
            )
        }

        override fun onFailed() {
            _state.postValue(WebRtcState.FAILED)
        }

        override fun onDisconnected() {
            _state.postValue(WebRtcState.DISCONNECTED)
        }
    }

    inner class CallOptionState : RtcCallOption {
        override fun onCameraDirectionChanged(isFrontCamera: Boolean) {
            _cameraDirection.postValue(Pair(isFrontCamera, false))
        }

        override fun onRemoteCameraVideo(isEnabled: Boolean) {
            _remoteCameraEnabled.postValue(isEnabled)
        }
    }
}

sealed class WebRtcEvent(val message: Int) {
    class LocalStopCalling(message: Int) : WebRtcEvent(message) {}
    class StartRinging(val callType: CallType, errorMessage: Int) : WebRtcEvent(errorMessage) {}
    class ConnectedCall() : WebRtcEvent(-1) {}
    class TriggerAnswerFromNotification(val isVideoEnabled: Boolean): WebRtcEvent(-1) {}
}