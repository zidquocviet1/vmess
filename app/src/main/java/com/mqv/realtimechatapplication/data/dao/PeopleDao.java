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
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PeopleDao {
    @Query("select * from people")
    Flowable<List<People>> getAll();

    @Query("select * from people limit 30")
    Single<List<People>> getSuggestionList();

    @Query("select * from people where uid = :uid")
    Single<People> getByUid(String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(People people);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable save(List<People> peopleList);

    @Delete
    Completable delete(People people);

    @Query("delete from people")
    Completable deleteAll();
}
