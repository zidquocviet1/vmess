package com.mqv.realtimechatapplication.dependencies;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.mqv.realtimechatapplication.data.DatabaseObserver;
import com.mqv.realtimechatapplication.data.MyDatabase;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.impl.ConversationRepositoryImpl;
import com.mqv.realtimechatapplication.message.IncomingMessageObserver;
import com.mqv.realtimechatapplication.message.IncomingMessageProcessor;
import com.mqv.realtimechatapplication.message.MessageSenderProcessor;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.network.websocket.WebSocketAlarmTimer;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;
import com.mqv.realtimechatapplication.network.websocket.WebSocketConnection;
import com.mqv.realtimechatapplication.network.websocket.WebSocketFactory;
import com.mqv.realtimechatapplication.network.websocket.WebSocketHeartbeatMonitor;

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
        WebSocketAlarmTimer       timer     = new WebSocketAlarmTimer();
        WebSocketHeartbeatMonitor monitor   = new WebSocketHeartbeatMonitor(timer);
        WebSocketClient           webSocket = new WebSocketClient(provideWebSocketFactory(monitor));

        monitor.monitor(webSocket);

        return webSocket;
    }

    @Override
    public IncomingMessageObserver provideIncomingMessageObserver() {
        return new IncomingMessageObserver(context);
    }

    @Override
    public IncomingMessageProcessor provideIncomingMessageProcessor() {
        ConversationRepository conversationRepository = new ConversationRepositoryImpl(retrofit.create(ConversationService.class),
                                                                                       database.getConversationDao(),
                                                                                       database.getChatDao());
        return new IncomingMessageProcessor(database.getChatDao(),
                                            database.getConversationDao(),
                                            conversationRepository);
    }

    @Override
    public DatabaseObserver provideDatabaseObserver() {
        return new DatabaseObserver();
    }

    @Override
    public MessageSenderProcessor provideMessageSenderProcessor() {
        return new MessageSenderProcessor(context,
                                          database.getPendingMessageDao(),
                                          database.getChatDao(),
                                          database.getSeenMessageDao());
    }

    private WebSocketFactory provideWebSocketFactory(WebSocketHeartbeatMonitor monitor) {
        return new WebSocketFactory() {
            @NonNull
            @Override
            public WebSocketConnection createWebSocket() {
                return new WebSocketConnection(okHttpClient, gson, monitor, false);
            }

            @NonNull
            @Override
            public WebSocketConnection createPresenceWebSocket() {
                return new WebSocketConnection(okHttpClient, gson, monitor, true);
            }
        };
    }
}
