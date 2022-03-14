package com.mqv.vmess.data.repository.impl;

import com.mqv.vmess.data.dao.HistoryLoggedInUserDao;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.data.repository.HistoryLoggedInUserRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class HistoryLoggedInUserRepositoryImpl implements HistoryLoggedInUserRepository {
    private final HistoryLoggedInUserDao historyUserDao;

    @Inject
    public HistoryLoggedInUserRepositoryImpl(HistoryLoggedInUserDao historyUserDao) {
        this.historyUserDao = historyUserDao;
    }

    @Override
    public Single<List<HistoryLoggedInUser>> getAll() {
        return historyUserDao.getAll();
    }

    @Override
    public Completable signOut(String uid) {
        return historyUserDao.signOut(uid);
    }

    @Override
    public Completable save(HistoryLoggedInUser historyUser) {
        return historyUserDao.save(historyUser);
    }

    @Override
    public Completable updateDisplayName(String uid, String newName) {
        return historyUserDao.updateDisplayName(uid, newName);
    }

    @Override
    public Completable updatePhotoUrl(String uid, String newPhotoUrl) {
        return historyUserDao.updatePhotoUrl(uid, newPhotoUrl);
    }

    @Override
    public Completable deleteHistoryUser(HistoryLoggedInUser historyUser) {
        return historyUserDao.delete(historyUser);
    }
}
