package com.mqv.realtimechatapplication.network.websocket;

import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.AUTHENTICATION_FAILED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.CONNECTED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.CONNECTING;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.DISCONNECTED;
import static com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState.DISCONNECTING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.mqv.realtimechatapplication.BuildConfig;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private final String                                    wsUri;
    private final Gson                                      gson;
    private final OkHttpClient                              okHttpClient;
    private final BehaviorSubject<WebSocketConnectionState> webSocketState;
    private final Map<Long, OutgoingRequest>                outgoingRequests = new HashMap<>();
    private final LinkedList<WebSocketRequestMessage>       incomingRequests = new LinkedList<>();

    private WebSocket client;

    public WebSocketConnection(OkHttpClient okHttpClient, Gson gson) {
        String uri = BuildConfig.SERVER_URL.replace("https://", "wss://")
                                           .replace("http://", "ws://");

        this.wsUri          = uri + "/v1/websocket";
        this.gson           = gson;
        this.webSocketState = BehaviorSubject.createDefault(DISCONNECTED);
        this.okHttpClient   = okHttpClient;
    }

    public synchronized Observable<WebSocketConnectionState> connect(@NonNull FirebaseUser user) throws InterruptedException {
        Logging.debug(TAG, "connect()");

        if (client == null) {
            user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    if (result == null) {
                        webSocketState.onNext(AUTHENTICATION_FAILED);
                    } else {
                        String token        = result.getToken();
                        String bearerToken  = Const.PREFIX_TOKEN + token;

                        Request.Builder requestBuilder = new Request.Builder()
                                                                    .url(wsUri)
                                                                    .addHeader(HttpHeaders.AUTHORIZATION, bearerToken);

                        webSocketState.onNext(CONNECTING);

                        this.client = okHttpClient.newWebSocket(requestBuilder.build(), this);
                    }
                });
        }
        return webSocketState;
    }

    public synchronized void disconnect() {
        Logging.debug(TAG, "disconnect()");

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

    public synchronized Single<WebSocketResponse> sendRequest(WebSocketRequestMessage request) throws IOException{
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
            Logging.debug(TAG, "Sending ping message...");
            long id = System.currentTimeMillis();

            if (!client.send(id + " hello")) {
                throw new IOException("Send failed!");
            }
        }
    }

    @Override
    public synchronized void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        if (client != null) {
            Logging.debug(TAG, "onOpen() connected");
            webSocketState.onNext(CONNECTED);
        }
    }

    @Override
    public synchronized void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Logging.debug(TAG, "onMessage(): " + text);

        WebSocketMessage message = gson.fromJson(text, WebSocketMessage.class);

        if (message.getType() == WebSocketMessage.Type.REQUEST) {
            incomingRequests.add(message.getRequest());
        } else if (message.getType() == WebSocketMessage.Type.RESPONSE) {
            WebSocketResponseMessage response = message.getResponse();
            if (response != null) {
                OutgoingRequest emitter = outgoingRequests.remove(response.getId());
                if (emitter != null) {
                    emitter.onSuccess(new WebSocketResponse(response.getStatus(), response.getBody()));
                }
            }
        }
        notifyAll();
    }

    @Override
    public synchronized void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Logging.debug(TAG, "onClosed()");
        webSocketState.onNext(DISCONNECTED);

        cleanupAfterShutdown();
        notifyAll();
    }

    @Override
    public synchronized void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Logging.debug(TAG, "onClosing()");
        webSocketState.onNext(DISCONNECTING);
        webSocket.close(1000, "OK");
    }

    @Override
    public synchronized void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        Logging.debug(TAG, "onFailure(): " + t);

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
            Logging.debug(TAG, "Client not null when closed!");
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
}
