package com.mqv.vmess.data.repository;

import com.mqv.vmess.data.model.HistoryLoggedInUser;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public interface HistoryLoggedInUserRepository {
    Single<List<HistoryLoggedInUser>> getAll();

    Completable signOut(String uid);

    Completable save(HistoryLoggedInUser historyUser);

    Completable updateDisplayName(String uid, String newName);

    Completable updatePhotoUrl(String uid, String newPhotoUrl);

    Completable deleteHistoryUser(HistoryLoggedInUser historyUser);
}
