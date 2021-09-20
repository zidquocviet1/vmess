package com.mqv.realtimechatapplication.activity.viewmodel;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
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
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Const;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ManageAccountViewModel extends CurrentUserViewModel {
    private final HistoryLoggedInUserRepository historyUserRepository;
    private final LoginRepository loginRepository;
    private final MutableLiveData<Result<User>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<List<HistoryLoggedInUser>> historyUserList = new MutableLiveData<>();
    private static final int LOADING_SIMULATION_TIME = 1500;

    @Inject
    public ManageAccountViewModel(HistoryLoggedInUserRepository historyUserRepository,
                                  LoginRepository loginRepository) {
        this.historyUserRepository = historyUserRepository;
        this.loginRepository = loginRepository;
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

    private void getAllHistoryUser() {
        cd.add(historyUserRepository.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(historyUserList::setValue, t -> {
                    t.printStackTrace();
                    historyUserList.setValue(null);
                }));
    }

    public void switchAccountWithPhoneNumber(Activity activity, String phoneNumber) {
        var phoneOptions = new PhoneAuthOptions.Builder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setActivity(activity)
                .setTimeout(Const.PHONE_AUTH_TIME_OUT, TimeUnit.SECONDS)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {

                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                    }
                });
        PhoneAuthProvider.verifyPhoneNumber(phoneOptions.build());
    }

    private void signInWithAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        var previousFirebaseUser = Objects.requireNonNull(getFirebaseUser().getValue());
        FirebaseAuth.getInstance().signOut();

        loginResult.setValue(Result.Loading());

        FirebaseAuth.getInstance()
                .signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(task -> {
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

    public void switchAccountWithEmailAndPassword(String email, String password) {
        /*
         * Get the current firebase logged in. And then, sign out to notify Id token user change.
         * Call signInAgainFirebaseUser() when switch to new User not complete
         * */
        var previousFirebaseUser = Objects.requireNonNull(getFirebaseUser().getValue());
        FirebaseAuth.getInstance().signOut();

        loginResult.setValue(Result.Loading());

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
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
                            error = R.string.msg_login_failed;
                        }else if (e instanceof FirebaseTooManyRequestsException){
                            error = R.string.error_too_many_request;
                        }else{
                            error = R.string.error_unknown;
                        }
                        handler.postDelayed(() -> loginResult.setValue(Result.Fail(error)), LOADING_SIMULATION_TIME);
                    }
                });
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
        cd.add(loginRepository.saveLoggedInUser(user, historyUser)
                .andThen(historyUserRepository.signOut(previousUser.getUid()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> loginResult.setValue(Result.Success(user)),
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

    private void signInAgainFirebaseUser(FirebaseUser previousUser) {
        FirebaseAuth.getInstance().updateCurrentUser(previousUser);
    }
}
