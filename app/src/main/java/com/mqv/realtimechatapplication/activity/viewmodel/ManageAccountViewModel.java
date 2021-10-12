package com.mqv.realtimechatapplication.activity.viewmodel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.SignInProvider;
import com.mqv.realtimechatapplication.data.repository.HistoryLoggedInUserRepository;
import com.mqv.realtimechatapplication.data.repository.LoginRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ManageAccountViewModel extends CurrentUserViewModel {
    private final HistoryLoggedInUserRepository historyUserRepository;
    private final LoginRepository loginRepository;
    private final PeopleRepository peopleRepository;
    private final NotificationRepository notificationRepository;
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<HistoryLoggedInUser> verifyResult = new MutableLiveData<>();
    private final MutableLiveData<List<HistoryLoggedInUser>> historyUserList = new MutableLiveData<>();
    private static final int LOADING_SIMULATION_TIME = 1500;
    private static final int DEFAULT_REQUEST_EMAIL = 1;
    private static final int DEFAULT_REQUEST_PHONE = 2;
    private String mVerifyCodeId;
    private boolean isTimeOut;
    private FirebaseUser previousFirebaseUser;
    private FirebaseUser loginUserOnStop;

    @Inject
    public ManageAccountViewModel(HistoryLoggedInUserRepository historyUserRepository,
                                  LoginRepository loginRepository,
                                  PeopleRepository peopleRepository,
                                  NotificationRepository notificationRepository) {
        this.historyUserRepository = historyUserRepository;
        this.loginRepository = loginRepository;
        this.peopleRepository = peopleRepository;
        this.notificationRepository = notificationRepository;

        getAllHistoryUser();
        loadLoggedInUser();
        loadFirebaseUser();
    }

    public LiveData<List<HistoryLoggedInUser>> getHistoryUserList() {
        return historyUserList;
    }

    public LiveData<Result<User>> getLoginResult() {
        return loginResult;
    }

    public LiveData<HistoryLoggedInUser> getVerifyResult() {
        return verifyResult;
    }

    public FirebaseUser getPreviousFirebaseUser() {
        return previousFirebaseUser;
    }

    public void setLoginUserOnStop(FirebaseUser user) {
        this.loginUserOnStop = user;
    }

    private void getAllHistoryUser() {
        cd.add(historyUserRepository.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(historyUserList::setValue, t -> {
                    t.printStackTrace();
                    historyUserList.setValue(null);
                }));
    }

    public void deleteHistoryUser(HistoryLoggedInUser user) {
        historyUserRepository.deleteHistoryUser(user)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        Logging.show("Delete history logged in user with id = " + user.getUid());
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Logging.show("Fail to delete history logged in user with id = " + user.getUid());
                    }
                });
    }

    public void requestVerifyPhoneAuth(Activity activity, HistoryLoggedInUser historyUser) {
        var phoneNumber = historyUser.getPhoneNumber() != null ? historyUser.getPhoneNumber() : "";
        var phoneOptions = new PhoneAuthOptions.Builder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setActivity(activity)
                .setTimeout(Const.PHONE_AUTH_TIME_OUT, TimeUnit.SECONDS)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        // This callback is invoked in an invalid request for verification is made,
                        // for instance if the the phone number format is not valid.
                        Logging.show("onVerificationFailed " + e);
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Invalid request
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            // The SMS quota for the project has been exceeded
                        }

                        // Show a message and update the UI
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        isTimeOut = false;
                        mVerifyCodeId = s;
                        verifyResult.postValue(historyUser);
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        isTimeOut = true;
                    }
                });
        PhoneAuthProvider.verifyPhoneNumber(phoneOptions.build());
    }

    public void switchAccountWithPhoneNumber(String smsCode) {
        if (isTimeOut)
            loginResult.setValue(Result.Fail(R.string.msg_verification_code_not_available));
        else
            signInWithAuthCredential(PhoneAuthProvider.getCredential(mVerifyCodeId, smsCode), DEFAULT_REQUEST_PHONE);
    }

    public void switchAccountWithEmailAndPassword(String email, String password) {
        /*
         * Get the current firebase logged in. And then, sign out to notify Id token user change.
         * Call signInAgainFirebaseUser() when switch to new User not complete
         * */
        signInWithAuthCredential(EmailAuthProvider.getCredential(email, password), DEFAULT_REQUEST_EMAIL);
    }

    private void signInWithAuthCredential(AuthCredential phoneAuthCredential, int signInMethod) {
        previousFirebaseUser = Objects.requireNonNull(getFirebaseUser().getValue());
        FirebaseAuth.getInstance().signOut();

        loginResult.setValue(Result.Loading());

        FirebaseAuth.getInstance()
                .signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(handleOnSignInComplete(previousFirebaseUser, signInMethod));
    }

    private OnCompleteListener<AuthResult> handleOnSignInComplete(FirebaseUser previousFirebaseUser, int signInMethod) {
        return task -> {
            if (task.isSuccessful()) {
                var result = task.getResult();

                if (result != null) {
                    /*
                     * In here the current firebase user was changed
                     * To make sure the login session is complete. Make a call to own backend server
                     * */
                    var user = Objects.requireNonNull(result.getUser());
                    loginWithToken(previousFirebaseUser, user);
                }
            } else {
                /*
                 * Task is not success, so we need to signInAgainFirebaseUser
                 * */
                signInAgainFirebaseUser(previousFirebaseUser);

                var e = task.getException();
                var handler = new Handler(Looper.getMainLooper());
                int error;

                if (e instanceof FirebaseNetworkException) {
                    error = R.string.error_network_connection;
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    error = signInMethod == DEFAULT_REQUEST_EMAIL ? R.string.msg_login_failed : R.string.error_verification_code_incorrect;
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    error = R.string.error_too_many_request;
                } else {
                    error = R.string.error_unknown;
                }
                handler.postDelayed(() -> loginResult.setValue(Result.Fail(error)), LOADING_SIMULATION_TIME);
            }
        };
    }

    private void loginWithToken(FirebaseUser previousUser, FirebaseUser user) {
        loginRepository.loginWithUidAndToken(user, observable ->
                        cd.add(observable
                                .subscribeOn(Schedulers.io())
                                .doOnDispose(() -> signInAgainFirebaseUser(previousUser))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(response -> handleLoginSuccess(previousUser, response, user),
                                        e -> handleLoginError(previousUser, R.string.error_connect_server_fail))
                        ),
                e -> handleLoginError(previousUser, R.string.error_authentication_fail));
    }

    private void handleLoginSuccess(FirebaseUser previousUser, ApiResponse<User> response, FirebaseUser user) {
        var code = response.getStatusCode();

        if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
            saveLoggedInUser(previousUser, response.getSuccess(), fetchHistoryUser(user));
        } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            handleLoginError(previousUser, R.string.error_authentication_fail);
        }
    }

    private void handleLoginError(FirebaseUser previousUser, int error) {
        signInAgainFirebaseUser(previousUser);
        loginResult.setValue(Result.Fail(error));
    }

    private void saveLoggedInUser(FirebaseUser previousUser, User user, HistoryLoggedInUser historyUser) {
        logoutPreviousUser(previousUser);

        cd.add(historyUserRepository.signOut(previousUser.getUid())
                .andThen(peopleRepository.deleteAll())
                .andThen(notificationRepository.deleteAllLocal())
                .andThen(loginRepository.saveLoggedInUser(user, historyUser))
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

    public void signInAgainFirebaseUser(FirebaseUser previousUser) {
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
        cd.add(notificationRepository.fetchNotification(uid, 1)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    if (response.getStatusCode() == HttpURLConnection.HTTP_OK){
                        notificationRepository.saveCachedNotification(response.getSuccess())
                                .subscribeOn(Schedulers.io())
                                .subscribe(new DisposableCompletableObserver() {
                                    @Override
                                    public void onComplete() {
                                        Logging.show("Fetch notification when login SUCCESSFULLY");
                                    }

                                    @Override
                                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {

                                    }
                                });
                    }else{
                        Logging.show("Fetch notification when login FAIL");
                    }
                }, Throwable::printStackTrace));
    }
}
