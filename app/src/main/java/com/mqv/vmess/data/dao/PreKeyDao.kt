package com.mqv.vmess.data.dao

import androidx.room.*
import com.mqv.vmess.data.model.PreKeyModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface PreKeyDao {
    @Query("SELECT * FROM pre_key WHERE keyId = :preKeyId and userId = :userId")
    fun getPreKey(preKeyId: Int, userId: String): Single<PreKeyModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePreKey(preKeyModel: PreKeyModel): Completable

    @Query(
        "SELECT EXISTS(SELECT *" +
                "      FROM pre_key " +
                "      WHERE keyId = :preKeyId " +
                "      AND userId = :userId " +
                "      LIMIT 1)"
    )
    fun containsPreKey(preKeyId: Int, userId: String): Single<Boolean>

    @Query("DELETE FROM pre_key WHERE keyId = :preKeyId AND userId = :userId")
    fun removePreKey(preKeyId: Int, userId: String): Completable

    @Query("DELETE FROM pre_key")
    fun removeAll(): Completable
}