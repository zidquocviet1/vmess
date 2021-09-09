package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class MainViewModel extends FirebaseUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> remoteUser = new MutableLiveData<>();

    @Inject
    public MainViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;

        loadFirebaseUser();
        loadRemoteUser(null);
    }

    public LiveData<Result<User>> getRemoteUser() {
        return remoteUser;
    }

    private void loadRemoteUser(@Nullable User remoteUser) {
        var user = firebaseUser.getValue();

        if (user != null) {
            userRepository.fetchUserFromRemote(remoteUser, user, observable -> {
                if (observable != null) {
                    cd.add(observable.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    this.remoteUser.setValue(Result.Success(response.getSuccess()));
                                } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    this.remoteUser.setValue(Result.Fail(R.string.error_authentication_fail));
                                }
                            }, t -> this.remoteUser.setValue(Result.Fail(R.string.error_connect_server_fail))));
                } else {
                    this.remoteUser.setValue(Result.Fail(R.string.error_authentication_fail));
                }
            });
        }
    }
}
