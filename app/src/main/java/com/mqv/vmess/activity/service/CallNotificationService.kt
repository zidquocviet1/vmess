package com.mqv.vmess.activity.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.os.IBinder
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.room.rxjava3.EmptyResultSetException
import com.mqv.vmess.R
import com.mqv.vmess.activity.WebRtcCallActivity
import com.mqv.vmess.activity.br.DeclineReceiver
import com.mqv.vmess.activity.br.HangUpReceiver
import com.mqv.vmess.data.repository.impl.PeopleRepositoryImpl
import com.mqv.vmess.data.repository.impl.RtcRepositoryImpl
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.Picture
import com.mqv.vmess.util.ServiceUtil
import com.mqv.vmess.work.LifecycleUtil
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

private const val CHANNEL_ID = "123"
private const val CHANNEL_NAME = "Calling Message"

@AndroidEntryPoint
class CallNotificationService : Service() {
    @Inject
    lateinit var peopleRepository: PeopleRepositoryImpl

    @Inject
    lateinit var rtcRepository: RtcRepositoryImpl

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (LifecycleUtil.isAppForeground() && !AppDependencies.getWebRtcCallManager().shouldShowNotification) {
            Logging.debug(
                TAG,
                "The user is idle right now. Call will be ejected, send notification to eject that call to caller"
            )

            intent?.extras?.getString(EXTRA_CALLER)?.let { participantId ->
                rtcRepository.notifyBusy(participantId)
                    .compose(RxHelper.applyObservableSchedulers())
                    .onErrorComplete()
                    .subscribe()
            }
        } else {
            Logging.debug(TAG, "Show call notification message")

            val participantId = intent?.extras?.getString(EXTRA_CALLER)
            val isVideoEnable = intent?.extras?.getBoolean(EXTRA_VIDEO) ?: false

            if (participantId == null) {
                Logging.debug(
                    TAG,
                    "Can't detect who is the caller. Caller id not found. Stop call service"
                )

                stopSelf()
            } else {
                getCallerInformationAndShowNotification(participantId, isVideoEnable)
            }
        }

        return START_STICKY
    }

    private fun getCallerInformationAndShowNotification(recipient: String, isVideoEnable: Boolean) {
        peopleRepository.getCachedByUid(recipient)
            .onErrorResumeNext { t ->
                return@onErrorResumeNext if (t is EmptyResultSetException) {
                    peopleRepository.getConnectPeopleByUid(recipient)
                        .compose(RxHelper.parseResponseData())
                        .singleOrError()
                } else {
                    Single.error(t)
                }
            }
            .onErrorComplete {
                // Send notification to reject the phone call

                applicationContext.sendBroadcast(Intent(this, DeclineReceiver::class.java))

                true
            }
            .subscribeOn(Schedulers.io())
            .subscribe { people -> showNotification(people, recipient, isVideoEnable) }
    }

    private fun showNotification(people: People, participantId: String, isVideoEnable: Boolean) {
        val remoteView = RemoteViews(packageName, R.layout.custom_webrtc_call_notification).apply {
            setTextViewText(R.id.text_caller, people.displayName)
            setTextViewText(R.id.text_content, getTitleForNotification(isVideoEnable))
            setOnClickPendingIntent(R.id.button_decline, getDeclinePendingIntent(participantId))
            setOnClickPendingIntent(
                R.id.button_answer,
                getAnswerPendingIntent(isVideoEnable, participantId)
            )
            setOnClickPendingIntent(R.id.button_hangup, getHangUpPendingIntent())
            setImageViewBitmap(
                R.id.image_caller,
                Picture.loadUserAvatarIntoBitmap(applicationContext, people.photoUrl)
            )
        }
        val notificationChannel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                val audioAttr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(Settings.System.DEFAULT_RINGTONE_URI, audioAttr)
                enableLights(true)
                lightColor = Color.RED
            }

        ServiceUtil.getNotificationManager(this).createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setCustomContentView(remoteView)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setSound(Settings.System.DEFAULT_RINGTONE_URI)
            .setOngoing(true)
            .setFullScreenIntent(getFullScreenCallPendingIntent(isVideoEnable, participantId), true)

        startForeground(participantId.hashCode(), notificationBuilder.build())
    }

    private fun getFullScreenCallPendingIntent(
        isVideoEnable: Boolean,
        participantId: String
    ): PendingIntent {
        val fullScreenIntent = Intent(applicationContext, WebRtcCallActivity::class.java).apply {
            putExtra(WebRtcCallActivity.EXTRA_TYPE, WebRtcCallActivity.TYPE_ANSWER)
            putExtra(WebRtcCallActivity.EXTRA_VIDEO_ENABLE, isVideoEnable)
            putExtra(WebRtcCallActivity.EXTRA_PARTICIPANT_ID, participantId)
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getDeclinePendingIntent(recipient: String): PendingIntent {
        val intent = Intent(applicationContext, DeclineReceiver::class.java).apply {
            putExtra(DeclineReceiver.EXTRA_RECIPIENT_ID, recipient)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getAnswerPendingIntent(
        isVideoEnable: Boolean,
        participantId: String
    ): PendingIntent {
        return PendingIntent.getActivity(
            applicationContext,
            0,
            WebRtcCallActivity.createAnswerCallFromNotification(applicationContext, participantId, isVideoEnable),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getHangUpPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, HangUpReceiver::class.java)
        return PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getTitleForNotification(isVideoEnable: Boolean): String {
        return if (isVideoEnable) {
            applicationContext.getString(R.string.label_incoming_video_call)
        } else {
            applicationContext.getString(R.string.label_incoming_voice_call)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Logging.show("Call Notification Service is destroyed")

        stopForeground(true)
    }

    companion object {
        const val EXTRA_CALLER = "caller"
        const val EXTRA_VIDEO = "video"

        val TAG: String = CallNotificationService::class.java.simpleName

        fun cancelCallNotification(context: Context) {
            context.stopService(Intent(context, CallNotificationService::class.java))
        }

        fun updateOngoingNotification(context: Context) {

        }
    }
}