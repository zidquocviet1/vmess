package com.mqv.vmess.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.WebRtcEvent
import com.mqv.vmess.activity.viewmodel.WebRtcViewModel
import com.mqv.vmess.databinding.ActivityWebRtcCallBinding
import com.mqv.vmess.util.Logging
import com.mqv.vmess.webrtc.CallType
import com.mqv.vmess.webrtc.WebRtcCallView
import com.mqv.vmess.webrtc.WebRtcState
import com.mqv.vmess.webrtc.audio.OutgoingRinger
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class WebRtcCallActivity : BaseActivity<WebRtcViewModel, ActivityWebRtcCallBinding>(),
    WebRtcCallView.ControlCallListener {

    private val mTimer: Timer = Timer()
    private val mOutgoingRinger: OutgoingRinger = OutgoingRinger(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        setTheme(R.style.WebRTCCallTheme)

        super.onCreate(savedInstanceState)

        mBinding.screenView.setVideoEnable(
            intent.getBooleanExtra(EXTRA_VIDEO_ENABLE, false) || intent.getIntExtra(
                EXTRA_TYPE, -1
            ) == TYPE_VIDEO_CALL
        )
        mBinding.screenView.setControlCallListener(this)
        mViewModel.setSinks(mBinding.screenView.getRemoteSink(), mBinding.screenView.getLocalSink())
    }

    override fun binding() {
        mBinding = ActivityWebRtcCallBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<WebRtcViewModel> = WebRtcViewModel::class.java

    override fun setupObserver() {
        mViewModel.micEnabled.observe(this, mBinding.screenView::setMicEnable)
        mViewModel.webRtcControl.observe(this, mBinding.screenView::setWebRtcControls)
        mViewModel.user.observe(this, mBinding.screenView::setParticipant)
        mViewModel.cameraDirection.observe(this, mBinding.screenView::setCameraDirection)
        mViewModel.state.observe(this, ::handleWebRtcState)
        mViewModel.event.observe(this, ::handleWebRtcEvent)
        mViewModel.countRinging.observe(this, ::handleOutgoingRing)
    }

    private fun handleWebRtcState(state: WebRtcState) {
        when (state) {
            WebRtcState.START_CALL -> {
                mBinding.screenView.setCallingStatus(R.string.action_starting_call)
            }
            WebRtcState.CONNECTING -> {
                mBinding.screenView.setCallingStatus(R.string.action_connecting_call)
            }
            WebRtcState.FAILED -> {
                Toast.makeText(
                    this,
                    "Can't connect to this user. Please try again",
                    Toast.LENGTH_SHORT
                ).show()
                mViewModel.stopCall()

                displayEndingCallAndFinish()
            }
            WebRtcState.DISCONNECTED -> {
                startDisconnectedRinging()
                displayEndingCallAndFinish()
            }
            else -> {}
        }
    }

    private fun handleWebRtcEvent(event: WebRtcEvent) {
        when (event) {
            is WebRtcEvent.LocalStopCalling -> {
                startBusyRinging()

                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()

                mBinding.screenView.setCallingStatus(R.string.action_disconnecting_call)
                mBinding.screenView.disableHangupButton()

                delayedFinish()
            }
            is WebRtcEvent.StartRinging -> {
                if (event.callType == CallType.AUDIO) {
                    startSpeakerRinging()
                } else {
                    startEarSpeakerRinging()
                }

                val timerTask = object : TimerTask() {
                    override fun run() {
                        if (!isDestroyed) {
                            mViewModel.increaseElapsedRing()
                        }
                    }
                }
                mTimer.scheduleAtFixedRate(timerTask, 0, 1000)
            }
            is WebRtcEvent.ConnectedCall -> {
                mTimer.cancel()
                mOutgoingRinger.stop()
            }
        }
    }

    private fun handleOutgoingRing(elapsed: Int) {
        if (elapsed >= RINGING_INTERVAL) {
            displayEndingCallAndFinish()

            Toast.makeText(this, R.string.msg_webrtc_user_did_not_answer, Toast.LENGTH_SHORT).show()
        } else {
            Logging.debug(TAG, "Ringing: elapsed time = $elapsed")
        }
    }

    private fun startEarSpeakerRinging() {
        mOutgoingRinger.start(OutgoingRinger.Type.RINGING)
    }

    private fun startSpeakerRinging() {
        mOutgoingRinger.start(OutgoingRinger.Type.RINGING)
    }

    private fun startBusyRinging() {
        mOutgoingRinger.start(OutgoingRinger.Type.BUSY)
    }

    private fun startDisconnectedRinging() {
        mOutgoingRinger.start(OutgoingRinger.Type.DISCONNECTED)
    }

    private fun startCompletedRinging() {
        mOutgoingRinger.start(OutgoingRinger.Type.COMPLETED)
    }

    private fun handleHangUpCall() {
        mViewModel.stopCall()

        startCompletedRinging()
        displayEndingCallAndFinish()
    }

    private fun handleDenyCall() {
        mViewModel.denyCall()

        displayEndingCallAndFinish()
    }

    private fun displayEndingCallAndFinish() {
        mBinding.screenView.disableHangupButton()
        mBinding.screenView.setCallingStatus(R.string.action_ending_call)

        delayedFinish()
    }

    private fun delayedFinish() {
        delayedFinish(DELAYED_FINISH_DURATION)
    }

    private fun delayedFinish(duration: Long) {
        mBinding.screenView.postDelayed(::finish, duration)
    }

    override fun onDestroy() {
        mTimer.cancel()
        mOutgoingRinger.stop()
        mBinding.screenView.dispose()

        super.onDestroy()
    }

    override fun onControlsFadeOut() {
    }

    override fun showSystemUI() {
    }

    override fun hideSystemUI() {
    }

    override fun onVideoChanged(isVideoEnabled: Boolean) {
        mViewModel.handleEnableVideoAfterConnected(isVideoEnabled)
    }

    override fun onMicChanged(isMicEnabled: Boolean) {
        mViewModel.handleMicStatus(isMicEnabled)
    }

    override fun onAudioOutputChanged(isSpeakerEnabled: Boolean) {
        mViewModel.handleSpeakerStatus(isSpeakerEnabled)
    }

    override fun onCameraDirectionChanged() {
        mViewModel.handleFlipCameraDirection()
    }

    override fun onEndCallPressed() {
        handleHangUpCall()
    }

    override fun onDenyCallPressed() {
        handleDenyCall()
    }

    override fun onAcceptCallWithVoiceOnlyPressed() {
        mViewModel.answerCall(false)
    }

    override fun onAcceptCallPressed() {
        mViewModel.setSinks(mBinding.screenView.getRemoteSink(), mBinding.screenView.getLocalSink())
        mViewModel.answerCall(true)
    }

    override fun onLocalPictureInPictureClicked() {
    }

    override fun onRingGroupChanged(ringGroup: Boolean, ringingAllowed: Boolean) {
    }

    companion object {
        const val EXTRA_PARTICIPANT_ID = "participant_id"
        const val EXTRA_TYPE = "type"
        const val EXTRA_VIDEO_ENABLE = "video_enable"
        const val TYPE_AUDIO_CALL = 0
        const val TYPE_VIDEO_CALL = 1
        const val TYPE_ANSWER = 2
        const val TYPE_ANSWER_FROM_NOTIFICATION = 3

        private const val DELAYED_FINISH_DURATION = 1000L
        private const val RINGING_INTERVAL = 60 * 1000

        val TAG: String = WebRtcCallActivity::class.java.simpleName

        @JvmStatic
        fun createAudioCallIntent(context: Context, participantId: String): Intent =
            Intent(context, WebRtcCallActivity::class.java).apply {
                putExtra(EXTRA_PARTICIPANT_ID, participantId)
                putExtra(EXTRA_TYPE, TYPE_AUDIO_CALL)
            }

        @JvmStatic
        fun createVideoCallIntent(context: Context, participantId: String): Intent =
            Intent(context, WebRtcCallActivity::class.java).apply {
                putExtra(EXTRA_PARTICIPANT_ID, participantId)
                putExtra(EXTRA_TYPE, TYPE_VIDEO_CALL)
            }

        @JvmStatic
        fun createAnswerCallFromNotification(context: Context, participantId: String, isVideoEnabled: Boolean): Intent =
            Intent(context, WebRtcCallActivity::class.java).apply {
                putExtra(EXTRA_PARTICIPANT_ID, participantId)
                putExtra(EXTRA_VIDEO_ENABLE, isVideoEnabled)
                putExtra(EXTRA_TYPE, TYPE_ANSWER_FROM_NOTIFICATION)
            }
    }
}