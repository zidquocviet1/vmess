package com.mqv.realtimechatapplication.network.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.mqv.realtimechatapplication.network.model.Chat;

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

    public synchronized void connect() {
        this.canConnect = true;
        try {
            getWebSocket();
        } catch (WebSocketUnavailableException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public synchronized void disconnect() {
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

    private synchronized WebSocketConnection getWebSocket() throws WebSocketUnavailableException, InterruptedException {
        if (!canConnect || user == null) {
            throw new WebSocketUnavailableException();
        }

        if (webSocket == null || webSocket.isDead()) {
            webSocketDisposable.dispose();

            webSocket           = new WebSocketConnection(okHttpClient, gson);
            webSocketDisposable = new CompositeDisposable();

            Disposable disposable = webSocket.connect(user)
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
        } catch (IOException | InterruptedException e) {
            return Single.error(e);
        }
    }

    public void sendPingMessage() {
        try {
            getWebSocket().sendPingMessage();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public WebSocketResponse readMessage(long timeoutMillis)
            throws IOException, TimeoutException, InterruptedException {
        while (true) {
            WebSocketRequestMessage  request  = getWebSocket().readRequest(timeoutMillis);
            WebSocketResponseMessage response = createWebSocketResponse(request);

            try {
                // Decrypt the request message and return the readable message
                if (isServiceMessage(request)) {
                    return new WebSocketResponse(HttpURLConnection.HTTP_OK, request.getBody());
                } else if (isAcceptedMessage(request) || isSeenMessage(request)) {
                    return new WebSocketResponse(HttpURLConnection.HTTP_ACCEPTED, request.getBody());
                } else if (isEmptyMessage(request)) {
                    return null;
                }
            } finally {
                getWebSocket().sendResponse(response);
            }
        }
    }

    private static boolean isServiceMessage(WebSocketRequestMessage request) {
        return request.getStatus() == WebSocketRequestMessage.Status.INCOMING_MESSAGE;
    }

    private static boolean isAcceptedMessage(WebSocketRequestMessage request) {
        return request.getStatus() == WebSocketRequestMessage.Status.ACCEPTED_MESSAGE;
    }

    private static boolean isSeenMessage(WebSocketRequestMessage request) {
        return request.getStatus() == WebSocketRequestMessage.Status.SEEN_MESSAGE;
    }

    private static boolean isEmptyMessage(WebSocketRequestMessage request) {
        return request.getStatus() == WebSocketRequestMessage.Status.UNKNOWN;
    }

    private static WebSocketResponseMessage createWebSocketResponse(WebSocketRequestMessage request) {
        if (isServiceMessage(request)) {
            return new WebSocketResponseMessage(request.getId(),
                                                HttpURLConnection.HTTP_ACCEPTED,
                                                "Accepted",
                                                request.getBody());
        } else if (isAcceptedMessage(request) || isSeenMessage(request)) {
            return new WebSocketResponseMessage(request.getId(),
                                                HttpURLConnection.HTTP_NO_CONTENT,
                                                "No content",
                                                request.getBody());
        } else {
            return new WebSocketResponseMessage(request.getId(),
                                                HttpURLConnection.HTTP_BAD_REQUEST,
                                                "Empty",
                                                new Chat());
        }
    }
}
