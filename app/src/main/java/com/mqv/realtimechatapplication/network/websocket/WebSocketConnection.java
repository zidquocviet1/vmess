package com.mqv.realtimechatapplication.network.websocket;

import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.AUTHENTICATION_FAILED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.CONNECTED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.CONNECTING;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.DISCONNECTED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.DISCONNECTING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.gson.Gson;
import com.mqv.realtimechatapplication.BuildConfig;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketConnection extends WebSocketListener {
    private static final String TAG = WebSocketConnection.class.getSimpleName();
    private static final int    RESPONSE_PRESENCE_USER_LIST_MESSAGE = 205;

    private final String                                    name;
    private final String                                    wsUri;
    private final Gson                                      gson;
    private final OkHttpClient                              okHttpClient;
    private final WebSocketHeartbeatMonitor                 monitor;
    private final BehaviorSubject<WebSocketConnectionState> webSocketState;
    private final Map<Long, OutgoingRequest>                outgoingRequests = new HashMap<>();
    private final LinkedList<WebSocketRequestMessage>       incomingRequests = new LinkedList<>();
    private final Set<Long>                                 pingRequests     = new HashSet<>();

    private WebSocket    client;
    private FirebaseUser user;

    public WebSocketConnection(OkHttpClient okHttpClient,
                               Gson gson,
                               WebSocketHeartbeatMonitor monitor,
                               boolean isPresence) {
        String uri = BuildConfig.SERVER_URL.replace("https://", "wss://")
                                           .replace("http://", "ws://");

        if (isPresence) {
            this.wsUri = uri + "/v1/websocket/presence";
            this.name  = "presence";
        } else {
            this.wsUri = uri + "/v1/websocket";
            this.name  = "message";
        }
        this.gson           = gson;
        this.webSocketState = BehaviorSubject.createDefault(DISCONNECTED);
        this.okHttpClient   = okHttpClient;
        this.monitor        = monitor;
    }

    @WorkerThread
    public synchronized Observable<WebSocketConnectionState> connect(@NonNull FirebaseUser user) throws InterruptedException {
        log("connect()");

        if (client == null) {
            try {
                this.user = user;

                GetTokenResult result = Tasks.await(user.getIdToken(true));

                if (result == null) {
                    webSocketState.onNext(AUTHENTICATION_FAILED);
                } else {
                    String token       = result.getToken();
                    String bearerToken = Const.PREFIX_TOKEN + token;

                    Request.Builder requestBuilder = new Request.Builder()
                                                                .url(wsUri)
                                                                .addHeader(Const.AUTHORIZATION, bearerToken);

                    webSocketState.onNext(CONNECTING);

                    this.client = okHttpClient.newWebSocket(requestBuilder.build(), this);
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return webSocketState;
    }

    public synchronized void disconnect() {
        log("disconnect()");

        if (client != null) {
            client.close(1000, "OK");
            client = null;
            webSocketState.onNext(DISCONNECTING);
        }

        notifyAll();
    }

    public boolean isDead() {
        return this.client == null;
    }

    public synchronized WebSocketRequestMessage readRequest(long timeout) throws IOException, TimeoutException, InterruptedException {
        if (client == null) {
            throw new IOException("Connection is closed!");
        }

        // Make the thread wait for new message coming in.
        long startTime = System.currentTimeMillis();

        while (client != null && incomingRequests.isEmpty() && elapsedTime(startTime) < timeout) {
            wait(Math.max(1, timeout - elapsedTime(startTime)));
        }

        if (client == null) {
            throw new IOException("No connection!");
        } else if (incomingRequests.isEmpty()) {
            throw new TimeoutException("Timeout exceeded");
        } else {
            return incomingRequests.removeFirst();
        }
    }

    public synchronized Single<WebSocketResponse> sendRequest(WebSocketRequestMessage request) throws IOException {
        if (client == null) {
            throw new IOException("No connection!");
        }

        WebSocketMessage message                = new WebSocketMessage(WebSocketMessage.Type.REQUEST, request, null);
        SingleSubject<WebSocketResponse> single = SingleSubject.create();

        outgoingRequests.put(request.getId(), new OutgoingRequest(single));

        if (!client.send(gson.toJson(message))) {
            throw new IOException("Send failed!");
        }

        return single.observeOn(Schedulers.io())
                     .subscribeOn(Schedulers.io())
                     .timeout(10, TimeUnit.SECONDS, Schedulers.io());
    }

    public synchronized void sendResponse(WebSocketResponseMessage response) throws IOException {
        if (client == null) {
            throw new IOException("No connection!");
        }

        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.RESPONSE, null, response);

        if (!client.send(gson.toJson(message))) {
            throw new IOException("Send failed!");
        }
    }

    public synchronized void sendPingMessage() throws IOException {
        if (client != null) {
            log("Sending ping message...");

            long                    id   = System.currentTimeMillis();
            WebSocketRequestMessage ping = new WebSocketRequestMessage(id,
                                                                       WebSocketRequestMessage.Status.PING,
                                                                       new Chat(),
                                                                       user.getUid());
            WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.REQUEST, ping, null);

            pingRequests.add(id);

            if (!client.send(gson.toJson(message))) {
                throw new IOException("Send failed!");
            }
        }
    }

    public void notifyMessageError(WebSocketRequestMessage request) {
        monitor.onMessageError(request);
    }

    public List<String> getSeenMessagesNeedToPush() {
        return monitor.getSeenMessage()
                      .stream()
                      .map(c -> c.getBody().getId())
                      .collect(Collectors.toList());
    }

    @Override
    public synchronized void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        if (client != null) {
            log("onOpen() connected");
            webSocketState.onNext(CONNECTED);
        }
    }

    @Override
    public synchronized void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        log("onMessage(): " + text);

        WebSocketMessage message = gson.fromJson(text, WebSocketMessage.class);

        if (message.getType() == WebSocketMessage.Type.REQUEST) {
            //noinspection ConstantConditions
            if (message.getRequest().getStatus() == WebSocketRequestMessage.Status.UNKNOWN) {
                monitor.onUserPresence(message.getRequest());
            } else {
                incomingRequests.add(message.getRequest());
            }
        } else if (message.getType() == WebSocketMessage.Type.RESPONSE) {
            WebSocketResponseMessage response = message.getResponse();
            if (response != null) {
                OutgoingRequest emitter = outgoingRequests.remove(response.getId());
                if (emitter != null) {
                    emitter.onSuccess(new WebSocketResponse(response.getStatus(), response.getBody()));
                } else if (pingRequests.remove(response.getId())) {
                    Logging.debug(TAG, "Receive pong message, sent time: " + response.getId());
                    monitor.onKeepAliveResponse(response.getId());
                } else if (response.getStatus() == RESPONSE_PRESENCE_USER_LIST_MESSAGE) {
                    // noinspection unchecked
                    List<String> onlineUsers = gson.fromJson(response.getMessage(), List.class);

                    for (String onlineUser : onlineUsers) {
                        monitor.onUserPresence(new WebSocketRequestMessage(response.getId(), WebSocketRequestMessage.Status.UNKNOWN, new Chat(), onlineUser));
                    }
                }
            }
        }
        notifyAll();
    }

    @Override
    public synchronized void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        log("onClosed()");
        webSocketState.onNext(DISCONNECTED);

        cleanupAfterShutdown();
        notifyAll();
    }

    @Override
    public synchronized void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        log("onClosing()");
        webSocketState.onNext(DISCONNECTING);
        webSocket.close(1000, "OK");
    }

    @Override
    public synchronized void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        log("onFailure(): " + t);

        if (response != null && (response.code() == 401 || response.code() == 403)) {
            webSocketState.onNext(WebSocketConnectionState.AUTHENTICATION_FAILED);
        } else {
            webSocketState.onNext(WebSocketConnectionState.FAILED);
        }

        cleanupAfterShutdown();
        notifyAll();
    }

    private void cleanupAfterShutdown() {
        Iterator<Map.Entry<Long, OutgoingRequest>> iterator = outgoingRequests.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, OutgoingRequest> entry = iterator.next();
            entry.getValue().onError(new IOException("Closed unexpectedly!"));
            iterator.remove();
        }

        if (client != null) {
            log("Client not null when closed!");
            client.close(1000, "OK");
            client = null;
        }
    }

    private long elapsedTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private static class OutgoingRequest {
        private final SingleSubject<WebSocketResponse> singleResponse;

        public OutgoingRequest(SingleSubject<WebSocketResponse> singleResponse) {
            this.singleResponse = singleResponse;
        }

        public void onSuccess(WebSocketResponse response) {
            singleResponse.onSuccess(response);
        }

        public void onError(Throwable t) {
            singleResponse.onError(t);
        }
    }

    private void log(String message) {
        Logging.debug(TAG, "[" + name + "]: " + message);
    }
}
