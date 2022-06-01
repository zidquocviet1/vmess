package com.mqv.vmess.webrtc

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomWebrtcCallViewBinding
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.User
import com.mqv.vmess.util.Picture
import com.mqv.vmess.util.views.ViewUtil.px
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSink

class WebRtcCallView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attr, defStyleAttr) {
    private val mBinding = CustomWebrtcCallViewBinding.bind(
        inflate(context, R.layout.custom_webrtc_call_view, this)
    )
    private var isVideoEnabled = false
    private var isConnected = false
    private var controlCallListener: ControlCallListener? = null
    private var lastVisibleView: Set<View> = setOf()

    init {
        with(mBinding) {
            callScreenAnswerCall.setOnClickListener {
                if (isVideoEnabled) {
                    controlCallListener?.onAcceptCallPressed()
                } else {
                    controlCallListener?.onAcceptCallWithVoiceOnlyPressed()
                }
            }
            callScreenDeclineCall.setOnClickListener { controlCallListener?.onDenyCallPressed() }
            callScreenEndCall.setOnClickListener { controlCallListener?.onEndCallPressed() }
            callScreenCameraDirectionToggle.setOnClickListener { controlCallListener?.onCameraDirectionChanged() }
            callScreenVideoToggle.setOnCheckedChangeListener { _, isOn ->
                controlCallListener?.onVideoChanged(
                    isOn
                )
                mBinding.callScreenFullParticipantAvatar.visibility =
                    if (isOn) View.GONE else View.VISIBLE
                mBinding.callScreenFullShade.visibility =
                    if (isOn) View.GONE else View.VISIBLE
            }
            callScreenAudioMicToggle.setOnCheckedChangeListener { _, isOn ->
                controlCallListener?.onMicChanged(
                    isOn
                )
            }
            callScreenSpeakerToggle.setOnCheckedChangeListener { _, isOn ->
                controlCallListener?.onAudioOutputChanged(
                    !isOn
                )
            }

            pipScreenView.clipToOutline = true
            pipScreenView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline) {
                    outline.setRoundRect(0, 0,  120.px, 160.px, 20f)
                }
            }
        }
    }

    fun setVideoEnable(isVideoEnabled: Boolean) {
        this.isVideoEnabled = isVideoEnabled

        with(mBinding) {
            if (isVideoEnabled) {
                screenView.init(AppDependencies.getWebRtcCallManager().eglContext, null)
                screenView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                screenView.setMirror(true)

                pipScreenView.init(AppDependencies.getWebRtcCallManager().eglContext, null)
                pipScreenView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                pipScreenView.setMirror(true)

                mBinding.callScreenFullParticipantAvatar.visibility =
                    if (isVideoEnabled) View.GONE else View.VISIBLE
                mBinding.callScreenFullShade.visibility =
                    if (isVideoEnabled) View.GONE else View.VISIBLE
            }
        }
    }

    fun setMicEnable(isMicEnabled: Boolean) {
        mBinding.callScreenAudioMicToggle.setChecked(isMicEnabled, false)
    }

    fun setCameraDirection(property: Pair<Boolean, Boolean>) {
        val isFrontCamera = property.first
        val isLocalFlip = property.second

        if (isVideoEnabled) {
            if (isConnected) {
                if (isLocalFlip) {
                    mBinding.pipScreenView.setMirror(isFrontCamera)
                } else {
                    mBinding.screenView.setMirror(isFrontCamera)
                }
            } else {
                mBinding.pipScreenView.setMirror(isFrontCamera && isLocalFlip)
            }
        }
    }

    fun setParticipant(participant: User) {
        mBinding.callHeader.callParticipantName.text = participant.displayName

        Picture.loadUserAvatar(context, participant.photoUrl)
            .into(mBinding.callHeader.callParticipantAvatar)
        Picture.loadUserAvatar(context, participant.photoUrl)
            .centerCrop()
            .into(mBinding.callScreenFullParticipantAvatar)
    }

    fun getRemoteSink(): SurfaceViewRenderer =
        mBinding.screenView

    fun getLocalSink(): VideoSink =
        mBinding.pipScreenView

    fun setCallingStatus(status: Int) {
        mBinding.callHeader.callStatus.setText(status)
    }

    fun disableHangupButton() {
        mBinding.callScreenDeclineCall.isEnabled = false
    }

    fun setWebRtcControls(control: WebRtcControl) {
        val visibleView = mutableSetOf<View>()

        if (control.displayIncomingCall()) {
            visibleView.add(mBinding.callScreenAnswerCall)
            visibleView.add(mBinding.callScreenAnswerCallLabel)

            visibleView.add(mBinding.callScreenDeclineCall)
            visibleView.add(mBinding.callScreenDeclineCallLabel)
        }

        if (control.displayMicToggleButton()) {
            visibleView.add(mBinding.callScreenAudioMicToggle)
            visibleView.add(mBinding.callScreenAudioMicToggleLabel)
        }

        if (control.displayCameraToggleButton()) {
            visibleView.add(mBinding.callScreenVideoToggle)
            visibleView.add(mBinding.callScreenVideoToggleLabel)
        }

        if (control.displayFlipToggleButton(context)) {
            visibleView.add(mBinding.callScreenCameraDirectionToggle)
            visibleView.add(mBinding.callScreenCameraDirectionToggleLabel)
        }

        if (control.displayHangUpButton()) {
            visibleView.add(mBinding.callScreenEndCall)
            visibleView.add(mBinding.callScreenEndCallLabel)
        }

        if (control.displaySpeakerToggleButton()) {
            visibleView.add(mBinding.callScreenSpeakerToggle)
            visibleView.add(mBinding.callScreenSpeakerToggleLabel)
        }

        if (control.displayOngoingCall()) {
            mBinding.callHeader.root.visibility = View.GONE
            visibleView.add(mBinding.cardPipScreenView)
        } else {
            mBinding.callHeader.root.visibility = View.VISIBLE
        }

        if (lastVisibleView != visibleView) {
            lastVisibleView.forEach { it.visibility = GONE }
            visibleView.forEach { it.visibility = VISIBLE }
        }

        lastVisibleView = visibleView
        isConnected = control.displayOngoingCall()
    }

    fun setControlCallListener(listener: ControlCallListener) {
        controlCallListener = listener
    }

    fun dispose() {
        mBinding.screenView.release()
        mBinding.pipScreenView.release()
    }

    interface ControlCallListener {
        fun onControlsFadeOut()
        fun showSystemUI()
        fun hideSystemUI()
        fun onVideoChanged(isVideoEnabled: Boolean)
        fun onMicChanged(isMicEnabled: Boolean)
        fun onAudioOutputChanged(isSpeakerEnabled: Boolean)
        fun onCameraDirectionChanged()
        fun onEndCallPressed()
        fun onDenyCallPressed()
        fun onAcceptCallWithVoiceOnlyPressed()
        fun onAcceptCallPressed()
        fun onLocalPictureInPictureClicked()
        fun onRingGroupChanged(ringGroup: Boolean, ringingAllowed: Boolean)
    }
}