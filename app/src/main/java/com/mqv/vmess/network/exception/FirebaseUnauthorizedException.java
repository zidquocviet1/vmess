package com.mqv.vmess.network.exception;

import com.google.firebase.auth.FirebaseAuthException;

public class FirebaseUnauthorizedException extends FirebaseAuthException {
    private final int error;

    public FirebaseUnauthorizedException(int errorRes) {
        super("a", "a");
        this.error = errorRes;
    }

    public int getError() {
        return error;
    }
}
