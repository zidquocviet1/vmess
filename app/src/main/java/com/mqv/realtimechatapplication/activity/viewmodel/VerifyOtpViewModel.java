package com.mqv.realtimechatapplication.activity.viewmodel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.validator.OtpCodeFormState;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class VerifyOtpViewModel extends ViewModel {
    private final MutableLiveData<OtpCodeFormState> otpCodeFormState = new MutableLiveData<>();
    private final MutableLiveData<Long> timeOut = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();

    private final LoginRepository loginRepository;

    @Inject
    public VerifyOtpViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public LiveData<OtpCodeFormState> getOtpCodeFormState() {
        return otpCodeFormState;
    }

    public LiveData<Long> getTimeOut() {
        return timeOut;
    }

    public LiveData<Result<User>> getLoginResult() {
        return loginResult;
    }

    public void timeOutChanged(Long value) {
        timeOut.setValue(value);
    }

    public void otpCodeChanged(String code1,
                               String code2,
                               String code3,
                               String code4,
                               String code5,
                               String code6) {
        if (TextUtils.isEmpty(code1)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else if (TextUtils.isEmpty(code2)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else if (TextUtils.isEmpty(code3)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else if (TextUtils.isEmpty(code4)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else if (TextUtils.isEmpty(code5)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else if (TextUtils.isEmpty(code6)) {
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        } else {
            otpCodeFormState.setValue(new OtpCodeFormState(true));
        }
    }

    public void loginWithPhoneAuthCredential(Activity activity, PhoneAuthCredential credential) {
        FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();

                        if (result != null) {
                            var user = result.getUser();
                            if (user != null)
                                loginWithToken(user);
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                loginResult.setValue(Result.Fail(R.string.error_verification_code_incorrect));
                            }else if (task.getException() instanceof FirebaseNetworkException){
                                loginResult.setValue(Result.Fail(R.string.error_network_connection));
                            }else{
                                loginResult.setValue(Result.Fail(R.string.error_unknown));
                            }
                        }, 1500);
                    }
                });
    }

    private void loginWithToken(FirebaseUser user) {
        loginRepository.loginWithUidAndToken(user, observable ->
                        cd.add(observable
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(this::handleLoginSuccess, this::handleLoginError)),
                e -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    private void handleLoginSuccess(ApiResponse<User> response) {
        var code = response.getStatusCode();

        if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
            loginResult.setValue(Result.Success(response.getSuccess()));
        } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            loginResult.setValue(Result.Fail(R.string.error_authentication_fail));
        }
    }

    private void handleLoginError(Throwable e) {
        loginResult.setValue(Result.Fail(R.string.error_connect_server_fail));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
