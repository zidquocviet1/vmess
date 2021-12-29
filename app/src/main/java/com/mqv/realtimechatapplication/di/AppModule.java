package com.mqv.realtimechatapplication.di;

import com.google.gson.Gson;
import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;
import com.mqv.realtimechatapplication.work.UserUtil;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    @Provides
    @Singleton
    public UserUtil provideUserUtil(UserDao userDao, HistoryLoggedInUserDao historyLoggedInUserDao) {
        return new UserUtil(userDao, historyLoggedInUserDao);
    }

    @Singleton
    @Provides
    public WebSocketClient provideWebSocketClient(OkHttpClient client, Gson gson) {
        return new WebSocketClient(client, gson);
    }
}
