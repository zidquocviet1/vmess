package com.mqv.realtimechatapplication.message;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.websocket.WebSocketClient;
import com.mqv.realtimechatapplication.network.websocket.WebSocketConnectionState;
import com.mqv.realtimechatapplication.network.websocket.WebSocketResponse;
import com.mqv.realtimechatapplication.util.Logging;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Class for observing all of the messages from websocket: request, response, ping, pong
* */
public class IncomingMessageObserver {
    private static final String TAG                     = IncomingMessageObserver.class.getSimpleName();
    private static final long   READ_REQUEST_TIMEOUT    = TimeUnit.MINUTES.toMillis(1);

    public IncomingMessageObserver(Context context) {
        Constraints constraints = new Constraints.Builder()
                                                 .setRequiredNetworkType(NetworkType.CONNECTED)
                                                 .build();
        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(MessageWorkerRetriever.class)
                                                                        .setConstraints(constraints).build());
    }

    @HiltWorker
    public static class MessageWorkerRetriever extends RxWorker {
        private final WebSocketClient webSocket;
        private final ChatDao chatDao;
        private final ConversationDao conversationDao;
        private boolean terminated;
        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public MessageWorkerRetriever(@Assisted @NonNull Context appContext,
                                      @Assisted @NonNull WorkerParameters workerParams,
                                      ChatDao chatDao,
                                      ConversationDao conversationDao,
                                      WebSocketClient webSocket) {
            super(appContext, workerParams);
            this.webSocket = webSocket;
            this.chatDao = chatDao;
            this.conversationDao = conversationDao;
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            Logging.debug(TAG, "Making websocket connection...");

            webSocket.connect();

            //noinspection ResultOfMethodCallIgnored
            webSocket.getWebSocketState()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(state -> {
                        if (state == WebSocketConnectionState.CONNECTED) {
                            try {
                                while (!terminated) {
                                    try {
                                        Logging.debug(TAG, "Reading message....");

                                        WebSocketResponse message = webSocket.readMessage(TimeUnit.MINUTES.toMillis(READ_REQUEST_TIMEOUT));

                                        if (message.getStatus() == 200) {
                                            Logging.debug(TAG, "Receive new request message");

                                            Chat body = message.getBody();

                                            // Fetch local if not present then make a request to server to get conversation
                                            // Insert message into conversation
                                            // update conversation status as inbox

                                            conversationDao.fetchById(body.getConversationId())
                                                            .flatMap(map -> Single.just(map.keySet()
                                                                    .stream()
                                                                    .filter(c -> c.getId().equals(body.getConversationId()))
                                                                    .findFirst()))
                                                            .flatMapCompletable(optional -> {
                                                                if (optional.isPresent()) {
                                                                    return chatDao.insert(Collections.singletonList(body))
                                                                            .andThen(conversationDao.markConversationAsInbox(optional.get()))
                                                                            .onErrorComplete()
                                                                            .doOnError(t -> Logging.debug(TAG, "Insert incoming message failed: " + t));
                                                                } else {
                                                                    // TODO: make remote request to receive conversation
                                                                    return Completable.error(ResourceNotFoundException::new);
                                                                }
                                                            })
                                                           .onErrorComplete()
                                                           .doOnError(t -> Logging.debug(TAG, "Can't notify new incoming message update: " + t))
                                                           .subscribe();
                                        }
                                    } catch (IOException e) {
                                        Logging.show("Connection is not available right now!");
                                        terminated = true;
                                    } catch (TimeoutException e) {
                                        Logging.show("Have no any request message");
                                        terminated = true;
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        terminated = true;
                                    }
                                }
                            } finally {
                                 webSocket.disconnect();
                            }
                        }
                    });
            return Single.just(Result.success());
        }
    }
}
