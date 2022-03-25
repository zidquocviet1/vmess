package com.mqv.vmess.activity.br

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.mqv.vmess.data.repository.impl.NotificationRepositoryImpl
import com.mqv.vmess.reactive.RxHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MarkNotificationReadReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var repository: NotificationRepositoryImpl

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val id = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1L)

        if (id != -1L) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(id.toInt())

            repository.fetchById(id)
                .flatMapObservable { repository.markAsRead(it) }
                .compose(RxHelper.applyObservableSchedulers())
                .compose(RxHelper.parseResponseData())
                .onErrorComplete()
                .subscribe()

        }
    }

    companion object {
        const val EXTRA_INTENT = "notification_broadcast"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}