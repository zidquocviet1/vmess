package com.mqv.realtimechatapplication.network.firebase;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.provider.Settings;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.preferences.AppPreferences;
import com.mqv.realtimechatapplication.activity.preferences.AppPreferencesImpl;
import com.mqv.realtimechatapplication.activity.preferences.PreferenceFriendRequestActivity;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Locale;

public class MessagingService extends FirebaseMessagingService {

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

        Logging.show(String.format(Locale.getDefault(),
                "onMessageReceived() ID: %s, Delay: %d, Priority: %d, Original Priority: %d",
                remoteMessage.getMessageId(),
                (System.currentTimeMillis() - remoteMessage.getSentTime()),
                remoteMessage.getPriority(),
                remoteMessage.getOriginalPriority()));

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            var data = remoteMessage.getData();

            Logging.show("Message data payload: " + data);

            var title = data.get("title");
            var body = data.get("body");
            var channelId = data.get("channelId");
            var channelName = data.get("channelName");
            var actionClick = data.get("actionClick");
            var imageUrl = data.get("imageUrl");

            var intent = getIntentByAction(actionClick);

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

            loadImageAndPushNotification(channelId, channelName, title, body, pendingIntent, imageUrl);
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

    private void loadImageAndPushNotification(String channelId, String channelName,
                                              String title, String body, PendingIntent pendingIntent,
                                              @Nullable String imageUrl) {
        var glideRequest = GlideApp.with(this);

        if (imageUrl != null){
            var photoUrl = imageUrl.replace("localhost", Const.BASE_IP);

            glideRequest.asBitmap()
                    .load(photoUrl)
                    .error(R.drawable.ic_round_account)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            showNotification(channelId, channelName, title, body, pendingIntent, resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }else{
            var bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_round_account);
            showNotification(channelId, channelName, title, body, pendingIntent, bitmap);
        }
    }

    private void showNotification(String channelId, String channelName,
                                  String title, String body, PendingIntent pendingIntent,
                                  @Nullable Bitmap bitmap) {

        var formatBody = Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT);

        var notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(formatBody)
                .setLargeIcon(bitmap)
                .setAutoCancel(true) // Remove notification when user clicked
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(pendingIntent)
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

    private Intent getIntentByAction(String actionClick) {
        if (actionClick == null) {
            Logging.show("Action click must not be null");
            return null;
        }

        switch (actionClick) {
            case "newFriendRequest":
                return new Intent(getApplicationContext(), PreferenceFriendRequestActivity.class);
            case "acceptedFriendRequest":
                return new Intent(getApplicationContext(), MainActivity.class);
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
}
