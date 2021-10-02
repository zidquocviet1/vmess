package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class MainViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PeopleRepository peopleRepository;
    private final MutableLiveData<Result<User>> remoteUser = new MutableLiveData<>();
    private final MutableLiveData<List<People>> listPeople = new MutableLiveData<>();

    @Inject
    public MainViewModel(UserRepository userRepository,
                         FriendRequestRepository friendRequestRepository,
                         PeopleRepository peopleRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.peopleRepository = peopleRepository;

        loadFirebaseUser();
        loadRemoteUserUsingNBR(null);
        loadAllPeople();
    }

    public LiveData<Result<User>> getRemoteUser() {
        return remoteUser;
    }

    public LiveData<List<People>> getListPeople() {
        return listPeople;
    }

    private void loadRemoteUserUsingNBR(@Nullable User remoteUser) {
        var user = firebaseUser.getValue();
        if (user != null) {
            var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

            cd.add(userRepository.fetchUserUsingNBS(remoteUser, user)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(listUser -> {
                        var targetUser = listUser.stream()
                                .filter(u -> u.getUid().equals(uid))
                                .findAny()
                                .orElse(null);
                        this.remoteUser.setValue(Result.Success(targetUser));
                    }, t -> this.remoteUser.setValue(Result.Fail(R.string.error_connect_server_fail))));
        }
    }

    private void loadAllPeople() {
        cd.add(peopleRepository
                .fetchPeopleUsingNBS(this::createCall, this::handleAuthError)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.listPeople::setValue,
                        t -> this.listPeople.setValue(null)));
    }

    private void createCall(String token) {
        cd.add(friendRequestRepository.getFriendListId(token)
                .flatMap((Function<ApiResponse<List<String>>, ObservableSource<String>>) response ->
                        response.getStatusCode() == HttpURLConnection.HTTP_OK ?
                                Observable.fromIterable(response.getSuccess()) : null)
                .flatMap((Function<String, ObservableSource<ApiResponse<People>>>) s ->
                        peopleRepository.getConnectPeopleByUid(s, token))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ApiResponse<People>>() {
                    final ArrayList<People> freshPeopleList = new ArrayList<>();

                    @Override
                    public void onNext(@NonNull ApiResponse<People> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            var p = response.getSuccess();
                            freshPeopleList.add(p);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        peopleRepository.save(freshPeopleList)
                                .subscribeOn(Schedulers.io())
                                .subscribe(new CompletableObserver() {
                                    @Override
                                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onComplete() {
                                        Logging.show("Add people list successfully");
                                    }

                                    @Override
                                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                        e.printStackTrace();
                                        Logging.show("Save people list into database failure");
                                    }
                                });
                    }
                }));
    }

    private void handleAuthError(Exception e) {

    }
}
