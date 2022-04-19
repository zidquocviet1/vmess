package com.mqv.vmess.activity

import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil.DecoderQueryException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerControlView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.databinding.ActivityVideoPlayerBinding
import com.mqv.vmess.network.OkHttpProvider.UnsafeOkHttpClient
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.util.Const
import com.mqv.vmess.util.Logging
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import okhttp3.Call
import okhttp3.OkHttpClient
import java.util.stream.Collectors
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class VideoPlayerActivity : BaseActivity<AndroidViewModel, ActivityVideoPlayerBinding>(),
    PlayerControlView.VisibilityListener {
    @UnsafeOkHttpClient
    @Inject
    lateinit var client: OkHttpClient

    private var mCurrentUser: FirebaseUser? = null
    private var player: ExoPlayer? = null
    private var mediaItems: List<MediaItem>? = null
    private var startPosition: Long = 0
    private var startItemIndex = 0
    private var startAutoPlay = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentUser = FirebaseAuth.getInstance().currentUser

        if (mCurrentUser == null) {
            Toast.makeText(this, "Your login session has been ended", Toast.LENGTH_SHORT).show()
            finish()
        }

        mBinding.playerView.setControllerVisibilityListener(this)
        mBinding.playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        mBinding.playerView.requestFocus()
    }

    override fun onStart() {
        super.onStart()

        mCurrentUser!!.authorizeToken()
            .observeOn(AndroidSchedulers.mainThread())
            .map { token -> initializePlayer(token) }
            .subscribe { isSuccess, _ ->
                if (isSuccess) {
                    mBinding.playerView.onResume()
                } else {
                    Toast.makeText(
                        this,
                        "Can't start the video. Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onStop() {
        super.onStop()
        mBinding.playerView.onPause()
        releasePlayer()
    }

    override fun binding() {
        mBinding = ActivityVideoPlayerBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {
    }

    override fun onVisibilityChange(visibility: Int) {

    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // See whether the player view wants to handle media or DPAD keys events.
        return mBinding.playerView.dispatchKeyEvent((event)!!) || super.dispatchKeyEvent(event)
    }

    private fun initializePlayer(token: String): Boolean {
        if (player == null) {
            mediaItems = createMediaItems(intent)
            if (mediaItems!!.isEmpty()) {
                return false
            }
            //            trackSelector = new DefaultTrackSelector(/* context= */ this);
            //            lastSeenTracksInfo = TracksInfo.EMPTY;
            val okHttpDataSource = OkHttpDataSource.Factory(client as Call.Factory)
                .setDefaultRequestProperties(hashMapOf(Const.AUTHORIZATION to token))

            player = ExoPlayer.Builder(this)
                //                            .setRenderersFactory(renderersFactory)
                //                            .setTrackSelector(trackSelector)
                .setMediaSourceFactory(DefaultMediaSourceFactory(okHttpDataSource))
                .build()
            //            player.setTrackSelectionParameters(trackSelectionParameters);
            player?.addListener(PlayerEventListener())
            //            player.addAnalyticsListener(new EventLogger(trackSelector));
            player?.setAudioAttributes(AudioAttributes.DEFAULT,true)
            player?.playWhenReady = startAutoPlay
            mBinding.playerView.player = player
            //            serverSideAdsLoader.setPlayer(player);
            //            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
            //            debugViewHelper.start();
        }
        val haveStartPosition: Boolean = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player?.seekTo(startItemIndex, startPosition)
        }
        player?.setMediaItems((mediaItems)!!, !haveStartPosition)
        player?.prepare()
        return true
    }

    private fun releasePlayer() {
        if (player != null) {
//            updateTrackSelectorParameters();
            updateStartPosition()
            //            serverSideAdsLoaderState = serverSideAdsLoader.release();
//            serverSideAdsLoader = null;
//            debugViewHelper.stop();
//            debugViewHelper = null;
            player?.release()
            player = null
            mBinding.playerView.player = null
            mediaItems = emptyList()
        }
//        if (clientSideAdsLoader != null) {
//            clientSideAdsLoader.setPlayer(null);
//        } else {
//        }
//        playerView.getAdViewGroup().removeAllViews();
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val videoUrl = intent.getStringArrayListExtra(EXTRA_URI)!!

        startItemIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

        return videoUrl.stream().map { url ->
            val mmd = MediaMetadata.Builder()
                .setTitle("Example")
                .build()
            MediaItem.Builder()
                .setMediaMetadata(mmd)
                .setUri(url.replace("localhost", Const.BASE_IP))
                .build()
        }.collect(Collectors.toList())
    }

    private fun updateStartPosition() {
        player?.let {
            startAutoPlay = it.playWhenReady
            startItemIndex = it.currentMediaItemIndex
            startPosition = 0.coerceAtLeast(it.contentPosition.toInt()).toLong()
        }
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    mBinding.loading.visibility = View.GONE
                    mBinding.playerView.isEnabled = true
                    mBinding.playerView.hideController()
                    Logging.debug(TAG, "The player is idle")
                }
                Player.STATE_BUFFERING -> {
                    mBinding.loading.visibility = View.VISIBLE
                    mBinding.playerView.isEnabled = false
                    mBinding.playerView.hideController()
                    Logging.debug(TAG, "The player is buffering")
                }
                Player.STATE_READY -> {
                    mBinding.loading.visibility = View.GONE
                    mBinding.playerView.isEnabled = true
                    mBinding.playerView.showController()
                    Logging.debug(TAG, "The player is ready to start")
                }
                Player.STATE_ENDED -> Toast.makeText(
                    this@VideoPlayerActivity,
                    "The video has been ended",
                    Toast.LENGTH_SHORT
                ).show()
                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player?.seekToDefaultPosition()
                player?.prepare()
            } else {
//                updateButtonVisibility();
//                showControls();
            }
        }

        override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
//            updateButtonVisibility();
//            if (tracksInfo == lastSeenTracksInfo) {
//                return;
//            }
//            if (!tracksInfo.isTypeSupportedOrEmpty(
//                    C.TRACK_TYPE_VIDEO, /* allowExceedsCapabilities= */ true)) {
//                showToast(R.string.error_unsupported_video);
//            }
//            if (!tracksInfo.isTypeSupportedOrEmpty(
//                    C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities= */ true)) {
//                showToast(R.string.error_unsupported_audio);
//            }
//            lastSeenTracksInfo = tracksInfo;
        }
    }

    private class PlayerErrorMessageProvider :
        ErrorMessageProvider<PlaybackException> {
        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
//            String errorString = getString(R.string.error_generic);
            val cause = e.cause
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                val decoderInitializationException = cause
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.cause is DecoderQueryException) {
//                        errorString = getString(R.string.error_querying_decoders);
                    } else if (decoderInitializationException.secureDecoderRequired) {
//                        errorString =
//                                getString(
//                                        R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                    } else {
//                        errorString =
//                                getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                    }
                } else {
//                    errorString =
//                            getString(
//                                    R.string.error_instantiating_decoder,
//                                    decoderInitializationException.codecInfo.name);
                }
            }
            return Pair.create(0, "something when wrong")
        }
    }

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_START_INDEX = "start_index"

        val TAG: String = VideoPlayerActivity::class.java.simpleName
    }
}