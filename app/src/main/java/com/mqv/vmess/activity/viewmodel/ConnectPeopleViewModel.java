package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;

import java.net.HttpURLConnection;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConnectPeopleViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> connectUserResult = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> connectUserIdResult = new MutableLiveData<>();
    private Disposable qrCodeDisposable;
    private Disposable usernameDisposable;
    private static final int QR_CODE_REQUEST = -1;
    private static final int USERNAME_REQUEST = 0;

    @Inject
    public ConnectPeopleViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        loadFirebaseUser();
    }

    public LiveData<Result<User>> getConnectUserResult() {
        return connectUserResult;
    }

    public LiveData<Result<User>> getConnectUserIdResult() {
        return connectUserIdResult;
    }

    public void getConnectUserByQrCode(String code) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            connectUserResult.setValue(Result.Loading());

            userRepository.getConnectUserByQrCode(code, firebaseUser, observable ->
                    handleRemoteCall(observable, QR_CODE_REQUEST), handleFirebaseAuthError());
        }
    }

    public void getConnectUserByUsername(String username) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            connectUserResult.setValue(Result.Loading());

            userRepository.getConnectUserByUsername(username, firebaseUser, observable ->
                    handleRemoteCall(observable, USERNAME_REQUEST), handleFirebaseAuthError());
        }
    }

    public void getConnectUserByUid(String uid) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            connectUserResult.setValue(Result.Loading());

            userRepository.getConnectUserByUid(firebaseUser,
                    uid,
                    observable -> cd.add(observable.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    connectUserIdResult.setValue(Result.Success(response.getSuccess()));
                                } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    connectUserIdResult.setValue(Result.Fail(R.string.error_authentication_fail));
                                }
                            }, t -> {
                                var message = t.getMessage();

                                if (message != null && message.equals("HTTP 404 ")) {
                                    connectUserIdResult.setValue(Result.Fail(R.string.error_qr_code_not_found));
                                } else
                                    connectUserIdResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                            })),
                    handleFirebaseAuthError());
        }
    }

    private void handleRemoteCall(Observable<ApiResponse<User>> observable, int requestCode) {
        var disposable = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                        connectUserResult.setValue(Result.Success(response.getSuccess()));
                    } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail));
                    } else if (response.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        connectUserResult.setValue(Result.Fail(R.string.error_qr_code_not_found));
                    }
                }, t -> {
                    var message = t.getMessage();

                    if (message != null && message.equals("HTTP 404 ")) {
                        connectUserResult.setValue(Result.Fail(R.string.error_qr_code_not_found));
                    } else
                        connectUserResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                });

        if (requestCode == QR_CODE_REQUEST) {
            qrCodeDisposable = disposable;
        } else if (requestCode == USERNAME_REQUEST) {
            usernameDisposable = disposable;
        }

        cd.add(disposable);
    }

    private Consumer<Exception> handleFirebaseAuthError() {
        return e -> connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail));
    }

    public void resetConnectUserResult() {
        connectUserResult.setValue(null);
    }

    public void removeQrCodeObservable() {
        if (qrCodeDisposable != null) {
            cd.remove(qrCodeDisposable);
            qrCodeDisposable = null;
        }
    }

    public void removeUsernameDisposable() {
        if (usernameDisposable != null) {
            cd.remove(usernameDisposable);
            usernameDisposable = null;
        }
    }
}
