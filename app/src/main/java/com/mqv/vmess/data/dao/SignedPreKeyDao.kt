package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.SignedPreKeyModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SignedPreKeyDao {
    @Query("SELECT * FROM signed_pre_key WHERE userId = :userId")
    fun getAllSignedPreKeys(userId: String): Single<MutableList<SignedPreKeyModel>>

    @Query("SELECT * FROM signed_pre_key WHERE keyId = :signedPreKeyId and userId = :userId")
    fun getSignedPreKey(signedPreKeyId: Int, userId: String): Single<SignedPreKeyModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignedPreKey(signedPreKeyModel: SignedPreKeyModel): Completable

    @Query(
        "SELECT EXISTS(SELECT *" +
                "      FROM signed_pre_key " +
                "      WHERE keyId = :signedPreKeyId " +
                "      AND userId = :userId " +
                "      LIMIT 1)"
    )
    fun containsSignedPreKey(signedPreKeyId: Int, userId: String): Single<Boolean>

    @Query("DELETE FROM signed_pre_key WHERE keyId = :signedPreKeyId AND userId = :userId")
    fun removeSignedPreKey(signedPreKeyId: Int, userId: String): Completable
}