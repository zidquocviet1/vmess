package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.ui.data.LoggedInUserView;
import com.mqv.realtimechatapplication.data.result.LoginResult;
import com.mqv.realtimechatapplication.ui.validator.LoginForm;
import com.mqv.realtimechatapplication.ui.validator.LoginFormValidator;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class LoginViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> loginValidationResult = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();
    private final LoginRepository loginRepository;

    @Inject
    public LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public LiveData<LoginRegisterValidationResult> getLoginValidationResult() {
        return loginValidationResult;
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        // can be launched in a separate asynchronous job
        var result = loginRepository.login(username, password);

        if (result.getStatus() == NetworkStatus.SUCCESS) {
            LoggedInUser data = result.getData();
            loginResult.setValue(new LoginResult(new LoggedInUserView(data.getDisplayName())));
        } else {
            loginResult.setValue(new LoginResult(R.string.msg_login_failed));
        }
    }

    public void loginDataChanged(String username, String password) {
        var form = new LoginForm(username, password);

        var result = LoginFormValidator.isUsernameValid()
                .and(LoginFormValidator.isPasswordValid())
                .apply(form);

        loginValidationResult.setValue(result);
    }

    public void fetchCustomUserInfo(String token) {
        cd.add(loginRepository.fetchCustomUserInfo(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                        var user = response.getSuccess();

                        Logging.show("Hello from Spring Boot");
                    }
                }, Throwable::printStackTrace));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!cd.isDisposed()) cd.dispose();
    }
}