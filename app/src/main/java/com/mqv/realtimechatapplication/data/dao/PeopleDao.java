package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mqv.realtimechatapplication.ui.data.People;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface PeopleDao {
    @Query("select * from people")
    Flowable<List<People>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(People people);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(List<People> peopleList);

    @Delete
    Completable delete(People people);

    @Query("delete from people")
    Completable deleteAll();
}
