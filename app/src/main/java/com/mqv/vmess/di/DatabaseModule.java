package com.mqv.vmess.di;


import android.content.Context;

import androidx.room.Room;

import com.mqv.vmess.data.MyDatabase;
import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.data.dao.ConversationDao;
import com.mqv.vmess.data.dao.HistoryLoggedInUserDao;
import com.mqv.vmess.data.dao.NotificationDao;
import com.mqv.vmess.data.dao.PeopleDao;
import com.mqv.vmess.data.dao.UserDao;
import com.mqv.vmess.util.Const;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class DatabaseModule {
    @Singleton
    @Provides
    public MyDatabase provideDatabase(@ApplicationContext Context context){
        return Room.databaseBuilder(context, MyDatabase.class, Const.DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public UserDao provideUserDao(MyDatabase db){
        return db.getUserDao();
    }

    @Provides
    @Singleton
    public HistoryLoggedInUserDao provideHistoryUserDao(MyDatabase db){
        return db.getHistoryUserDao();
    }

    @Provides
    @Singleton
    public PeopleDao providePeopleDao(MyDatabase db){
        return db.getPeopleDao();
    }

    @Provides
    @Singleton
    public NotificationDao provideNotificationDao(MyDatabase db){
        return db.getNotificationDao();
    }

    @Provides
    @Singleton
    public ConversationDao provideConversationDao(MyDatabase db){
        return db.getConversationDao();
    }

    @Provides
    @Singleton
    public ChatDao provideChatDao(MyDatabase db){
        return db.getChatDao();
    }
}
