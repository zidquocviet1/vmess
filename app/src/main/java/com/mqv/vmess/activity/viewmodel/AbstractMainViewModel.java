package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Logging;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class AbstractMainViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PeopleRepository peopleRepository;
    private final NotificationRepository notificationRepository;

    private final MutableLiveData<Result<User>> remoteUserResult = new MutableLiveData<>();
    private final MutableLiveData<List<People>> listPeople       = new MutableLiveData<>();
    private final MutableLiveData<List<People>> activePeopleList = new MutableLiveData<>();

    protected static final int NOTIFICATION_DURATION_LIMIT = 1;

    public AbstractMainViewModel(UserRepository userRepository,
                                 FriendRequestRepository friendRequestRepository,
                                 PeopleRepository peopleRepository,
                                 NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.peopleRepository = peopleRepository;
        this.notificationRepository = notificationRepository;

        loadFirebaseUser();
        loadLoggedInUser();
    }

    public abstract void onRefresh();

    protected MutableLiveData<Result<User>> getRemoteUserResult() {
        return remoteUserResult;
    }

    protected MutableLiveData<List<People>> getListPeople() {
        return listPeople;
    }

    protected MutableLiveData<List<People>> getActivePeopleList() {
        return activePeopleList;
    }

    protected void loadRemoteUserUsingNBR() {
        var user = firebaseUser.getValue();
        if (user != null) {
            var uid = user.getUid();

            cd.add(userRepository.fetchUserUsingNBS(null, user)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(listUser -> {
                        var targetUser = listUser.stream()
                                .filter(u -> u.getUid().equals(uid))
                                .findAny()
                                .orElse(null);
                        this.remoteUserResult.setValue(Result.Success(targetUser));
                    }, t -> this.remoteUserResult.setValue(Result.Fail(R.string.error_connect_server_fail))));
        }
    }

    protected void loadAllPeople() {
        cd.add(peopleRepository
                .fetchPeopleUsingNBS(this::createCall, this::handleAuthError)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(people -> listPeople.postValue(people.stream()
                                               .filter(People::getFriend)
                                               .collect(Collectors.toList())),
                        t -> this.listPeople.setValue(null)));
    }

    protected void loadAllRemoteNotification() {
        Disposable disposable = notificationRepository.fetchNotification(NOTIFICATION_DURATION_LIMIT)
                                                      .compose(RxHelper.parseResponseData())
                                                      .flatMapIterable(list -> list)
                                                      .flatMap(fn -> userRepository.fetchUserFromRemote(fn.getSenderId())
                                                                                   .compose(RxHelper.parseResponseData())
                                                                                   .flatMapCompletable(user -> friendRequestRepository.isFriend(user.getUid())
                                                                                           .flatMapCompletable(isFriend -> peopleRepository.save(People.mapFromUser(user, isFriend))))
                                                                                   .subscribeOn(Schedulers.io())
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

    private void handleAuthError(Exception e) {

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
                            p.setFriend(true);
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

    public void forceClearDispose() {
        cd.clear();
    }
}
