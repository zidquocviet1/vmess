package com.mqv.realtimechatapplication.dependencies;

import androidx.annotation.MainThread;

import com.mqv.realtimechatapplication.data.DatabaseObserver;
import com.mqv.realtimechatapplication.message.IncomingMessageObserver;
import com.mqv.realtimechatapplication.message.IncomingMessageProcessor;
import com.mqv.realtimechatapplication.message.MessageSenderProcessor;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;

/*
* Create a new Dependency class based on Service Locator Pattern
* Initialized when app have started before got any dependency
* */
public class AppDependencies {
    private static final Object LOCK = new Object();

    private static volatile WebSocketClient webSocket;
    private static volatile IncomingMessageObserver incomingMessageObserver;
    private static volatile IncomingMessageProcessor incomingMessageProcessor;
    private static volatile DatabaseObserver databaseObserver;
    private static volatile MessageSenderProcessor messageSenderProcessor;

    private static Provider provider;

    @MainThread
    public static void init(Provider provider) {
        synchronized (LOCK) {
            if (AppDependencies.provider != null) {
                throw new IllegalStateException("Already initialized");
            }
            AppDependencies.provider = provider;
        }
    }

    public static WebSocketClient getWebSocket() {
        if (webSocket == null) {
            synchronized (LOCK) {
                if (webSocket == null) {
                    webSocket = provider.provideWebSocket();
                }
            }
        }
        return webSocket;
    }

    public static IncomingMessageObserver getIncomingMessageObserver() {
        if (incomingMessageObserver == null) {
            synchronized (LOCK) {
                if (incomingMessageObserver == null) {
                    incomingMessageObserver = provider.provideIncomingMessageObserver();
                }
            }
        }
        return incomingMessageObserver;
    }

    public static IncomingMessageProcessor getIncomingMessageProcessor() {
        if (incomingMessageProcessor == null) {
            synchronized (LOCK) {
                if (incomingMessageProcessor == null) {
                    incomingMessageProcessor = provider.provideIncomingMessageProcessor();
                }
            }
        }
        return incomingMessageProcessor;
    }

    public static DatabaseObserver getDatabaseObserver() {
        if (databaseObserver == null) {
            synchronized (LOCK) {
                if (databaseObserver == null) {
                    databaseObserver = provider.provideDatabaseObserver();
                }
            }
        }
        return databaseObserver;
    }

    public static void closeAllConnection() {
        synchronized (LOCK) {
            if (incomingMessageObserver != null) {
                incomingMessageObserver.terminate();
            }

            incomingMessageObserver = null;
        }
    }

    public static MessageSenderProcessor getMessageSenderProcessor() {
        if (messageSenderProcessor == null) {
            synchronized (LOCK) {
                if (messageSenderProcessor == null) {
                    messageSenderProcessor = provider.provideMessageSenderProcessor();
                }
            }
        }
        return messageSenderProcessor;
    }

    public interface Provider {
        WebSocketClient provideWebSocket();
        IncomingMessageObserver provideIncomingMessageObserver();
        IncomingMessageProcessor provideIncomingMessageProcessor();
        DatabaseObserver provideDatabaseObserver();
        MessageSenderProcessor provideMessageSenderProcessor();
    }
}
