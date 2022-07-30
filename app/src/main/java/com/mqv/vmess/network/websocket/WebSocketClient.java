package com.mqv.vmess.network.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.network.model.Chat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class WebSocketClient {
    private WebSocketConnection webSocket;
    private WebSocketConnection presenceWebSocket;
    private CompositeDisposable presenceWebSocketDisposable;
    private CompositeDisposable webSocketDisposable;

    private final WebSocketFactory                          webSocketFactory;
    private final BehaviorSubject<WebSocketConnectionState> webSocketState;
    private final BehaviorSubject<WebSocketConnectionState> presenceWebSocketState;
    private final BehaviorSubject<List<String>>             presenceUserList;

    private boolean canConnect;

    public WebSocketClient(WebSocketFactory webSocketFactory) {
        this.webSocketFactory            = webSocketFactory;
        this.webSocketState              = BehaviorSubject.createDefault(WebSocketConnectionState.DISCONNECTED);
        this.presenceWebSocketState      = BehaviorSubject.createDefault(WebSocketConnectionState.DISCONNECTED);
        this.presenceUserList            = BehaviorSubject.createDefault(Collections.emptyList());
        this.presenceWebSocketDisposable = new CompositeDisposable();
        this.webSocketDisposable         = new CompositeDisposable();
    }

    public Observable<WebSocketConnectionState> getWebSocketState() {
        return webSocketState;
    }

    public Observable<WebSocketConnectionState> getPresenceWebSocketState() {
        return presenceWebSocketState;
    }

    public Observable<List<String>> getPresenceUserList() {
        //noinspection ResultOfMethodCallIgnored
        getPresenceWebSocketState().observeOn(Schedulers.io())
                                   .subscribeOn(Schedulers.io())
                                   .onErrorComplete()
                                   .subscribe(s -> {
                                       if (s != WebSocketConnectionState.CONNECTED) {
                                           presenceUserList.onNext(Collections.emptyList());
                                       }
                                   });
        return presenceUserList;
    }

    public void postPresenceValue(String uid, boolean isOnline) {
        List<String> presence = new ArrayList<>(presenceUserList.getValue());

        if (isOnline) {
            if (!presence.contains(uid)) {
                presence.add(uid);
            }
        } else {
            presence.remove(uid);
        }

        presenceUserList.onNext(presence);
    }

    public synchronized void connect() {
        this.canConnect = true;
        try {
            getPresenceWebSocket();
            getWebSocket();
        } catch (WebSocketUnavailableException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public synchronized void disconnect() {
        canConnect = false;

        disconnectWebSocket();
        disconnectPresenceWebSocket();
    }

    private void disconnectWebSocket() {
        if (webSocket != null) {
            webSocketDisposable.dispose();
            webSocket.disconnect();
            webSocket = null;

            if (!webSocketState.getValue().isFailure()) {
                webSocketState.onNext(WebSocketConnectionState.DISCONNECTED);
            }
        }
    }

    private void disconnectPresenceWebSocket() {
        if (presenceWebSocket != null) {
            presenceWebSocketDisposable.dispose();
            presenceWebSocket.disconnect();
            presenceWebSocket = null;

            if (!presenceWebSocketState.getValue().isFailure()) {
                presenceWebSocketState.onNext(WebSocketConnectionState.DISCONNECTED);
            }
        }
    }

    private synchronized WebSocketConnection getPresenceWebSocket() throws InterruptedException, WebSocketUnavailableException {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (!canConnect || user == null) {
            throw new WebSocketUnavailableException();
        }

        if (presenceWebSocket == null || presenceWebSocket.isDead()) {
            presenceWebSocketDisposable.dispose();

            presenceWebSocket           = webSocketFactory.createPresenceWebSocket();
            presenceWebSocketDisposable = new CompositeDisposable();

            Disposable disposable = presenceWebSocket.connect(user)
                                                     .subscribeOn(Schedulers.computation())
                                                     .observeOn(Schedulers.computation())
                                                     .subscribe(presenceWebSocketState::onNext);
            presenceWebSocketDisposable.add(disposable);
        }
        return presenceWebSocket;
    }

    private synchronized WebSocketConnection getWebSocket() throws WebSocketUnavailableException, InterruptedException {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (!canConnect || user == null) {
            throw new WebSocketUnavailableException();
        }

        if (webSocket == null || webSocket.isDead()) {
            webSocketDisposable.dispose();

            webSocket           = webSocketFactory.createWebSocket();
            webSocketDisposable = new CompositeDisposable();

            Disposable disposable = webSocket.connect(user)
                                             .subscribeOn(Schedulers.computation())
                                             .observeOn(Schedulers.computation())
                                             .subscribe(webSocketState::onNext);
            webSocketDisposable.add(disposable);
        }
        return webSocket;
    }

    public boolean isDead() {
        return webSocket == null || presenceWebSocket == null || webSocket.isDead() || presenceWebSocket.isDead();
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
            getPresenceWebSocket().sendPingMessage();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void notifyMessageError(WebSocketRequestMessage request) {
        try {
            getWebSocket().notifyMessageError(request);
        } catch (WebSocketUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<String> getSeenMessagesNeedToPush() {
        return webSocket != null ? webSocket.getSeenMessagesNeedToPush() : Collections.emptyList();
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
                } else if (isAcceptedMessage(request) || isSeenMessage(request) || isUnsentMessage(request)) {
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

    private static boolean isUnsentMessage(WebSocketRequestMessage request) {
        return request.getStatus() == WebSocketRequestMessage.Status.UNSENT;
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
