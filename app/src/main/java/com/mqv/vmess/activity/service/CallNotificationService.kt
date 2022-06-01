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
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.R
import com.mqv.vmess.activity.WebRtcCallActivity
import com.mqv.vmess.activity.br.DeclineReceiver
import com.mqv.vmess.activity.br.HangUpReceiver
import com.mqv.vmess.data.repository.impl.PeopleRepositoryImpl
import com.mqv.vmess.data.repository.impl.RtcRepositoryImpl
import com.mqv.vmess.network.model.User
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.Picture
import com.mqv.vmess.util.ServiceUtil
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
            callId = (participantId + FirebaseAuth.getInstance().currentUser?.uid).hashCode()

            getCallerInformationAndShowNotification(participantId, isVideoEnable)
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

        ServiceUtil.getNotificationManager(this)
            .createNotificationChannel(getCallNotificationChannel())

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setCustomContentView(remoteView)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setSound(Settings.System.DEFAULT_RINGTONE_URI)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
        val intent = WebRtcCallActivity.createAnswerCallFromNotification(
            applicationContext,
            participantId,
            isVideoEnable
        )
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
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

        var callId = -1

        private fun getCallNotificationChannel(): NotificationChannel =
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

        fun cancelCallNotification(context: Context) {
            context.stopService(Intent(context, CallNotificationService::class.java))
        }

        fun updateAnsweredNotification(context: Context, participantId: String) {
            cancelCallNotification(context)
        }

        fun updateConnectedNotification(context: Context, user: User) {
            cancelCallNotification(context)
        }
    }
}