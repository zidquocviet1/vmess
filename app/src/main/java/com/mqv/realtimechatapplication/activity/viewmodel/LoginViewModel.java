package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.ui.validator.LoginForm;
import com.mqv.realtimechatapplication.ui.validator.LoginFormValidator;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class LoginViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> loginValidationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();
    private final LoginRepository loginRepository;
    private final UserRepository userRepository;

    @Inject
    public LoginViewModel(LoginRepository loginRepository, UserRepository repository) {
        this.loginRepository = loginRepository;
        this.userRepository = repository;
    }

    public LiveData<LoginRegisterValidationResult> getLoginValidationResult() {
        return loginValidationResult;
    }

    public LiveData<Result<Boolean>> getLoginResult() {
        return loginResult;
    }

    public void loginWithEmailAndPassword(String email, String password) {
        loginResult.setValue(Result.Loading());

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();

                        if (result != null) {
                            var user = Objects.requireNonNull(result.getUser());
                            addUser(user.getUid());
                        }
                    } else {
                        loginResult.setValue(Result.Fail(R.string.msg_login_failed));
                    }
                });
    }

    private void addUser(String uid) {
        cd.add(userRepository.addUser(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    var code = response.code();

                    if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_NO_CONTENT){
                        loginResult.setValue(Result.Success(true));
                    }else if (code == HttpURLConnection.HTTP_UNAUTHORIZED){
                        FirebaseAuth.getInstance().signOut();
                        loginResult.setValue(Result.Fail(R.string.error_authentication_fail));
                    }
                }, t -> {
                    FirebaseAuth.getInstance().signOut();
                    loginResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                }));
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