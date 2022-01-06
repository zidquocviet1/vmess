package com.mqv.realtimechatapplication.message;

import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.network.websocket.WebSocketResponse;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Primary class for processing the encrypted message received from websocket
* */
public final class IncomingMessageProcessor {
    private static final String TAG = IncomingMessageProcessor.class.getSimpleName();

    private final ChatDao chatDao;
    private final ConversationDao conversationDao;
    private final ConversationService conversationService;

    public IncomingMessageProcessor(ChatDao chatDao,
                                    ConversationDao conversationDao,
                                    ConversationService conversationService) {
        this.chatDao = chatDao;
        this.conversationDao = conversationDao;
        this.conversationService = conversationService;
    }

    public void process(WebSocketResponse message) {
        Logging.debug(TAG, "Receive new request message");

        Chat body = message.getBody();

        if (message.getStatus() == 200) {
            processMessageInternal(body, chatDao.insert(Collections.singletonList(body))
                                                .andThen(Completable.fromAction(() ->
                                                        AppDependencies.getDatabaseObserver()
                                                                       .notifyMessageInserted(body.getConversationId(), body.getId()))));
        } else if (message.getStatus() == HttpURLConnection.HTTP_ACCEPTED) {
            processMessageInternal(body, chatDao.update(body)
                                                .andThen(Completable.fromAction(() ->
                                                        AppDependencies.getDatabaseObserver()
                                                                       .notifyMessageUpdated(body.getConversationId(), body.getId()))));
        }
    }

    private void processMessageInternal(Chat body, Completable action) {
        fetchCacheConversation(body.getConversationId()).subscribeOn(Schedulers.computation())
                                                        .observeOn(Schedulers.computation())
                                                        .flatMapCompletable(optional -> {
                                                            if (optional.isPresent()) {
                                                                return action.andThen(conversationDao.markConversationAsInbox(optional.get()))
                                                                             .onErrorComplete()
                                                                             .doOnError(t -> Logging.debug(TAG, "Insert incoming message failed: " + t));
                                                            } else {
                                                                return fetchRemoteConversation(body.getConversationId());
                                                            }
                                                        })
                                                        .onErrorComplete()
                                                        .doOnError(t -> Logging.debug(TAG, "Can't notify new incoming message update: " + t))
                                                        .subscribe();
    }

    private Single<Optional<Conversation>> fetchCacheConversation(String conversationId) {
        return conversationDao.fetchById(conversationId)
                              .subscribeOn(Schedulers.io())
                              .observeOn(Schedulers.io())
                              .flatMap(map -> Single.just(map.keySet()
                                                    .stream()
                                                    .filter(c -> c.getId().equals(conversationId))
                                                    .findFirst()));
    }

    private Completable fetchRemoteConversation(String conversationId) {
        // fetch new conversation and then insert into database
        return Completable.error(ResourceNotFoundException::new);
    }
}
