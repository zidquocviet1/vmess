package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable saveAll(List<Conversation> data);

    @Query("select * from Conversation where status = :status")
    Flowable<List<Conversation>> fetchAllByStatus(ConversationStatusType status);

    @Update
    Completable update(Conversation conversation);

    @Query("delete from Conversation")
    Completable deleteAll();

    @Query("delete from Conversation where id not in (:conversationListId)")
    Completable deleteAll(List<String> conversationListId);
}
