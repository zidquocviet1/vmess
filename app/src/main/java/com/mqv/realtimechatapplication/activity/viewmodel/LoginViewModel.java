package com.mqv.realtimechatapplication.activity.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.SignInProvider;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.validator.LoginForm;
import com.mqv.realtimechatapplication.ui.validator.LoginFormValidator;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.util.Const;

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
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();
    private final LoginRepository loginRepository;

    @Inject
    public LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public LiveData<LoginRegisterValidationResult> getLoginValidationResult() {
        return loginValidationResult;
    }

    public LiveData<Result<User>> getLoginResult() {
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
                            loginWithUidAndToken(user);
                        }
                    } else {
                        var e = task.getException();

                        if (e instanceof FirebaseNetworkException) {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    loginResult.setValue(Result.Fail(R.string.error_network_connection)), 1500);
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    loginResult.setValue(Result.Fail(R.string.msg_login_failed)), 1500);
                        }
                    }
                });
    }

    private void loginWithUidAndToken(@NonNull FirebaseUser user) {
        loginRepository.loginWithUidAndToken(user, observable ->
                        cd.add(observable.observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(response -> {
                                    var code = response.getStatusCode();

                                    if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {

                                        saveLoggedInUser(response.getSuccess(), fetchHistoryUser(user));
                                    } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                        loginResult.setValue(Result.Fail(R.string.error_authentication_fail));
                                    }
                                }, t -> loginResult.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                e -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    private HistoryLoggedInUser fetchHistoryUser(FirebaseUser user) {
        var uri = user.getPhotoUrl();
        var url = uri != null ? uri.toString().replace("localhost", Const.BASE_IP) : "";

        var signInProvider = user.getProviderData()
                .stream()
                .map(userInfo -> {
                    // TODO: need to check the other sign in methods
                    var provider = SignInProvider.getSignInProvider(userInfo.getProviderId());
                    if (provider == SignInProvider.EMAIL) {
                        provider.setUsername(userInfo.getEmail());
                    } else if (provider == SignInProvider.PHONE) {
                        provider.setUsername(user.getPhoneNumber());
                    }
                    return provider;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        var historyUserBuilder = new HistoryLoggedInUser.Builder()
                .setUid(user.getUid())
                .setDisplayName(user.getDisplayName())
                .setLogin(true)
                .setPhotoUrl(url)
                .setProvider(signInProvider);

        if (signInProvider == SignInProvider.EMAIL) {
            historyUserBuilder.setEmail(signInProvider.getUsername());
        } else if (signInProvider == SignInProvider.PHONE) {
            historyUserBuilder.setPhoneNumber(signInProvider.getUsername());
        }

        return historyUserBuilder.build();
    }

    private void saveLoggedInUser(User user, HistoryLoggedInUser historyUser) {
        cd.add(loginRepository.saveLoggedInUser(user, historyUser)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> loginResult.setValue(Result.Success(user)),
                        t -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail)))
        );
    }

    public void loginDataChanged(String username, String password) {
        var form = new LoginForm(username, password);

        var result = LoginFormValidator.isUsernameValid()
                .and(LoginFormValidator.isPasswordValid())
                .apply(form);

        loginValidationResult.setValue(result);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!cd.isDisposed()) cd.dispose();
    }
}