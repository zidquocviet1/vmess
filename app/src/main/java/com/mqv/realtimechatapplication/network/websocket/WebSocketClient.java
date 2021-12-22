package com.mqv.realtimechatapplication.network.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;

public class WebSocketClient {
    private final OkHttpClient  okHttpClient;
    private final Gson          gson;
    private final FirebaseUser  user;

    private WebSocketConnection webSocket;
    private CompositeDisposable webSocketDisposable;
    private final BehaviorSubject<WebSocketConnectionState> webSocketState;

    private String  conversationId;
    private boolean canConnect;

    public WebSocketClient(OkHttpClient httpClient, Gson gson) {
        this.okHttpClient           = httpClient;
        this.gson                   = gson;
        this.user                   = FirebaseAuth.getInstance().getCurrentUser();
        this.webSocketState         = BehaviorSubject.createDefault(WebSocketConnectionState.DISCONNECTED);
        this.webSocketDisposable    = new CompositeDisposable();
    }

    public Observable<WebSocketConnectionState> getWebSocketState() {
        return webSocketState;
    }

    public void connect(String conversationId) {
        this.canConnect     = true;
        this.conversationId = conversationId;
        try {
            getWebSocket();
        } catch (WebSocketUnavailableException e) {
            throw new AssertionError(e);
        }
    }

    public void disconnect() {
        canConnect = false;

        if (webSocket != null) {
            webSocketDisposable.dispose();
            webSocket.disconnect();
            webSocket = null;

            if (!webSocketState.getValue().isFailure()) {
                webSocketState.onNext(WebSocketConnectionState.DISCONNECTED);
            }
        }
    }

    private WebSocketConnection getWebSocket() throws WebSocketUnavailableException {
        if (!canConnect || user == null) {
            throw new WebSocketUnavailableException();
        }

        if (webSocket == null || webSocket.isDead()) {
            webSocketDisposable.dispose();

            webSocket           = new WebSocketConnection(okHttpClient, gson);
            webSocketDisposable = new CompositeDisposable();

            Disposable disposable = webSocket.connect(user, conversationId)
                                             .subscribeOn(Schedulers.computation())
                                             .observeOn(Schedulers.computation())
                                             .subscribe(webSocketState::onNext);
            webSocketDisposable.add(disposable);
        }
        return webSocket;
    }

    public Single<WebSocketResponse> sendRequest(WebSocketRequestMessage requestMessage) {
        try {
            return getWebSocket().sendRequest(requestMessage);
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    public WebSocketResponse readMessage(long timeoutMillis) throws IOException, TimeoutException, InterruptedException {
        WebSocketRequestMessage  request  = getWebSocket().readRequest(timeoutMillis);
        WebSocketResponseMessage response = createWebSocketResponse(request);

        try {
            // Decrypt the request message and return the readable message
            return new WebSocketResponse(HttpURLConnection.HTTP_OK, request.getBody());
        } finally {
            // Acknowledge the request message
            getWebSocket().sendResponse(response);
        }
    }

    private WebSocketResponseMessage createWebSocketResponse(WebSocketRequestMessage request) {
        return new WebSocketResponseMessage(request.getId(), HttpURLConnection.HTTP_OK, "OK", request.getBody());
    }
}
