package com.mqv.vmess.notification

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.MainApplication
import com.mqv.vmess.activity.ConversationActivity
import com.mqv.vmess.activity.MainActivity
import com.mqv.vmess.data.dao.ConversationOptionDao
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.ui.fragment.ConversationListInboxFragment
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.work.LifecycleUtil
import io.reactivex.rxjava3.core.Single
import java.time.LocalDateTime

class NotificationValidatorImpl(
    private val mConversationOptionDao: ConversationOptionDao,
    private val mUser: FirebaseUser,
    private val mContext: Context
) : NotificationValidator {

    override fun shouldShowNotification(
        conversation: Conversation,
        message: Chat
    ): Single<Boolean> {
        if (!AppDependencies.getAppPreferences().notificationStatus) {
            return Single.just(false)
        } else {
            return mConversationOptionDao.isTurnOffNotification(
                conversation.id,
                LocalDateTime.now().toLong()
            ).map { isTurnOff: Boolean ->
                if (!isTurnOff) {
                    if (message.senderId != mUser.uid) {
                        if (!LifecycleUtil.isAppForeground()) {
                            return@map true
                        } else {
                            val app = mContext.applicationContext as MainApplication
                            val currentActivity = app.activeActivity
                            val isInCurrentConversationOrMainActivity =
                                if (currentActivity is ConversationActivity) {
                                    currentActivity.extraConversationId == conversation.id
                                } else {
                                    (currentActivity is MainActivity) && (currentActivity.visibleFragment is ConversationListInboxFragment)
                                }
                            return@map !isInCurrentConversationOrMainActivity
                        }
                    }
                    return@map false
                }
                return@map false
            }
        }
    }
}