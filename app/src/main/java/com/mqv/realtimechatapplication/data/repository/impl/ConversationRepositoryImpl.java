package com.mqv.realtimechatapplication.data.repository.impl;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.rxjava3.EmptyResultSetException;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.NetworkBoundResource;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.Retriever;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationService service;
    private final ConversationDao     dao;
    private final ChatDao             chatDao;
    private       FirebaseUser        user;

    @Inject
    public ConversationRepositoryImpl(ConversationService service,
                                      ConversationDao dao,
                                      ChatDao chatDao) {
        this.service = service;
        this.dao     = dao;
        this.chatDao = chatDao;
        this.user    = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> user = firebaseAuth.getCurrentUser());
    }

    @Override
    public Flowable<Map<Conversation, List<Chat>>> conversationListUpdateObserve() {
        return dao.observe();
    }

    @Override
    public Observable<ApiResponse<Conversation>> fetchById(String conversationId) {
        return getBearerTokenObservable()
                .flatMapSingle(token -> service.findById(token, conversationId))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<List<Conversation>>> fetchByUid(ConversationStatusType type, int page, int size) {
        return getBearerTokenObservable()
                .flatMap(token -> service.fetchConversation(token, type, page, size))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<Conversation>> fetchByUidNBR(ConversationStatusType type,
                                                        int page,
                                                        int size,
                                                        Runnable onDataChanged) {
        return new NetworkBoundResource<List<Conversation>, ApiResponse<List<Conversation>>>(false) {
            private boolean isCachedNotExists = false;
            private boolean isSaveFreshSuccess = false;

            @Override
            protected void saveCallResult(@NonNull ApiResponse<List<Conversation>> response) {
                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    List<Conversation> freshData = response.getSuccess();

                    if (freshData == null || freshData.isEmpty()) {
                        if (isCachedNotExists)
                            return;

                        dao.deleteAll()
                           .subscribeOn(Schedulers.io())
                           .subscribe(new CompletableObserver() {
                               @Override
                               public void onSubscribe(@NonNull Disposable d) {

                               }

                               @Override
                               public void onComplete() {
                                   Logging.show("Delete all conversation complete");

                                   onDataChanged.run();
                               }

                               @Override
                               public void onError(@NonNull Throwable e) {
                                   Logging.show("Something wrong when insert new conversation data."
                                           + e.getMessage());
                               }
                           });
                    } else {
                        if (isSaveFreshSuccess)
                            return;

                        saveAll(freshData, type)
                                   .subscribeOn(Schedulers.io())
                                   .subscribe(new CompletableObserver() {
                                       @Override
                                       public void onSubscribe(@NonNull Disposable d) {

                                       }

                                       @Override
                                       public void onComplete() {
                                           isSaveFreshSuccess = true;

                                           onDataChanged.run();

                                           Logging.show("Insert new conversation complete");
                                       }

                                       @Override
                                       public void onError(@NonNull Throwable e) {
                                           Logging.show("Something wrong when insert new conversation data."
                                                   + e.getMessage());
                                       }
                                   });
                    }
                }
            }

            @Override
            protected Boolean shouldFetch(@Nullable List<Conversation> data) {
                if (data == null || data.isEmpty()) {
                    isCachedNotExists = true;
                }
                return !isSaveFreshSuccess;
            }

            @Override
            protected Flowable<List<Conversation>> loadFromDb() {
                return fetchCached(type, page, size).toFlowable();
            }

            @Override
            protected Observable<ApiResponse<List<Conversation>>> createCall() {
                return fetchByUid(type, page, size);
            }

            @Override
            protected void callAndSaveResult() {

            }
        }.asObservable();
    }

    @Override
    public Single<List<Conversation>> fetchCached(ConversationStatusType type, int page, int size) {
        return dao.fetchAllByStatus(type, 0, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                  .toObservable()
                  .flatMap(Observable::fromIterable)
                  .flatMap(c -> chatDao.fetchChatByConversation(c.getId(), Const.DEFAULT_CHAT_PAGING_SIZE)
                                       .toObservable()
                                       .flatMap(list -> {
                                           Collections.reverse(list);

                                           c.setChats(list);

                                           return Observable.just(c);
                                       })
                                       .defaultIfEmpty(c))
                  .toList();
    }

    @Override
    public Completable save(Conversation conversation) {
        return Completable.fromAction(() -> dao.saveConversationList(Collections.singletonList(conversation)));
    }

    @Override
    public Completable saveAll(List<Conversation> freshData, ConversationStatusType type) {
        List<Conversation> updatedData = freshData.stream()
                                                  .peek(u -> {
                                                      u.setStatus(type);
                                                      Collections.reverse(u.getChats());
                                                  })
                                                  .collect(Collectors.toList());

        List<String> conversationListId = freshData.stream()
                                                   .map(Conversation::getId)
                                                   .collect(Collectors.toList());

        return Completable.fromAction(() -> dao.saveConversationList(updatedData))
                          .andThen(dao.deleteAll(conversationListId, type));
    }

    @Override
    public Completable deleteAll() {
        return dao.deleteAll();
    }

    @Override
    public Completable delete(Conversation conversation) {
        return dao.delete(conversation);
    }

    @Override
    public void deleteConversationChatRemote(Conversation conversation) {
        delete(conversation).andThen(getBearerTokenObservable()
                            .flatMap(token -> service.deleteConversationChat(token, conversation.getId())))
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .onErrorComplete()
                            .subscribe();
    }

    @Override
    public Completable changeConversationStatus(Conversation conversation) {
        return dao.update(conversation);
    }

    @Override
    public Observable<ApiResponse<Conversation>> changeConversationStatusRemote(String conversationId, int ordinal) {
        return getBearerTokenObservable()
                .flatMap(token -> service.requestChangeConversationStatus(token, conversationId, ordinal))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Conversation> fetchCachedById(String conversationId) {
        return dao.fetchById(conversationId)
                  .flatMap(map -> {
                      Conversation result = null;

                      for (Map.Entry<Conversation, List<Chat>> entry : map.entrySet()) {
                          if (entry.getKey().getId().equals(conversationId)) {
                              List<Chat> chats = Retriever.getOrDefault(entry.getValue(), new ArrayList<>());

                              Collections.reverse(chats);

                              result = entry.getKey();
                              result.setChats(chats);
                          }
                      }
                      return result != null ? Single.just(result) : Single.error(new EmptyResultSetException("not found"));
                  });
    }

    @Override
    public void deleteNormalByParticipantId(String userId, String otherUserId) {
        dao.deleteNormalByParticipantId(userId, otherUserId);
    }

    @Override
    public void deleteByParticipantId(String userId, String otherUserId) {
        dao.deleteByParticipantId(userId, otherUserId);
    }

    private Observable<String> getBearerTokenObservable() {
        return Observable.fromCallable(() -> {
                            try {
                                return new Pair<Optional<String>, Throwable>(UserTokenUtil.getToken(this.user), null);
                            } catch (Throwable t) {
                                return new Pair<>(Optional.<String>empty(), t);
                            }
                         })
                         .flatMap(pair -> {
                             Optional<String> tokenOptional = pair.first;
                             Throwable throwable = pair.second;

                             if (tokenOptional.isPresent()) {
                                 String token = tokenOptional.get();
                                 String bearerToken = Const.PREFIX_TOKEN + token;

                                 return Observable.just(bearerToken);
                             } else {
                                 return Observable.create(emitter -> {
                                     if (!emitter.isDisposed()) {
                                         if (throwable instanceof FirebaseNetworkException) {
                                             emitter.onError(new FirebaseNetworkException("Network error"));
                                         } else {
                                             emitter.onError(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                                         }
                                     }
                                 });
                             }
                         });
    }
}
