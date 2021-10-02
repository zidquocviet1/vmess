package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.ui.data.People;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

public interface PeopleRepository {
    Flowable<List<People>> getAll();

    Completable save(List<People> peopleList);

    Completable deleteAll();

    Observable<List<People>> fetchPeopleUsingNBS(Consumer<String> onAuthSuccess,
                                                 Consumer<Exception> onAuthFail);
}
