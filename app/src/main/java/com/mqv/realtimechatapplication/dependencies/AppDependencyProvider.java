package com.mqv.realtimechatapplication.dependencies;

import android.content.Context;

import com.google.gson.Gson;
import com.mqv.realtimechatapplication.data.DatabaseObserver;
import com.mqv.realtimechatapplication.data.MyDatabase;
import com.mqv.realtimechatapplication.message.IncomingMessageObserver;
import com.mqv.realtimechatapplication.message.IncomingMessageProcessor;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/*
* Implementation of {@link AppDependencies.Provider} to provide the app dependency
* */
public class AppDependencyProvider implements AppDependencies.Provider {
    private final Context      context;
    private final MyDatabase   database;
    private final Retrofit     retrofit;
    private final OkHttpClient okHttpClient;
    private final Gson         gson;

    public AppDependencyProvider(Context context,
                                 MyDatabase database,
                                 Retrofit retrofit,
                                 OkHttpClient okHttpClient,
                                 Gson gson) {
        this.context      = context;
        this.database     = database;
        this.retrofit     = retrofit;
        this.okHttpClient = okHttpClient;
        this.gson         = gson;
    }

    @Override
    public WebSocketClient provideWebSocket() {
        return new WebSocketClient(okHttpClient, gson);
    }

    @Override
    public IncomingMessageObserver provideIncomingMessageObserver() {
        return new IncomingMessageObserver(context);
    }

    @Override
    public IncomingMessageProcessor provideIncomingMessageProcessor() {
        return new IncomingMessageProcessor(database.getChatDao(),
                                            database.getConversationDao(),
                                            retrofit.create(ConversationService.class));
    }

    @Override
    public DatabaseObserver provideDatabaseObserver() {
        return new DatabaseObserver();
    }
}
