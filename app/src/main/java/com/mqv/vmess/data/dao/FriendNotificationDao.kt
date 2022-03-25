package com.mqv.vmess.data.dao

import androidx.room.*
import com.mqv.vmess.data.model.FriendNotification
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

@Dao
interface FriendNotificationDao {
    @Query("SELECT * FROM friend_notification WHERE id = :id")
    fun fetchById(id: Long): Single<FriendNotification>

    @Query("SELECT * FROM friend_notification WHERE sender_id = :userId AND type = 'REQUEST_FRIEND'")
    fun fetchRequestNotificationByUserId(userId: String): Single<FriendNotification>

    @Query("SELECT * FROM friend_notification WHERE sender_id = :userId AND type = 'ACCEPTED_FRIEND'")
    fun fetchAcceptedNotificationByUserId(userId: String): Single<FriendNotification>

    @Query("SELECT * FROM friend_notification WHERE sender_id = :userId")
    fun fetchAllNotificationRelatedToUser(userId: String): Single<List<FriendNotification>>

    @Query("SELECT * FROM friend_notification")
    fun fetchAll(): Flowable<List<FriendNotification>>

    @Query("SELECT count(*) FROM friend_notification WHERE has_read = 0")
    fun fetchUnreadNotification(): Flowable<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAndReturn(item: FriendNotification): Single<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: FriendNotification): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: List<FriendNotification>): Completable

    @Query("delete from friend_notification")
    fun deleteAll(): Completable

    @Query("delete from friend_notification where id not in (:listId)")
    fun deleteById(listId: List<Long>): Completable

    @Delete
    fun delete(notification: FriendNotification): Completable

    @Delete
    fun delete(notifications: List<FriendNotification>): Completable

    @Update
    fun update(notification: FriendNotification): Completable
}