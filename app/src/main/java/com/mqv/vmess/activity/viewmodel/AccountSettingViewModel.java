package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.HistoryLoggedInUserRepository;
import com.mqv.vmess.data.repository.LoginRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.manager.LoggedInUserManager;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class AccountSettingViewModel extends LogoutHandlerViewModel {
    private final LoginRepository loginRepository;
    private final MutableLiveData<Result<Boolean>> signOutStatus = new MutableLiveData<>();
    private Disposable logoutDisposable;
    private Disposable logoutLocalDisposable;

    @Inject
    public AccountSettingViewModel(HistoryLoggedInUserRepository historyUserRepository,
                                   PeopleRepository peopleRepository,
                                   LoginRepository loginRepository,
                                   NotificationRepository notificationRepository,
                                   ConversationRepository conversationRepository) {
        super(historyUserRepository, peopleRepository, notificationRepository, conversationRepository, loginRepository);

        this.loginRepository = loginRepository;
    }

    public LiveData<Result<Boolean>> getSignOutStatus() {
        return signOutStatus;
    }

    public void signOut(FirebaseUser currentUser) {
        signOutStatus.setValue(Result.Loading());

        loginRepository.logoutWithObserve(currentUser, observable -> {
            var disposable = observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            LoggedInUserManager.getInstance().signOut();
                            FirebaseAuth.getInstance().signOut();
                            AppDependencies.closeAllConnection();

                            var localDisposable = removeAfterLogout(currentUser.getUid())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .subscribe(() -> signOutStatus.postValue(Result.Success(response.getSuccess())),
                                            t -> handleSignOutError(R.string.error_unknown));

                            logoutLocalDisposable = localDisposable;

                            cd.add(localDisposable);
                        } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                            handleSignOutError(R.string.error_authentication_fail);
                        } else {
                            handleSignOutError(R.string.error_unknown);
                        }
                    }, t -> handleSignOutError(R.string.error_connect_server_fail));

            logoutDisposable = disposable;

            cd.add(disposable);
        }, e -> handleSignOutError(R.string.error_unknown));
    }

    private void handleSignOutError(int error) {
        signOutStatus.setValue(Result.Fail(error));
    }

    public void dispose() {
        if (logoutDisposable != null && !logoutDisposable.isDisposed())
            logoutDisposable.dispose();

        if (logoutLocalDisposable != null && !logoutLocalDisposable.isDisposed())
            logoutLocalDisposable.dispose();
    }
}
