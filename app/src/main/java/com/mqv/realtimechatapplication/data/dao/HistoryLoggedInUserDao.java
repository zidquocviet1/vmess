package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
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

    @Query("UPDATE history_logged_in_user SET display_name = :newName WHERE uid = :uid")
    Completable updateDisplayName(String uid, String newName);

    @Query("UPDATE history_logged_in_user SET photo_url = :newUrl WHERE uid = :uid")
    Completable updatePhotoUrl(String uid, String newUrl);

    @Delete
    Completable delete(HistoryLoggedInUser user);

    @Query("SELECT * FROM history_logged_in_user WHERE is_login = 1")
    Single<HistoryLoggedInUser> getLoggedInUser();
}
