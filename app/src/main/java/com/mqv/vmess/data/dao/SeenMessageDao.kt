package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.SeenMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SeenMessageDao {
    @Query(value = "SELECT * FROM seen_message")
    fun getAll(): Single<List<SeenMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: SeenMessage): Completable

    @Query(value = "DELETE FROM seen_message WHERE id = :id")
    fun delete(id: String): Completable
}