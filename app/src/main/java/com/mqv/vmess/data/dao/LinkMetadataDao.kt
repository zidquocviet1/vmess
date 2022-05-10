package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.LinkMetadata
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface LinkMetadataDao {
    @Query("SELECT * FROM link_metadata WHERE id = :id")
    fun getById(id: String): Single<LinkMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(link: LinkMetadata): Completable
}