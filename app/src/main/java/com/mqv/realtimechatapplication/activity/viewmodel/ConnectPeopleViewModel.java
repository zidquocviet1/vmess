package com.mqv.realtimechatapplication.activity.viewmodel;

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
public class ConnectPeopleViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> connectUserResult = new MutableLiveData<>();

    @Inject
    public ConnectPeopleViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        loadFirebaseUser();
    }

    public LiveData<Result<User>> getConnectUserResult() {
        return connectUserResult;
    }

    public void getConnectUserByQrCode(String code) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            connectUserResult.setValue(Result.Loading());

            userRepository.getConnectUserByQrCode(code,
                    firebaseUser,
                    observable -> cd.add(observable.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    connectUserResult.setValue(Result.Success(response.getSuccess()));
                                } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail));
                                }else if (response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                                    connectUserResult.setValue(Result.Fail(R.string.error_qr_code_not_found));
                                }
                            }, t -> connectUserResult.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                    e -> connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail)));
        }
    }

    public void getConnectUserByUsername(String username) {

    }

    public void resetConnectUserResult() {
        connectUserResult.setValue(null);
    }

    public void dispose(){
        if (!cd.isDisposed())
            cd.dispose();
    }
}
