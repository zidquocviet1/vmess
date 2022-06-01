package com.mqv.vmess.activity.br

import android.content.Context
import android.content.Intent
import com.mqv.vmess.activity.WebRtcCallActivity
import com.mqv.vmess.activity.service.CallNotificationService
import com.mqv.vmess.data.repository.impl.RtcRepositoryImpl
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Logging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeclineReceiver : HiltBroadcastReceiver() {
    @Inject
    lateinit var rtcRepository: RtcRepositoryImpl

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Logging.show("Start decline webrtc calling")

        CallNotificationService.cancelCallNotification(context)

        intent.getStringExtra(EXTRA_RECIPIENT_ID)?.let { recipientId ->
            rtcRepository.denyCall(recipientId)
                .compose(RxHelper.applyObservableSchedulers())
                .doOnError { Logging.debug(WebRtcCallActivity.TAG, "Can't make a stop call") }
                .onErrorComplete()
                .subscribe {}
        }
    }

    companion object {
        const val EXTRA_RECIPIENT_ID = "recipient_id"
    }
}