package com.mqv.realtimechatapplication.di;

import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.work.UserUtil;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    @Provides
    @Singleton
    public UserUtil provideUserUtil(UserDao userDao, HistoryLoggedInUserDao historyLoggedInUserDao) {
        return new UserUtil(userDao, historyLoggedInUserDao);
    }
}
