package com.mqv.vmess.dependencies;

import androidx.annotation.MainThread;

import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.manager.MemoryManager;
import com.mqv.vmess.message.IncomingMessageObserver;
import com.mqv.vmess.message.IncomingMessageProcessor;
import com.mqv.vmess.message.MessageSenderProcessor;
import com.mqv.vmess.network.websocket.WebSocketClient;
import com.mqv.vmess.notification.NotificationEntry;

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
    private static volatile NotificationEntry notificationEntry;
    private static volatile MemoryManager memoryManager;

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

    public static NotificationEntry getNotificationEntry() {
        if (notificationEntry == null) {
            synchronized (LOCK) {
                if (notificationEntry == null) {
                    notificationEntry = provider.provideNotificationEntry();
                }
            }
        }
        return notificationEntry;
    }

    public static MemoryManager getMemoryManager() {
        if (memoryManager == null) {
            synchronized (LOCK) {
                if (memoryManager == null) {
                    memoryManager = new MemoryManager();
                }
            }
        }
        return memoryManager;
    }

    public interface Provider {
        WebSocketClient provideWebSocket();
        IncomingMessageObserver provideIncomingMessageObserver();
        IncomingMessageProcessor provideIncomingMessageProcessor();
        DatabaseObserver provideDatabaseObserver();
        MessageSenderProcessor provideMessageSenderProcessor();
        NotificationEntry provideNotificationEntry();
    }
}
