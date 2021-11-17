package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isDisplayNameValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isEmailValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isPasswordValid;
import static com.mqv.realtimechatapplication.ui.validator.RegisterFormValidator.isRePasswordValid;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.ui.validator.RegisterForm;

import java.net.ConnectException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class RegisterViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> registerValidationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Integer>> registerResult = new MutableLiveData<>();
    private final UserRepository repository;
    private final CompositeDisposable cd;

    @Inject
    public RegisterViewModel(UserRepository repository) {
        this.repository = repository;
        this.cd = new CompositeDisposable();
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
        Disposable disposable = repository.registerEmailAndPassword(email, password, displayName)
                                          .doOnSubscribe(d -> {
                                              if (!d.isDisposed()) registerResult.postValue(Result.Loading());
                                          })
                                          .subscribeOn(Schedulers.io())
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  registerResult.postValue(Result.Success(R.string.msg_register_successfully));
                                              } else {
                                                  registerResult.postValue(Result.Fail(R.string.error_auth_email_in_use));
                                              }
                                          }, t -> {
                                              if (t instanceof ConnectException) {
                                                  registerResult.postValue(Result.Fail(R.string.error_network_connection));
                                              } else {
                                                  registerResult.postValue(Result.Fail(R.string.error_connect_server_fail));
                                              }
                                          });

        cd.add(disposable);
    }
}
