package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.MessageStatus;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private final MutableLiveData<Result<User>> remoteUserResult = new MutableLiveData<>();
    private final MutableLiveData<List<People>> listPeople = new MutableLiveData<>();
    private final MutableLiveData<List<People>> activePeopleList = new MutableLiveData<>();
    private final MutableLiveData<List<Conversation>> conversationList = new MutableLiveData<>();
    private final MutableLiveData<List<RemoteUser>> remoteUserList = new MutableLiveData<>();

    public AbstractMainViewModel(UserRepository userRepository,
                                 FriendRequestRepository friendRequestRepository,
                                 PeopleRepository peopleRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.peopleRepository = peopleRepository;

        loadFirebaseUser();
        loadLoggedInUser();

        loadRemoteUserUsingNBR();
        loadAllPeople();
        newData();
    }

    public abstract void onRefresh();

    protected MutableLiveData<Result<User>> getRemoteUserResult() {
        return remoteUserResult;
    }

    protected MutableLiveData<List<People>> getListPeople() {
        return listPeople;
    }

    protected MutableLiveData<List<Conversation>> getConversationList() {
        return conversationList;
    }

    protected MutableLiveData<List<RemoteUser>> getRemoteUserList() {
        return remoteUserList;
    }

    protected MutableLiveData<List<People>> getActivePeopleList() {
        return activePeopleList;
    }

    private void loadRemoteUserUsingNBR() {
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

    private void loadAllPeople() {
        cd.add(peopleRepository
                .fetchPeopleUsingNBS(this::createCall, this::handleAuthError)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.listPeople::setValue,
                        t -> this.listPeople.setValue(null)));
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

    /// test
    public void newData() {
        var listConversation = Arrays.asList(
                new Conversation(1L, "Phạm Thảo Duyên", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(2L, "Phạm Băng Băng", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(3L, "Triệu Lệ Dĩnh", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(4L, "Ngô Diệc Phàm", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(5L, "Lưu Đức Hoa", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(6L, "Pham thao duyen 1", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(7L, "Pham Bang Bang 1", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(8L, "Trieu Le Dinh 1", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(9L, "Ngo Diec Pham 1", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(10L, "Luu Duc Hoa 1", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(11L, "Pham thao duyen 2", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(12L, "Pham Bang Bang 2", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(13L, "Trieu Le Dinh 2", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(14L, "Ngo Diec Pham 2", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(15L, "Luu Duc Hoa 2", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED)
        );

        var listRemoteUser = Arrays.asList(
                new RemoteUser("1", "Thảo Duyên", "Phạm"),
                new RemoteUser("2", "Băng Băng", "Phạm"),
                new RemoteUser("3", "Lệ Dĩnh", "Triệu"),
                new RemoteUser("4", "Diệc Phàm", "Ngô"),
                new RemoteUser("5", "Đức Hoa", "Lưu"),
                new RemoteUser("6", "Viet", "Mai"),
                new RemoteUser("7", "Han", "Ngoc"),
                new RemoteUser("8", "Nhu", "Tran"),
                new RemoteUser("9", "Mai", "Phuong"),
                new RemoteUser("10", "Tuyen", "Huyen")
        );
        conversationList.setValue(listConversation);
        remoteUserList.setValue(listRemoteUser);
    }
}
