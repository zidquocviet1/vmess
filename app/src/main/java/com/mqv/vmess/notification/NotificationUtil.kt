package com.mqv.vmess.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.text.HtmlCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.R
import com.mqv.vmess.activity.ConversationActivity
import com.mqv.vmess.activity.br.DirectReplyReceiver
import com.mqv.vmess.activity.br.MarkNotificationReadReceiver
import com.mqv.vmess.activity.preferences.PreferenceFriendRequestActivity
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.data.ConversationMapper
import com.mqv.vmess.ui.data.ConversationMetadata
import com.mqv.vmess.util.Picture
import java.time.ZoneId

private const val CHANNEL_NAME_CONVERSATION = "Conversation"
private const val CHANNEL_NAME_FRIEND_REQUEST = "Friend Request"
private const val CHANNEL_NAME_ACCEPTED_REQUEST = "Accepted Friend"
private const val CHANNEL_NAME_INCOMING_MESSAGE = "Incoming Message"

private const val CHANNEL_ID_CONVERSATION = "1"
private const val CHANNEL_ID_FRIEND_REQUEST = "2"
private const val CHANNEL_ID_ACCEPTED = "3"
private const val CHANNEL_ID_INCOMING_MESSAGE = "4"
private const val REQUEST_CODE = 1212

object NotificationUtil {
    @JvmStatic
    fun sendAddedConversationNotification(context: Context, conversation: Conversation) {
        val metadata = getConversationMetadata(context, conversation)
        val conversationHashId = conversation.hashCode()
        val intent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, conversation.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        metadata?.let {
            sendNotification(
                context = context,
                id = conversationHashId,
                channelId = CHANNEL_ID_CONVERSATION,
                channelName = CHANNEL_NAME_CONVERSATION,
                title = it.conversationName,
                body = context.getString(R.string.msg_added_to_group, it.conversationCreatedBy),
                pendingIntent = pendingIntent,
            )
        }
    }

    @JvmStatic
    fun sendFriendRequestNotification(context: Context, sender: User, notificationId: Long) {
        val markAsReadIntent = Intent(context, MarkNotificationReadReceiver::class.java).apply {
            putExtra(MarkNotificationReadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val intent = Intent(context, PreferenceFriendRequestActivity::class.java).apply {
            putExtra(MarkNotificationReadReceiver.EXTRA_INTENT, markAsReadIntent)
        }
        val pendingIntent =
            PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
        val body = Html.fromHtml(
            context.getString(
                R.string.msg_new_friend_request_notification_fragment,
                sender.displayName
            ), HtmlCompat.FROM_HTML_MODE_COMPACT
        ).toString()

        sendNotification(
            context = context,
            id = notificationId.toInt(),
            channelId = CHANNEL_ID_FRIEND_REQUEST,
            channelName = CHANNEL_NAME_FRIEND_REQUEST,
            title = context.getString(R.string.app_name),
            body = body,
            pendingIntent = pendingIntent,
            largeIcon = Picture.loadUserAvatarIntoBitmap(context, sender.photoUrl),
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    @JvmStatic
    fun sendAcceptedFriendRequestNotification(context: Context, whoAccepted: User, id: Long) {
        val markAsReadIntent = Intent(context, MarkNotificationReadReceiver::class.java).apply {
            putExtra(MarkNotificationReadReceiver.EXTRA_NOTIFICATION_ID, id)
        }
        val intent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(ConversationActivity.EXTRA_PARTICIPANT_ID, whoAccepted.uid)
            putExtra(MarkNotificationReadReceiver.EXTRA_INTENT, markAsReadIntent)
        }
        val pendingIntent =
            PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
        val body = Html.fromHtml(
            context.getString(
                R.string.msg_accepted_friend_request_notification_fragment,
                whoAccepted.displayName
            ), HtmlCompat.FROM_HTML_MODE_COMPACT
        ).toString()

        sendNotification(
            context = context,
            id = id.toInt(),
            channelId = CHANNEL_ID_ACCEPTED,
            channelName = CHANNEL_NAME_ACCEPTED_REQUEST,
            title = context.getString(R.string.app_name),
            body = body,
            pendingIntent = pendingIntent,
            largeIcon = Picture.loadUserAvatarIntoBitmap(context, whoAccepted.photoUrl),
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    @JvmStatic
    fun sendIncomingMessageNotification(
        context: Context,
        metadata: MessageNotificationMetadata
    ) {
        val conversation = metadata.conversation
        val message = metadata.message
        val sender = metadata.sender

        getConversationMetadata(context, conversation)?.let {
            val intent = Intent(context, ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra("oneshot", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_ONE_SHOT
            )
            val conversationThumbnailBitmap =
                Picture.loadUserAvatarIntoBitmap(context, it.conversationThumbnail[0])
            val person = Person.Builder()
                .setName(context.getString(R.string.label_you))
                .build()
            val style = NotificationCompat.MessagingStyle(person).let { style ->
                style.conversationTitle =
                    if (it.type == ConversationType.GROUP) it.conversationName else ""
                style.isGroupConversation = conversation.group != null
                style.addMessage(
                    message.content,
                    message.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    Person.Builder().setName(sender.displayName).build()
                )
            }
            val remoteInput = RemoteInput.Builder("reply").run {
                setLabel(context.getString(R.string.action_reply))
                build()
            }
            val replyIntent = Intent(context, DirectReplyReceiver::class.java)
            val replyPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    conversation.hashCode(),
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            val action = NotificationCompat.Action.Builder(
                null,
                context.getString(R.string.action_reply),
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()
            sendNotification(
                context = context,
                id = conversation.hashCode(),
                title = it.conversationName,
                body = message.content,
                channelId = CHANNEL_ID_INCOMING_MESSAGE,
                channelName = CHANNEL_NAME_INCOMING_MESSAGE,
                importance = NotificationManager.IMPORTANCE_HIGH,
                pendingIntent = pendingIntent,
                largeIcon = conversationThumbnailBitmap,
                style = style,
                actions = arrayOf(action)
            )
        }
    }

    private fun sendNotification(
        context: Context,
        id: Int,
        channelId: String,
        channelName: String,
        title: String,
        body: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        pendingIntent: PendingIntent? = null,
        largeIcon: Bitmap? = null,
        style: NotificationCompat.Style? = null,
        vararg actions: NotificationCompat.Action?
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        val notification = NotificationCompat.Builder(context, channelId).apply {
            setContentTitle(title)
            setContentText(body)
            setAutoCancel(true)
            setLargeIcon(largeIcon)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(pendingIntent)
            setStyle(style)

            for (a in actions)
                addAction(a)
        }.build()

        notificationManager.createNotificationChannel(notificationChannel)
        notificationManager.notify(id, notification)
    }

    private fun getConversationMetadata(
        context: Context,
        conversation: Conversation
    ): ConversationMetadata? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser?.let {
            ConversationMapper.mapToMetadata(
                conversation,
                it.toUser(),
                context
            )
        }
    }
}

fun FirebaseUser.toUser(): User {
    return User.Builder()
        .setUid(uid)
        .setDisplayName(displayName)
        .setPhotoUrl(if (photoUrl == null) null else photoUrl.toString())
        .create()
}