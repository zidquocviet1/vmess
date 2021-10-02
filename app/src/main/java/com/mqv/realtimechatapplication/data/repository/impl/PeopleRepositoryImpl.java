package com.mqv.realtimechatapplication.data.repository.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.data.dao.PeopleDao;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.NetworkBoundResource;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

public class PeopleRepositoryImpl implements PeopleRepository {
    private final PeopleDao peopleDao;
    private final UserService userService;
    private final FirebaseUser user;
    private final FriendRequestService friendRequestService;

    @Inject
    public PeopleRepositoryImpl(PeopleDao peopleDao, UserService userService,
                                FriendRequestService friendRequestService) {
        this.peopleDao = peopleDao;
        this.userService = userService;
        this.friendRequestService = friendRequestService;
        this.user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public Flowable<List<People>> getAll() {
        return peopleDao.getAll();
    }

    @Override
    public Completable save(People people) {
        return peopleDao.save(people);
    }

    @Override
    public Completable save(List<People> peopleList) {
        return peopleDao.save(peopleList);
    }

    @Override
    public Completable delete(People people) {
        return peopleDao.delete(people);
    }

    @Override
    public Completable deleteAll() {
        Logging.show(String.format("Sign out user with uid: %s, display name: %s", user.getUid(), user.getDisplayName()));
        return peopleDao.deleteAll();
    }

    @Override
    public Observable<List<People>> fetchPeopleUsingNBS(Consumer<String> onAuthSuccess,
                                                        Consumer<Exception> onAuthFail) {
        return new NetworkBoundResource<List<People>, List<People>>(true) {
            @Override
            protected void saveCallResult(@NonNull List<People> item) {

            }

            @Override
            protected Boolean shouldFetch(@Nullable List<People> data) {
                if (user == null)
                    return false;

                if (data == null || data.isEmpty())
                    return true;

                var time = data.stream()
                        .map(p -> p.getAccessedDate().plusMinutes(10).compareTo(LocalDateTime.now()))
                        .filter(i -> i <= 0)
                        .collect(Collectors.toList());

                return !time.isEmpty();
            }

            @Override
            protected Flowable<List<People>> loadFromDb() {
                return getAll();
            }

            @Override
            protected Observable<List<People>> createCall() {
                return null;
            }

            @Override
            protected void callAndSaveResult() {
                validateIdToken(onAuthSuccess, onAuthFail);
            }
        }.asObservable();
    }

    private void validateIdToken(Consumer<String> onAuthSuccess,
                                 Consumer<Exception> onAuthFail) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();
                        var token = Objects.requireNonNull(result).getToken();

                        onAuthSuccess.accept(token);
                    } else {
                        onAuthFail.accept(task.getException());
                    }
                });
    }

    @Override
    public Observable<ApiResponse<People>> getConnectPeopleByUid(String uid, String token) {
        return userService.getConnectPeopleByUid(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, uid);
    }

    @Override
    public void unfriend(String uid,
                         Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                         Consumer<Exception> onAuthFail) {
        validateIdToken(token ->
                        onAuthSuccess.accept(friendRequestService.unfriend(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                uid)),
                onAuthFail);
    }
}
