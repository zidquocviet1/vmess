package com.mqv.realtimechatapplication.activity.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.ui.validator.OtpCodeFormState;


public class VerifyOtpViewModel extends ViewModel {
    private final MutableLiveData<OtpCodeFormState> otpCodeFormState = new MutableLiveData<>();
    private final MutableLiveData<Long> timeOut = new MutableLiveData<>();

    public VerifyOtpViewModel(){

    }

    public LiveData<OtpCodeFormState> getOtpCodeFormState() {
        return otpCodeFormState;
    }

    public LiveData<Long> getTimeOut() {
        return timeOut;
    }

    public void timeOutChanged(Long value){
        timeOut.setValue(value);
    }

    public void otpCodeChanged(String code1,
                               String code2,
                               String code3,
                               String code4,
                               String code5,
                               String code6){
        if (TextUtils.isEmpty(code1)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else if (TextUtils.isEmpty(code2)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else if (TextUtils.isEmpty(code3)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else if (TextUtils.isEmpty(code4)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else if (TextUtils.isEmpty(code5)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else if (TextUtils.isEmpty(code6)){
            otpCodeFormState.setValue(new OtpCodeFormState(false));
        }else{
            otpCodeFormState.setValue(new OtpCodeFormState(true));
        }
    }
}
