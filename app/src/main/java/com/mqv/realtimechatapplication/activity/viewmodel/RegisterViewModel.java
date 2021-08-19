package com.mqv.realtimechatapplication.activity.viewmodel;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.RegisterRepository;
import com.mqv.realtimechatapplication.ui.validator.RegisterFormState;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<RegisterFormState> registerFormState = new MutableLiveData<>();
    private final RegisterRepository repository;

    @Inject
    public RegisterViewModel(RegisterRepository registerRepository){
        this.repository = registerRepository;
    }

    public LiveData<RegisterFormState> getRegisterFormState(){ return registerFormState; }

    public void registerDataChanged(String username,
                                    String displayName,
                                    String password,
                                    String rePassword,
                                    boolean isPhoneType){
        if (!isUserNameValid(isPhoneType, username)){
            var form = new RegisterFormState();
            form.setUsernameError(R.string.invalid_username);
            registerFormState.setValue(form);
        }else if (TextUtils.isEmpty(displayName)){
            var form = new RegisterFormState();
            form.setDisplayNameError(R.string.invalid_display_name);
            registerFormState.setValue(form);
        }else if (password.length() < 8){
            var form = new RegisterFormState();
            form.setPasswordError(R.string.invalid_password);
            registerFormState.setValue(form);
        }else if (!rePassword.equals(password)){
            var form = new RegisterFormState();
            form.setRePasswordError(R.string.invalid_re_password);
            registerFormState.setValue(form);
        }else{
            registerFormState.setValue(new RegisterFormState(true));
        }
    }

    private boolean isUserNameValid(boolean isPhoneType, String input){
        return isPhoneType ? Pattern.compile(Const.PHONE_REGEX_PATTERN).matcher(input).matches()
                : Patterns.EMAIL_ADDRESS.matcher(input).matches();
    }

    public void register(Map<String, Object> payload){
        repository.login(payload);
    }
}
