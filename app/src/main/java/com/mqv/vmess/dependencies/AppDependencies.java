package com.mqv.vmess.dependencies;

import androidx.annotation.MainThread;

import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.crypto.storage.LocalStorageSessionStore;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.manager.MemoryManager;
import com.mqv.vmess.message.IncomingMessageObserver;
import com.mqv.vmess.message.IncomingMessageProcessor;
import com.mqv.vmess.message.MessageBuilder;
import com.mqv.vmess.message.MessageSenderProcessor;
import com.mqv.vmess.network.websocket.WebSocketClient;
import com.mqv.vmess.notification.NotificationEntry;
import com.mqv.vmess.webrtc.WebRtcCallManager;

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
    private static volatile AppPreferences appPreferences;
    private static volatile WebRtcCallManager webRtcCallManager;
    private static volatile LocalStorageSessionStore localStorageSessionStore;
    private static volatile MessageBuilder messageBuilder;

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

    public static AppPreferences getAppPreferences() {
        if (appPreferences == null) {
            synchronized (LOCK) {
                if (appPreferences == null) {
                    appPreferences = provider.provideAppPreferences();
                }
            }
        }
        return appPreferences;
    }

    public static WebRtcCallManager getWebRtcCallManager() {
        if (webRtcCallManager == null) {
            synchronized (LOCK) {
                if (webRtcCallManager == null) {
                    webRtcCallManager = provider.provideWebRtcCallManager();
                }
            }
        }
        return webRtcCallManager;
    }

    public static LocalStorageSessionStore getLocalStorageSessionStore() {
        if (localStorageSessionStore == null) {
            synchronized (LOCK) {
                if (localStorageSessionStore == null) {
                    localStorageSessionStore = provider.provideLocalStorageSessionStore();
                }
            }
        }
        return localStorageSessionStore;
    }

    public static MessageBuilder getMessageBuilder() {
        if (messageBuilder == null) {
            synchronized (LOCK) {
                if (messageBuilder == null) {
                    messageBuilder = provider.provideMessageBuilder();
                }
            }
        }
        return messageBuilder;
    }

    public interface Provider {
        WebSocketClient provideWebSocket();
        IncomingMessageObserver provideIncomingMessageObserver();
        IncomingMessageProcessor provideIncomingMessageProcessor();
        DatabaseObserver provideDatabaseObserver();
        MessageSenderProcessor provideMessageSenderProcessor();
        NotificationEntry provideNotificationEntry();
        AppPreferences provideAppPreferences();
        WebRtcCallManager provideWebRtcCallManager();
        LocalStorageSessionStore provideLocalStorageSessionStore();
        MessageBuilder provideMessageBuilder();
    }
}
