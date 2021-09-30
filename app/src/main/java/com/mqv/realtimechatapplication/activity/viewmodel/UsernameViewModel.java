package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class UsernameViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> updateResult = new MutableLiveData<>();
    private final MutableLiveData<Result<String>> usernameStatus = new MutableLiveData<>();

    @Inject
    public UsernameViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;

        loadLoggedInUser();
        loadFirebaseUser();
    }

    public LiveData<String> getUsername() {
        return Transformations.map(getLoggedInUser(), user -> {
            if (user.getUsername() == null)
                return "";
            return user.getUsername();
        });
    }

    public LiveData<Result<User>> getUpdateResult() {
        return updateResult;
    }

    public LiveData<Result<String>> getUsernameStatus() {
        return usernameStatus;
    }

    public void observeQueryTextChanged(Observable<String> observable) {
        observable.filter(s -> !s.isEmpty())
                .distinctUntilChanged()
                .switchMap((Function<String, ObservableSource<String>>) s ->
                        Observable.create((ObservableOnSubscribe<String>) emitter -> {
                            if (!emitter.isDisposed()) {
                                emitter.onNext(s);
                            }
                        }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        cd.add(d);
                    }

                    @Override
                    public void onNext(@NonNull String s) {
                        Logging.show("Username: " + s);
                        checkUserConnectName(s);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void editUsername(String username) {
        var user = getLoggedInUser().getValue();
        var firebaseUser = getFirebaseUser().getValue();

        if (user != null && firebaseUser != null) {
            var updatedUser = new User(user);
            updatedUser.setUsername(username);

            updateResult.setValue(Result.Loading());

            userRepository.editUserConnectName(updatedUser,
                    firebaseUser,
                    observable -> cd.add(observable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                var code = response.getStatusCode();

                                if (code == HttpURLConnection.HTTP_OK) {
                                    saveCallResult(response.getSuccess());
                                } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    updateResult.setValue(Result.Fail(R.string.error_authentication_fail));
                                } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                                    updateResult.setValue(Result.Fail(R.string.error_user_id_not_found));
                                } else if (code == HttpURLConnection.HTTP_CONFLICT) {
                                    updateResult.setValue(Result.Fail(R.string.error_user_connect_name_conflict));
                                }
                            }, t -> updateResult.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                    e -> updateResult.setValue(Result.Fail(R.string.error_authentication_fail)));
        } else
            updateResult.setValue(Result.Fail(R.string.error_user_id_not_found));
    }

    private void saveCallResult(User user) {
        cd.add(userRepository.addUserToDb(user)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> updateResult.setValue(Result.Success(user)),
                        t -> Logging.show("Insert user fail with id = " + user.getUid()))
        );
    }

    public void checkUserConnectName(String username) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            usernameStatus.setValue(Result.Loading());

            userRepository.checkUserConnectName(username,
                    firebaseUser,
                    observable -> cd.add(observable.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                var code = response.getStatusCode();

                                if (code == HttpURLConnection.HTTP_OK) {
                                    var isExists = response.getSuccess();
                                    if (isExists) {
                                        usernameStatus.setValue(Result.Fail(R.string.error_user_connect_name_conflict));
                                    } else {
                                        usernameStatus.setValue(Result.Success(null));
                                    }
                                } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    usernameStatus.setValue(Result.Fail(R.string.error_authentication_fail));
                                }
                            }, t -> usernameStatus.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                    e -> usernameStatus.setValue(Result.Fail(R.string.error_authentication_fail))
            );
        }
    }
}
