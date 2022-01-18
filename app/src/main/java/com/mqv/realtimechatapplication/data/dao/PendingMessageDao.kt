package com.mqv.realtimechatapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.realtimechatapplication.data.model.PendingMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface PendingMessageDao {
    @Query(value = "SELECT * FROM pending_message")
    fun getAll(): Single<List<PendingMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: PendingMessage): Completable

    @Query(value = "DELETE FROM pending_message WHERE id = :id")
    fun delete(id: String): Completable
}