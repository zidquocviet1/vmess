package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.IdentityKeyModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface IdentityKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveIdentity(model: IdentityKeyModel): Completable

    @Query("SELECT identityKey FROM identity_key WHERE address = :address")
    fun getIdentityKey(address: String): Single<String>

    @Query("SELECT * FROM identity_key WHERE address = :address")
    fun getIdentityKeyModel(address: String): Single<IdentityKeyModel>

    @Query("DELETE FROM identity_key")
    fun removeAll(): Completable
}