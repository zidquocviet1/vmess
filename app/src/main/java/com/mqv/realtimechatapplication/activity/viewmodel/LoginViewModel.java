package com.mqv.realtimechatapplication.activity.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.SignInProvider;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.HistoryLoggedInUserRepository;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.validator.LoginForm;
import com.mqv.realtimechatapplication.ui.validator.LoginFormValidator;
import com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class LoginViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> loginValidationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();
    private final LoginRepository loginRepository;
    private final HistoryLoggedInUserRepository historyUserRepository;
    private final PeopleRepository peopleRepository;
    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private FirebaseUser currentLoginFirebaseUser;
    private FirebaseUser previousFirebaseUser;
    private FirebaseUser loginUserOnStop;

    @Inject
    public LoginViewModel(LoginRepository loginRepository,
                          HistoryLoggedInUserRepository historyUserRepository,
                          PeopleRepository peopleRepository,
                          NotificationRepository notificationRepository,
                          ConversationRepository conversationRepository) {
        this.loginRepository = loginRepository;
        this.historyUserRepository = historyUserRepository;
        this.peopleRepository = peopleRepository;
        this.notificationRepository = notificationRepository;
        this.conversationRepository = conversationRepository;
    }

    public LiveData<LoginRegisterValidationResult> getLoginValidationResult() {
        return loginValidationResult;
    }

    public LiveData<Result<User>> getLoginResult() {
        return loginResult;
    }

    public FirebaseUser getCurrentLoginFirebaseUser() {
        return currentLoginFirebaseUser;
    }

    public FirebaseUser getPreviousFirebaseUser() {
        return previousFirebaseUser;
    }

    public void setLoginUserOnStop(FirebaseUser user) {
        this.loginUserOnStop = user;
    }

    public void switchAccountWithEmailAndPassword(String email, String password) {
        previousFirebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
        FirebaseAuth.getInstance().signOut();

        loginWithAuthCredential(EmailAuthProvider.getCredential(email, password), previousFirebaseUser);
    }

    public void loginWithEmailAndPassword(String email, String password) {
        loginWithAuthCredential(EmailAuthProvider.getCredential(email, password), null);
    }

    private void loginWithAuthCredential(AuthCredential credential, @Nullable FirebaseUser previousUser) {
        loginResult.setValue(Result.Loading());

        loginRepository.login(
                credential,
                e -> {
                    signInAgainFirebaseUser(previousUser);

                    var handler = new Handler(Looper.getMainLooper());
                    int error;

                    if (e instanceof FirebaseNetworkException) {
                        error = R.string.error_network_connection;
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        error = R.string.msg_login_failed;
                    } else if (e instanceof FirebaseAuthInvalidUserException) {
                        error = R.string.invalid_username_not_exists;
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        error = R.string.error_too_many_request;
                    } else {
                        error = R.string.error_unknown;
                    }
                    handler.postDelayed(() -> loginResult.setValue(Result.Fail(error)), 1500);
                },
                (observable, user) -> {
                    currentLoginFirebaseUser = user;

                    cd.add(observable
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(response -> {
                                var code = response.getStatusCode();

                                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                                    saveLoggedInUser(previousUser, response.getSuccess(), fetchHistoryUser(user));
                                } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    handleLoginError(previousUser, R.string.error_authentication_fail);
                                }
                            }, t -> handleLoginError(previousUser, R.string.error_connect_server_fail)));
                },
                e -> handleLoginError(previousUser, R.string.error_authentication_fail));
    }

    private void handleLoginError(@Nullable FirebaseUser previousUser, int error) {
        signInAgainFirebaseUser(previousUser);
        loginResult.setValue(Result.Fail(error));
    }

    private HistoryLoggedInUser fetchHistoryUser(FirebaseUser user) {
        var uri = user.getPhotoUrl();
        var url = uri != null ? uri.toString().replace("localhost", Const.BASE_IP) : null;

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

    private void saveLoggedInUser(@Nullable FirebaseUser previousUser, User user, HistoryLoggedInUser historyUser) {
        Completable saveRequest;

        if (previousUser != null) {
            logoutPreviousUser(previousUser);

            saveRequest = historyUserRepository.signOut(previousUser.getUid())
                    .andThen(peopleRepository.deleteAll())
                    .andThen(notificationRepository.deleteAllLocal())
                    .andThen(conversationRepository.deleteAll())
                    .andThen(loginRepository.saveLoggedInUser(user, historyUser));
        } else {
            saveRequest = loginRepository.saveLoggedInUser(user, historyUser);
        }

        cd.add(saveRequest
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            if (loginUserOnStop != null) {
                                signInAgainFirebaseUser(loginUserOnStop);
                                loginUserOnStop = null;
                            }
                            loginResult.setValue(Result.Success(user));

                            sendFcmTokenToServer();
                        },
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

    public void signInAgainFirebaseUser(@Nullable FirebaseUser previousUser) {
        if (previousUser != null)
            FirebaseAuth.getInstance().updateCurrentUser(previousUser);
    }

    private void logoutPreviousUser(FirebaseUser previousFirebaseUser) {
        loginRepository.logout(previousFirebaseUser);
    }

    private void sendFcmTokenToServer() {
        var currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loginRepository.sendFcmToken(currentUser);

        fetchNotification(Objects.requireNonNull(currentUser).getUid());
    }

    private void fetchNotification(String uid) {
        notificationRepository.fetchNotification(uid, 1)
                .subscribeOn(Schedulers.io())
                .subscribe(new DisposableObserver<>() {
                    @Override
                    public void onNext(@androidx.annotation.NonNull ApiResponse<List<Notification>> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK){
                            notificationRepository.saveCachedNotification(response.getSuccess())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(new DisposableCompletableObserver() {
                                        @Override
                                        public void onComplete() {
                                            Logging.show("Fetch notification when login SUCCESSFULLY");
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {

                                        }
                                    });
                        }else{
                            Logging.show("Fetch notification when login FAIL");
                        }
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!cd.isDisposed()) cd.dispose();
    }
}