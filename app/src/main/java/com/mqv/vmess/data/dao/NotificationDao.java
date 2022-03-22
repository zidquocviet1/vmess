package com.mqv.vmess.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mqv.vmess.network.model.Notification;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface NotificationDao {
    @Query("Select * from notification")
    Flowable<List<Notification>> fetchAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(List<Notification> notifications);

    @Query("delete from notification")
    Completable deleteAll();

    @Query("delete from notification where id not in (:listId)")
    Completable deleteById(List<Long> listId);

    @Delete
    Completable delete(Notification notification);

    @Update
    Completable update(Notification notification);
}