package com.mqv.vmess.data.repository;

import androidx.annotation.NonNull;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.ui.data.People;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface PeopleRepository {
    Flowable<List<People>> getAll();

    Single<List<People>> getSuggestionList();

    Single<People> getCachedByUid(@NonNull String uid);

    Completable save(People people);

    Completable save(List<People> peopleList);

    Completable delete(People people);

    Completable deleteAll();

    Observable<List<People>> fetchPeopleUsingNBS(Consumer<String> onAuthSuccess,
                                                 Consumer<Exception> onAuthFail);

    Observable<ApiResponse<People>> getConnectPeopleByUid(String uid, String token);

    Observable<ApiResponse<People>> getConnectPeopleByUid(String uid);

    void unfriend(String uid,
                  Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                  Consumer<Exception> onAuthFail);
}
