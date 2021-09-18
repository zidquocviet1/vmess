package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isDisplayNameValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isEmailValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isPasswordValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isRePasswordValid;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.ui.validator.RegisterForm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> registerValidationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Integer>> registerResult = new MutableLiveData<>();
    private final ExecutorService registerExecutor = Executors.newSingleThreadExecutor();
    private static final int SIMULATION_LOADING_TIME = 2000;

    public RegisterViewModel() {
    }

    public LiveData<LoginRegisterValidationResult> getRegisterValidationResult() {
        return registerValidationResult;
    }

    public LiveData<Result<Integer>> getRegisterResult() {
        return registerResult;
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

    public void createUserWithEmailAndPassword(String email, String password, String displayName) {
        registerResult.postValue(Result.Loading());

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(registerExecutor, task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();
                        if (result != null) {
                            var user = result.getUser();
                            if (user != null) {
                                var profileChangeRequest = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();
                                user.updateProfile(profileChangeRequest);

                                new Handler(Looper.getMainLooper()).postDelayed(() ->
                                        registerResult.postValue(Result.Success(R.string.msg_register_successfully)), SIMULATION_LOADING_TIME);
                            }
                        }
                    } else {
                        var e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    registerResult.postValue(Result.Fail(R.string.error_auth_email_in_use)), SIMULATION_LOADING_TIME);
                        } else {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    registerResult.postValue(Result.Fail(R.string.error_create_user_not_complete)), SIMULATION_LOADING_TIME);
                        }
                    }
                });
    }
}
