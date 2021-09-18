package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface HistoryLoggedInUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(HistoryLoggedInUser historyUser);

    @Query("SELECT * FROM history_logged_in_user")
    Single<List<HistoryLoggedInUser>> getAll();

    @Query("UPDATE history_logged_in_user SET is_login = 0 WHERE uid = :uid")
    Completable signOut(String uid);
}
