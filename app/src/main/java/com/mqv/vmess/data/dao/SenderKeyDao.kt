package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.SenderKeyModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SenderKeyDao {
    @Query("SELECT * FROM sender_key WHERE address = :address AND distributionId = :distributionId")
    fun getSenderKey(address: String, distributionId: String): Single<SenderKeyModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSenderKey(model: SenderKeyModel): Completable
}