package com.mqv.vmess.dependencies;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.AppPreferencesImpl;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.data.MyDatabase;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.impl.ConversationRepositoryImpl;
import com.mqv.vmess.message.IncomingMessageObserver;
import com.mqv.vmess.message.IncomingMessageProcessor;
import com.mqv.vmess.message.MessageSenderProcessor;
import com.mqv.vmess.network.service.ChatService;
import com.mqv.vmess.network.service.ConversationService;
import com.mqv.vmess.network.service.FriendRequestService;
import com.mqv.vmess.network.service.RtcService;
import com.mqv.vmess.network.service.UserService;
import com.mqv.vmess.network.websocket.WebSocketAlarmTimer;
import com.mqv.vmess.network.websocket.WebSocketClient;
import com.mqv.vmess.network.websocket.WebSocketConnection;
import com.mqv.vmess.network.websocket.WebSocketFactory;
import com.mqv.vmess.network.websocket.WebSocketHeartbeatMonitor;
import com.mqv.vmess.notification.NotificationEntry;
import com.mqv.vmess.notification.NotificationHandler;
import com.mqv.vmess.webrtc.WebRtcCallManager;

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
                                                                                       database.getConversationOptionDao(),
                                                                                       database.getConversationColorDao(),
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
                                          database.getSeenMessageDao(),
                                          retrofit.create(ChatService.class));
    }

    @Override
    public NotificationEntry provideNotificationEntry() {
        return new NotificationHandler(context,
                                       database,
                                       retrofit.create(ConversationService.class),
                                       retrofit.create(ChatService.class),
                                       retrofit.create(UserService.class),
                                       retrofit.create(FriendRequestService.class),
                                       retrofit.create(RtcService.class),
                                       gson);
    }

    @Override
    public AppPreferences provideAppPreferences() {
        return new AppPreferencesImpl(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    @Override
    public WebRtcCallManager provideWebRtcCallManager() {
        return new WebRtcCallManager(context);
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
