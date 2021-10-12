package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mqv.realtimechatapplication.network.model.User;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(User user);

    @Query("update user set photo_url = :photoUrl where uid = :uid")
    Completable updateUserPhotoUrl(String uid, String photoUrl);

    @Query("select * from user")
    Flowable<List<User>> findByUid();

    @Query("select * from user where uid = :uid")
    Single<User> findByUid(String uid);
}
