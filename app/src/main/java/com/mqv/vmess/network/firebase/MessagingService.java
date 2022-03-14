package com.mqv.vmess.network.firebase;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.provider.Settings;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.ConnectPeopleActivity;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.AppPreferencesImpl;
import com.mqv.vmess.activity.preferences.PreferenceFriendRequestActivity;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.di.GlideApp;
import com.mqv.vmess.notification.NotificationPayload;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.work.BaseWorker;
import com.mqv.vmess.work.ConversationNotificationWorkWrapper;
import com.mqv.vmess.work.FetchNotificationWorker;
import com.mqv.vmess.work.NewConversationWorkWrapper;
import com.mqv.vmess.work.WorkDependency;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MessagingService extends FirebaseMessagingService {
    // Friend notification key data
    public static final String EXTRA_KEY = "notification_key";
    public static final String EXTRA_ACTION_NEW_FRIEND = "new_friend_request";
    public static final String EXTRA_ACTION_ACCEPTED = "accepted_friend";

    private static final String TAG = MessagingService.class.getSimpleName();
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Logging.show("New Token: " + s);

        // This job is not complete
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Logging.show("Got a new FCM token, but the user isn't registered");
            return;
        }

        AppPreferences mPreferences = new AppPreferencesImpl(this, PreferenceManager.getDefaultSharedPreferences(this));
        mPreferences.setFcmToken(s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Logging.debug(TAG, String.format(Locale.getDefault(),
                "onMessageReceived() ID: %s, Delay: %d, Priority: %d, Original Priority: %d",
                remoteMessage.getMessageId(),
                (System.currentTimeMillis() - remoteMessage.getSentTime()),
                remoteMessage.getPriority(),
                remoteMessage.getOriginalPriority()));

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data    = remoteMessage.getData();
            NotificationPayload payload = NotificationPayload.handleRawPayload(data);

            Logging.debug(TAG, "Receive notification payload: " + payload);

            AppDependencies.getNotificationEntry().handleNotificationPayload(payload);

//            var conversationId = data.get(KEY_CONVERSATION_ID);
//
//            if (conversationId == null) {
//                startWorkFetchNotification();
//
//                fetchFriendNotification(data);
//            } else {
//                fetchConversationNotification(data);
//            }
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(@NonNull String s) {
        super.onMessageSent(s);
        Logging.show("onMessageSent()" + s);
    }

    @Override
    public void onSendError(@NonNull String s, @NonNull Exception e) {
        super.onSendError(s, e);
        Logging.show("onSendError()" + s);
    }

    private void fetchFriendNotification(Map<String, String> data) {
        var title = data.get(Const.KEY_TITLE);
        var body = data.get(Const.KEY_BODY);
        var channelId = data.get(Const.KEY_CHANNEL_ID);
        var channelName = data.get(Const.KEY_CHANNEL_NAME);
        var actionClick = data.get(Const.KEY_ACTION_CLICK);
        var imageUrl = data.get(Const.KEY_IMAGE_URL);
        var uid = data.get(Const.KEY_UID);
        var agentId = data.get(Const.KEY_AGENT_ID);

        // Start worker to retrieve new conversation if the app is foreground
        if (actionClick != null && actionClick.equals(Const.DEFAULT_ACCEPTED_FRIEND_REQUEST)) {
            Data workData = new Data.Builder()
                    .putString("otherId", agentId)
                    .putBoolean("from_notification", true)
                    .build();

            BaseWorker worker = new NewConversationWorkWrapper(this, workData);

            WorkDependency.enqueue(worker);
        }

        var intent = getIntentByAction(actionClick, uid, agentId, imageUrl);

        if (intent == null) {
            Logging.show("Intent must not be null");
            return;
        }

        PendingIntent pendingIntent;

        if (isAppForeground()) {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            var stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addNextIntentWithParentStack(intent);

            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        loadImageAndPushNotification(channelId, channelName, title, body, pendingIntent, imageUrl, actionClick);
    }

    private void fetchConversationNotification(Map<String, String> data) {
        var workerData = new Data.Builder()
                                        .putAll(new HashMap<>(data))
                                        .build();

        WorkDependency.enqueue(new ConversationNotificationWorkWrapper(this, workerData));
    }

    private void loadImageAndPushNotification(String channelId, String channelName,
                                              String title, String body, PendingIntent pendingIntent,
                                              @Nullable String imageUrl, String action) {
        var photoUrl = (imageUrl == null || imageUrl.equals("")) ? null : imageUrl.replace("localhost", Const.BASE_IP);

        Bitmap bitmap;

        try {
            bitmap = GlideApp.with(this)
                             .asBitmap()
                             .load(photoUrl)
                             .error(R.drawable.ic_account_undefined)
                             .fallback(R.drawable.ic_round_account)
                             .diskCacheStrategy(DiskCacheStrategy.ALL)
                             .submit()
                             .get();
        } catch (ExecutionException | InterruptedException e) {
            bitmap = null;
        }

        showNotification(channelId, channelName, title, body, pendingIntent, bitmap, action);
    }

    private void showNotification(String channelId, String channelName,
                                  String title, String body, PendingIntent pendingIntent,
                                  @Nullable Bitmap bitmap, String action) {

        var formatBody = Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT);

        var notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(formatBody)
                .setLargeIcon(bitmap)
                .setAutoCancel(true) // Remove notification when user clicked
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(pendingIntent)
                .setGroup(action.equals(Const.DEFAULT_ACCEPTED_FRIEND_REQUEST) ? Const.GROUP_KEY_ACCEPT_FRIEND_NOTIFICATION : Const.GROUP_KEY_SENT_FRIEND_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        var notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        var mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        var audioAttr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttr);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);

        notificationManager.createNotificationChannel(mChannel);
        notificationManager.notify(0, notificationBuilder.build());
    }

    private Intent getIntentByAction(String actionClick, String uid, String agentId, String imageUrl) {
        if (actionClick == null) {
            Logging.show("Action click must not be null");
            return null;
        }

        switch (actionClick) {
            case Const.DEFAULT_NEW_FRIEND_REQUEST:
                var friendRequestIntent = new Intent(getApplicationContext(), PreferenceFriendRequestActivity.class);
                friendRequestIntent.putExtra(EXTRA_KEY, EXTRA_ACTION_NEW_FRIEND);
                friendRequestIntent.putExtra(Const.KEY_UID, uid);
                friendRequestIntent.putExtra(Const.KEY_AGENT_ID, agentId);
                friendRequestIntent.putExtra(Const.KEY_IMAGE_URL, imageUrl);
                friendRequestIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return friendRequestIntent;
            case Const.DEFAULT_ACCEPTED_FRIEND_REQUEST:
                var acceptedFriendIntent = new Intent(getApplicationContext(), ConnectPeopleActivity.class);
                acceptedFriendIntent.putExtra(EXTRA_KEY, EXTRA_ACTION_ACCEPTED);
                acceptedFriendIntent.putExtra(Const.KEY_UID, uid);
                acceptedFriendIntent.putExtra(Const.KEY_AGENT_ID, agentId);
                acceptedFriendIntent.putExtra(Const.KEY_IMAGE_URL, imageUrl);
                return acceptedFriendIntent;
            default:
                return null;
        }
    }

    private boolean isAppForeground() {
        /*
         * Check the app is in foreground or background
         * Detail link: {https://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not}
         * */
        var runningAppInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(runningAppInfo);

        return runningAppInfo.importance == IMPORTANCE_FOREGROUND ||
                runningAppInfo.importance == IMPORTANCE_VISIBLE;
    }

    private void startWorkFetchNotification() {
        var constraint =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        var workRequest =
                new OneTimeWorkRequest.Builder(FetchNotificationWorker.class)
                        .setConstraints(constraint)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork("notification_worker", ExistingWorkPolicy.REPLACE, workRequest);
    }
}
