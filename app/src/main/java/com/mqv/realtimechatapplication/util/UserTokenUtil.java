package com.mqv.realtimechatapplication.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public final class UserTokenUtil {
    @WorkerThread
    public static Optional<String> getToken(@NonNull FirebaseUser user) {
        String token = null;
        try {
            var tokenResult = Tasks.await(user.getIdToken(true));
            token = tokenResult.getToken();
        } catch (ExecutionException e) {
            Logging.show("Was interrupted while waiting for the token.");
        } catch (InterruptedException e) {
            Logging.show("Failed to get the token." + e.getCause());
        }
        return Optional.ofNullable(TextUtils.isEmpty(token) ? null : token);
    }
}
