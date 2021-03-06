package com.mqv.vmess.activity.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.BuildConfig;
import com.mqv.vmess.R;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.data.model.SignInProvider;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.HistoryLoggedInUserRepository;
import com.mqv.vmess.data.repository.LoginRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.exception.BadRequestException;
import com.mqv.vmess.network.exception.FirebaseUnauthorizedException;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.ui.validator.LoginForm;
import com.mqv.vmess.ui.validator.LoginFormValidator;
import com.mqv.vmess.ui.validator.LoginRegisterValidationResult;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;

import java.net.HttpURLConnection;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Headers;

@HiltViewModel
public class LoginViewModel extends ViewModel {
    private final MutableLiveData<LoginRegisterValidationResult> loginValidationResult = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> demoLoginResult = new MutableLiveData<>();
    private final CompositeDisposable cd = new CompositeDisposable();
    private final LoginRepository loginRepository;
    private final HistoryLoggedInUserRepository historyUserRepository;
    private final PeopleRepository peopleRepository;
    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private FirebaseUser currentLoginFirebaseUser;
    private FirebaseUser previousFirebaseUser;
    private FirebaseUser loginUserOnStop;

    @Inject
    public LoginViewModel(LoginRepository loginRepository,
                          HistoryLoggedInUserRepository historyUserRepository,
                          PeopleRepository peopleRepository,
                          NotificationRepository notificationRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          FriendRequestRepository friendRequestRepository) {
        this.loginRepository = loginRepository;
        this.historyUserRepository = historyUserRepository;
        this.peopleRepository = peopleRepository;
        this.notificationRepository = notificationRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    public LiveData<LoginRegisterValidationResult> getLoginValidationResult() {
        return loginValidationResult;
    }

    public LiveData<Result<User>> getLoginResult() {
        return loginResult;
    }

    public LiveData<Result<User>> getDemoLoginResult() { return demoLoginResult; }

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

    public void loginForDemoSection() {
        Disposable disposable = loginRepository.loginForDemoSection()
                .startWith(Completable.fromAction(() -> demoLoginResult.postValue(Result.Loading())))
                .flatMap(response -> {
                    if (response.isSuccessful()) {
                        Headers           headers  = response.headers();
                        String            username = headers.get("username");
                        ApiResponse<User> body     = response.body();

                        if (username == null) return Observable.error(NoResponseHeaderException::new);
                        if (body == null)     return Observable.error(new FirebaseUnauthorizedException(-1));

                        return Observable.just(body)
                                         .compose(RxHelper.parseResponseData())
                                         .map(user -> new DemoUserData(username, user));
                    }
                    return Observable.error(new FirebaseUnauthorizedException(-1));
                })
                .compose(RxHelper.applyObservableSchedulers())
                .subscribe(data -> {
                    AuthCredential credential = EmailAuthProvider.getCredential(data.getUsername(), BuildConfig.DEMO_PASSWORD);
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener(authResult -> {
                                if (authResult.isSuccessful()) {
                                    AuthResult result = authResult.getResult();

                                    if (result != null && result.getUser() != null) {
                                        FirebaseUser firebaseUser = result.getUser();
                                        saveLoggedInUserForDemo(data.getUser(), fetchHistoryUser(firebaseUser));
                                    }
                                } else if (authResult.isCanceled()) {
                                    Exception e = authResult.getException();
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
                                    demoLoginResult.postValue(Result.Fail(error));
                                }
                            });
                }, t -> {
                    if (t instanceof BadRequestException) {
                        demoLoginResult.postValue(Result.Fail(R.string.msg_login_failed));
                    } else {
                        demoLoginResult.postValue(Result.Fail(R.string.error_unknown));
                    }
                });

        cd.add(disposable);
    }

    private void saveLoggedInUserForDemo(User user, HistoryLoggedInUser historyUser) {
        cd.add(loginRepository.saveLoggedInUser(user, historyUser)
                .compose(RxHelper.applyCompleteSchedulers())
                .subscribe(() -> {
                            demoLoginResult.postValue(Result.Success(user));

                            sendFcmTokenToServer();

                            AppDependencies.getDatabaseObserver().notifyOnLoginStateChanged();
                        },
                        t -> demoLoginResult.postValue(Result.Fail(R.string.error_authentication_fail)))
        );
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
            AppDependencies.closeAllConnection();

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

                            AppDependencies.getDatabaseObserver().notifyOnLoginStateChanged();
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

        fetchNotification();
    }

    private void fetchNotification() {
        Disposable disposable = notificationRepository.fetchNotification(1)
                .compose(RxHelper.parseResponseData())
                .flatMapIterable(list -> list)
                .flatMap(fn -> userRepository.fetchUserFromRemote(fn.getSenderId())
                        .compose(RxHelper.parseResponseData())
                        .flatMapCompletable(user -> friendRequestRepository.isFriend(user.getUid())
                                .flatMapCompletable(isFriend -> peopleRepository.save(People.mapFromUser(user, isFriend))))
                        .subscribeOn(Schedulers.io())
                        .doOnError(t -> Logging.show(t.getMessage()))
                        .toSingleDefault(fn)
                        .toObservable())
                .toList()
                .concatMapCompletable(notificationRepository::saveCachedNotification)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onErrorComplete()
                .subscribe();

        cd.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!cd.isDisposed()) cd.dispose();
    }

    private static class DemoUserData {
        private final String username;
        private final User user;

        private DemoUserData(String username, User user) {
            this.username = username;
            this.user = user;
        }

        public String getUsername() {
            return username;
        }

        public User getUser() {
            return user;
        }
    }

    private static class NoResponseHeaderException extends Exception {
        NoResponseHeaderException() {}
    }
}