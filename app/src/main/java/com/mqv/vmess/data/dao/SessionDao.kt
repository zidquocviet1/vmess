package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.SessionModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface SessionDao {
    @Query(
        " SELECT record" +
                " FROM sessions" +
                " WHERE userId = :userId" +
                " AND remoteAddress = :remoteAddress" +
                " AND deviceId = :deviceId"
    )
    fun getSessionRecord(userId: String, remoteAddress: String, deviceId: Int): Single<String>

    @Query("SELECT deviceId FROM sessions WHERE userId = :userId AND remoteAddress = :remoteAddress AND deviceId != :defaultDeviceId")
    fun getSubDevicesSession(userId: String, remoteAddress: String, defaultDeviceId: Int): Single<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSession(model: SessionModel): Completable

    @Query("DELETE FROM sessions WHERE userId = :userId AND remoteAddress = :remoteAddress AND deviceId = :deviceId")
    fun deleteSession(userId: String, remoteAddress: String, deviceId: Int): Completable

    @Query("DELETE FROM sessions WHERE userId = :userId AND remoteAddress = :remoteAddress")
    fun deleteAllSessionByRemoteAddress(userId: String, remoteAddress: String): Completable

    @Query("DELETE FROM sessions")
    fun removeAll(): Completable
}