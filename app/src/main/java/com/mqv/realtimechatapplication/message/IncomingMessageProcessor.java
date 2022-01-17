package com.mqv.realtimechatapplication.message;

import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
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

    private static final int RESPONSE_INCOMING_MESSAGE = 200;
    private static final int RESPONSE_STATUS_MESSAGE = 202;

    private final ChatDao chatDao;
    private final ConversationDao conversationDao;
    private final ConversationRepository conversationRepository;

    public IncomingMessageProcessor(ChatDao chatDao,
                                    ConversationDao conversationDao,
                                    ConversationService conversationService,
                                    ConversationRepository conversationRepository) {
        this.chatDao = chatDao;
        this.conversationDao = conversationDao;
        this.conversationRepository = conversationRepository;
    }

    public void process(WebSocketResponse message) {
        Logging.debug(TAG, "Receive new request message");

        Chat body = message.getBody();

        if (message.getStatus() == RESPONSE_INCOMING_MESSAGE) {
            processMessageInternal(body, chatDao.insert(Collections.singletonList(body))
                                                .andThen(Completable.fromAction(() ->
                                                         AppDependencies.getDatabaseObserver()
                                                                        .notifyMessageInserted(body.getConversationId(), body.getId()))), false);
        } else if (message.getStatus() == RESPONSE_STATUS_MESSAGE) {
            processMessageInternal(body, chatDao.update(body)
                                                .andThen(Completable.fromAction(() ->
                                                         AppDependencies.getDatabaseObserver()
                                                                        .notifyMessageUpdated(body.getConversationId(), body.getId()))), true);
        }
    }

    private void processMessageInternal(Chat body, Completable action, boolean isUpdate) {
        fetchCacheConversation(body.getConversationId()).subscribeOn(Schedulers.computation())
                                                        .observeOn(Schedulers.computation())
                                                        .flatMapCompletable(optional -> {
                                                            if (optional.isPresent()) {
                                                                Completable newAction = action;

                                                                if (!isUpdate) {
                                                                    newAction = action.andThen(conversationDao.markConversationAsInbox(optional.get()));
                                                                }

                                                                return newAction.onErrorComplete()
                                                                                .doOnError(t -> Logging.debug(TAG, "Insert incoming message failed: " + t));
                                                            } else {
                                                                return fetchRemoteConversation(body.getConversationId(), body.getId());
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

    /*
    * Fetch conversation when the conversation is deleted and make a notification with messageId
    * */
    private Completable fetchRemoteConversation(String conversationId, String messageId) {
        // fetch new conversation and then insert into database
        return conversationRepository.fetchById(conversationId)
                                     .subscribeOn(Schedulers.io())
                                     .observeOn(Schedulers.io())
                                     .flatMapCompletable(response -> {
                                         if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                             Conversation conversation = response.getSuccess();
                                             conversation.setStatus(ConversationStatusType.INBOX);
                                             return conversationRepository.save(conversation)
                                                     .andThen(Completable.fromAction(() -> AppDependencies.getDatabaseObserver().notifyConversationInserted(conversationId)));
                                         }
                                         return Completable.error(ResourceNotFoundException::new);
                                     })
                                     .onErrorComplete();
    }
}
