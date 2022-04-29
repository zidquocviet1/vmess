package com.mqv.vmess.data.dao

import androidx.room.*
import com.mqv.vmess.data.model.RecentSearchPeople
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

@Dao
interface RecentSearchDao {
    @Query("select * from recent_search_people")
    fun fetchAll(): Single<List<RecentSearchPeople>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(people: RecentSearchPeople): Completable

    @Query("delete from recent_search_people")
    fun deleteAll(): Completable

    @Delete
    fun delete(people: RecentSearchPeople): Completable
}