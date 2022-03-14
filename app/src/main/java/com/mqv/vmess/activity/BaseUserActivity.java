package com.mqv.vmess.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.ViewModel;
import androidx.viewbinding.ViewBinding;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.network.model.User;

public abstract class BaseUserActivity<V extends ViewModel, B extends ViewBinding> extends ToolbarActivity<V, B>{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerFirebaseUserChange(this::refreshFirebaseUserUi);

        registerLoggedInUserChanged(this::refreshLoggedInUserUi);
    }

    @UiThread
    public abstract void refreshFirebaseUserUi(FirebaseUser user);

    @UiThread
    public abstract void refreshLoggedInUserUi(User user);
}
