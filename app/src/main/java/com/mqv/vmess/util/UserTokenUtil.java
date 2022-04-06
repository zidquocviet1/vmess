package com.mqv.vmess.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.mqv.vmess.network.exception.NetworkException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import io.reactivex.rxjava3.core.Single;

public final class UserTokenUtil {
    public static Optional<String> getToken(@NonNull FirebaseUser user) throws Throwable {
        String token = null;
        try {
            var tokenResult = Tasks.await(user.getIdToken(true));
            token = tokenResult.getToken();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();

            if (t instanceof FirebaseNetworkException) {
                throw t;
            }

            Logging.show("Was interrupted while waiting for the token.");
        } catch (InterruptedException e) {
            Logging.show("Failed to get the token." + e.getCause());
        }
        return Optional.ofNullable(TextUtils.isEmpty(token) ? null : token);
    }

    public static Single<String> getTokenSingle(@NonNull FirebaseUser user) {
        return Single.create(emitter -> user.getIdToken(true).addOnCompleteListener(state -> {
            if (state.isSuccessful() && !emitter.isDisposed()) {
                GetTokenResult result = state.getResult();
                if (result != null) {
                    emitter.onSuccess(Const.PREFIX_TOKEN + result.getToken());
                } else {
                    emitter.onError(state.getException());
                }
            } else {
                Exception e = state.getException();

                if (e != null) {
                    if (e instanceof FirebaseNetworkException) {
                        emitter.onError(new NetworkException());
                    } else {
                        emitter.onError(e);
                    }
                }
            }
        }));
    }
}
