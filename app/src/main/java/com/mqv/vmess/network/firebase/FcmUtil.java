package com.mqv.vmess.network.firebase;

import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mqv.vmess.util.Logging;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public final class FcmUtil {
    @WorkerThread
    public static Optional<String> getToken() {
        String token = null;
        try {
            token = Tasks.await(FirebaseMessaging.getInstance().getToken());
        } catch (ExecutionException e) {
            Logging.show("Was interrupted while waiting for the token.");
        } catch (InterruptedException e) {
            Logging.show("Failed to get the token." + e.getCause());
        }
        return Optional.ofNullable(TextUtils.isEmpty(token) ? null : token);
    }
}
