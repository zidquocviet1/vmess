package com.mqv.realtimechatapplication.activity.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
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
    private final MutableLiveData<Result<Boolean>> addUserStatus = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();

    private final UserRepository userRepository;

    @Inject
    public VerifyOtpViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<OtpCodeFormState> getOtpCodeFormState() {
        return otpCodeFormState;
    }

    public LiveData<Long> getTimeOut() {
        return timeOut;
    }

    public LiveData<Result<Boolean>> getAddUserStatus() {
        return addUserStatus;
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

    public void addUser(String uid) {
        cd.add(userRepository.addUser(uid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    var code = response.code();
                    if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_NO_CONTENT) {
                        addUserStatus.setValue(Result.Success(true));
                    } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        addUserStatus.setValue(Result.Fail(R.string.error_authentication_fail));
                    }
                }, t -> {
                    addUserStatus.setValue(Result.Fail(R.string.error_connect_server_fail));
                }));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
