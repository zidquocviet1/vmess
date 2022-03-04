package com.mqv.realtimechatapplication.message;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.network.NetworkConstraint;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;
import com.mqv.realtimechatapplication.network.websocket.WebSocketResponse;
import com.mqv.realtimechatapplication.network.websocket.WebSocketUnavailableException;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.work.LifecycleUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/*
* Class for observing all of the messages from websocket: request, response, ping, pong
* */
public class IncomingMessageObserver {
    private static final String TAG                     = IncomingMessageObserver.class.getSimpleName();
    private static final long   READ_REQUEST_TIMEOUT    = 1;

    private static final AtomicInteger INSTANCE = new AtomicInteger(0);

    private final Context           context;
    private final BroadcastReceiver networkConnectionReceiver;

    private boolean terminated;

    public IncomingMessageObserver(Context context) {
        // Make sure only one instance running at the same time
        if (INSTANCE.incrementAndGet() != 1) {
            throw new AssertionError("Multiple instances");
        }

        this.context = context;

        new MessageRetriever().start();

        networkConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (IncomingMessageObserver.this) {
                    if (!NetworkConstraint.isMet(context)) {
                        Logging.debug(TAG, "Lost network need to shutdown our webSocket and reset state");
                        disconnect();
                    }
                    // When the network connection changed, we need to notifyAll to all threads to connect again.
                    IncomingMessageObserver.this.notifyAll();
                }
            }
        };
        context.registerReceiver(networkConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private synchronized void waitForConnectionNecessary() {
        while (!isConnectionNecessary()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private boolean isConnectionNecessary() {
        boolean registered = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean hasNetwork = NetworkConstraint.isMet(context);
        boolean isVisible  = LifecycleUtil.isAppForeground();

        Logging.debug(TAG, String.format("The connection details: isRegistered: %s, hasNetwork: %s, isVisible: %s",
                registered, hasNetwork, isVisible));

        return registered && hasNetwork;
    }

    private void disconnect() {
        AppDependencies.getWebSocket().disconnect();
    }

    public void terminate() {
        INSTANCE.decrementAndGet();

        context.unregisterReceiver(networkConnectionReceiver);

        Executors.newSingleThreadExecutor().execute(() -> {
            terminated = true;
            disconnect();
        });
    }

    private class MessageRetriever extends Thread {
        @Override
        public void run() {
            while (!terminated) {
                Logging.debug(TAG, "Waiting for websocket state change....");
                waitForConnectionNecessary();

                Logging.debug(TAG, "Making websocket connection...");
                WebSocketClient webSocket = AppDependencies.getWebSocket();
                webSocket.connect();

                try {
                    while (isConnectionNecessary()) {
                        try {
                            Logging.debug(TAG, "Reading message....");
                            WebSocketResponse message = webSocket.readMessage(TimeUnit.MINUTES.toMillis(READ_REQUEST_TIMEOUT));
                            AppDependencies.getIncomingMessageProcessor().process(message);
                        } catch (WebSocketUnavailableException e) {
                            Logging.debug(TAG, "Pipe unexpectedly unavailable, connecting");
                            webSocket.connect();
                        } catch (TimeoutException e) {
                            Logging.debug(TAG, "Application level read timeout...");
                        }
                    }
                } catch (Throwable e) {
                    Logging.debug(TAG, e.getMessage());
                } finally {
                    Logging.debug(TAG, "Shutting down pipe...");
                    disconnect();
                }
            }
        }
    }
}
