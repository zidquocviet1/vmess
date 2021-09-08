package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public abstract class FirebaseUserViewModel extends ViewModel {
    protected final MutableLiveData<FirebaseUser> firebaseUser = new MutableLiveData<>();

    protected void loadFirebaseUser(){
        var user = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUser.setValue(user);
    }

    public LiveData<FirebaseUser> getFirebaseUser() {
        return firebaseUser;
    }
}