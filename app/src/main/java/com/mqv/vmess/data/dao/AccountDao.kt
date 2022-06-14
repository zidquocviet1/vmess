package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.signal.libsignal.protocol.IdentityKeyPair

@Dao
interface AccountDao {
    @Query(
        "INSERT OR REPLACE INTO accounts" +
                " VALUES(:id," +
                " :registrationId," +
                " (SELECT identityKeyPair FROM accounts WHERE id = :id))"
    )
    fun setRegistrationId(id: String, registrationId: Int): Completable

    @Query(
        "INSERT OR REPLACE INTO accounts" +
                " VALUES (:id," +
                " (SELECT registrationId FROM accounts WHERE id = :id)," +
                " :identityKeyPair)"
    )
    fun setIdentityKeyPair(id: String, identityKeyPair: IdentityKeyPair): Completable

    @Query("SELECT registrationId FROM accounts WHERE id = :id")
    fun getRegistrationId(id: String): Single<Int>

    @Query("SELECT identityKeyPair FROM accounts WHERE id = :id")
    fun getIdentityKeyPair(id: String): Single<IdentityKeyPair>
}