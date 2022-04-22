package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.User;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class CurrentUserViewModel extends ViewModel {
    protected final MutableLiveData<FirebaseUser> firebaseUser = new MutableLiveData<>();
    protected final MutableLiveData<User> loggedInUser = new MutableLiveData<>();
    protected final CompositeDisposable cd = new CompositeDisposable();

    protected void loadFirebaseUser(){
        var user = FirebaseAuth.getInstance().getCurrentUser();
        firebaseUser.setValue(user);
    }

    protected void loadLoggedInUser(){
        var user = LoggedInUserManager.getInstance().getLoggedInUser();
        loggedInUser.setValue(user);
    }

    public LiveData<FirebaseUser> getFirebaseUser() {
        return firebaseUser;
    }

    public LiveData<User> getLoggedInUser() {
        return loggedInUser;
    }

    public void addDisposable(Disposable disposable) {
        cd.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
