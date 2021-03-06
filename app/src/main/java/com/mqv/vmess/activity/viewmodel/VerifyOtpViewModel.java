package com.mqv.vmess.activity.viewmodel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.mqv.vmess.R;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.data.model.SignInProvider;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.HistoryLoggedInUserRepository;
import com.mqv.vmess.data.repository.LoginRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.validator.OtpCodeFormState;
import com.mqv.vmess.util.Const;

import java.net.HttpURLConnection;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class VerifyOtpViewModel extends LogoutHandlerViewModel {
    private final MutableLiveData<OtpCodeFormState> otpCodeFormState = new MutableLiveData<>();
    private final MutableLiveData<Long> timeOut = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();

    private final LoginRepository loginRepository;

    @Inject
    public VerifyOtpViewModel(HistoryLoggedInUserRepository historyUserRepository,
                              PeopleRepository peopleRepository,
                              LoginRepository loginRepository,
                              NotificationRepository notificationRepository,
                              ConversationRepository conversationRepository) {
        super(historyUserRepository, peopleRepository, notificationRepository, conversationRepository, loginRepository);
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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();

                        if (result != null) {
                            var user = result.getUser();
                            if (user != null)
                                loginWithToken(user, currentUser);
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                loginResult.setValue(Result.Fail(R.string.error_verification_code_incorrect));
                            } else if (task.getException() instanceof FirebaseNetworkException) {
                                loginResult.setValue(Result.Fail(R.string.error_network_connection));
                            } else {
                                loginResult.setValue(Result.Fail(R.string.error_unknown));
                            }
                        }, 1500);
                    }
                });
    }

    private void loginWithToken(FirebaseUser user, FirebaseUser previousUser) {
        loginRepository.loginWithUidAndToken(user, observable ->
                        cd.add(observable
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(response -> handleLoginSuccess(response, user, previousUser), this::handleLoginError)),
                e -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    private void handleLoginSuccess(ApiResponse<User> response, FirebaseUser user, FirebaseUser previousUser) {
        var code = response.getStatusCode();

        if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
            saveLoggedInUser(response.getSuccess(), fetchHistoryUser(user), previousUser);
        } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            loginResult.setValue(Result.Fail(R.string.error_authentication_fail));
        }
    }

    private void handleLoginError(Throwable e) {
        loginResult.setValue(Result.Fail(R.string.error_connect_server_fail));
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

    private void saveLoggedInUser(User user, HistoryLoggedInUser historyUser, FirebaseUser previousUser) {
        if (previousUser != null) {
            cd.add(logout(previousUser, user, historyUser)
                    .andThen(Completable.fromAction(() -> logoutToRemovePreviousBundleKey(previousUser)))
                    .compose(RxHelper.applyCompleteSchedulers())
                    .subscribe(() -> loginResult.setValue(Result.Success(user)),
                            t -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail)))
            );
        } else {
            cd.add(loginRepository.saveLoggedInUser(user, historyUser)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> loginResult.setValue(Result.Success(user)),
                            t -> loginResult.setValue(Result.Fail(R.string.error_authentication_fail))));
        }
        AppDependencies.getDatabaseObserver().notifyOnLoginStateChanged();
    }

    private void logoutToRemovePreviousBundleKey(FirebaseUser previousFirebaseUser) {
        loginRepository.logoutWithObserve(previousFirebaseUser, observable -> {
            var disposable = observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {}, t -> {});
            cd.add(disposable);
        }, e -> {});
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
