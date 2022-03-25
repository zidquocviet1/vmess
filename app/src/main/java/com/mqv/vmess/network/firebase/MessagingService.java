package com.mqv.vmess.network.firebase;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.AppPreferencesImpl;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.notification.NotificationPayload;
import com.mqv.vmess.util.Logging;

import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MessagingService extends FirebaseMessagingService {
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
}
