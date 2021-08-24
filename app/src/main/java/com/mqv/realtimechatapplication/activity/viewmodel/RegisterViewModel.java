package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isDisplayNameValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isEmailValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isPasswordValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isRePasswordValid;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.data.repository.RegisterRepository;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.ui.validator.RegisterForm;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> registerValidationResult = new MutableLiveData<>();
    private final RegisterRepository repository;

    @Inject
    public RegisterViewModel(RegisterRepository registerRepository) {
        this.repository = registerRepository;
    }


    public LiveData<LoginRegisterValidationResult> getRegisterValidationResult() {
        return registerValidationResult;
    }

    public void registerDataChanged(String username,
                                    String displayName,
                                    String password,
                                    String rePassword) {
        var form = new RegisterForm(username, displayName, password, rePassword);

        var result = isEmailValid()
                .and(isDisplayNameValid())
                .and(isPasswordValid())
                .and(isRePasswordValid())
                .apply(form);

        registerValidationResult.setValue(result);
    }

    public void register(Map<String, Object> payload) {
        repository.login(payload);
    }
}
