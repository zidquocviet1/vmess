package com.mqv.vmess.data.repository.impl;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.rxjava3.EmptyResultSetException;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.data.dao.ConversationDao;
import com.mqv.vmess.data.dao.ConversationOptionDao;
import com.mqv.vmess.data.model.ConversationNotificationOption;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.NetworkBoundResource;
import com.mqv.vmess.network.exception.FirebaseUnauthorizedException;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.ConversationOption;
import com.mqv.vmess.network.model.type.ConversationStatusType;
import com.mqv.vmess.network.service.ConversationService;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.DateTimeHelper;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.Retriever;
import com.mqv.vmess.util.UserTokenUtil;

import java.io.File;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationService   service;
    private final ConversationDao       dao;
    private final ConversationOptionDao optionDao;
    private final ChatDao               chatDao;
    private       FirebaseUser          user;

    @Inject
    public ConversationRepositoryImpl(ConversationService service,
                                      ConversationDao dao,
                                      ConversationOptionDao optionDao,
                                      ChatDao chatDao) {
        this.service   = service;
        this.dao       = dao;
        this.chatDao   = chatDao;
        this.optionDao = optionDao;
        this.user      = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> user = firebaseAuth.getCurrentUser());
    }

    @Override
    public Flowable<Map<Conversation, Chat>> observeUnreadConversation(ConversationStatusType statusType, int limit) {
        return dao.observeUnreadConversation(statusType, limit);
    }

    @Override
    public Flowable<Map<Conversation, Chat>> conversationAndLastChat(ConversationStatusType statusType, int size) {
        return dao.conversationAndLastChat(statusType, size);
    }

    @Override
    public Flowable<List<ConversationNotificationOption>> observeNotificationOption() {
        return optionDao.fetchAllNotificationNonExpired(DateTimeHelper.toLong(LocalDateTime.now()));
    }

    @Override
    public Single<Map<Conversation, Chat>> conversationAndLastChat(String conversationId, ConversationStatusType statusType) {
        return dao.conversationAndLastChat(conversationId, statusType);
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
    public Single<Map<Conversation, Chat>> fetchCachePaging(ConversationStatusType type, int page, int size) {
        return dao.fetchCachePaging(type, page, size);
    }

    @Override
    public Single<ApiResponse<Conversation>> findNormalByParticipantId(String participantId) {
        return getBearerTokenObservable()
                .flatMap(token -> service.findNormalByParticipantId(token, participantId))
                .singleOrError()
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable saveConversationWithoutNotify(List<Conversation> freshData, ConversationStatusType type) {
        List<Conversation> updatedData = freshData.stream()
                                                  .peek(u -> {
                                                      u.setStatus(type);
                                                      Collections.reverse(u.getChats());
                                                  })
                                                  .collect(Collectors.toList());

        return Completable.fromAction(() -> dao.saveConversationListWithoutNotify(updatedData));
    }

    @Override
    public Single<List<Conversation>> fetchAllWithoutMessages() {
        return dao.fetchAllAsync();
    }

    @Override
    public Single<Boolean> isExists(String conversationId) {
        return dao.isExists(conversationId);
    }

    @Override
    public Single<List<Conversation>> suggestConversation(int size) {
        return dao.suggestConversation(size);
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

        return Completable.fromAction(() -> dao.saveConversationList(updatedData));
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
    public Completable insertNotificationOption(List<ConversationNotificationOption> option) {
        return optionDao.insertAllNotification(option);
    }

    @Override
    public Completable deleteNotificationOption(String conversationId) {
        return optionDao.deleteNotification(conversationId);
    }

    @Override
    public Completable deleteAllNotificationOption() {
        return optionDao.deleteAll();
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
    public Observable<ApiResponse<Conversation>> changeConversationGroupName(String conversationId, String groupName) {
        return getBearerTokenObservable()
                .flatMap(token -> service.changeConversationGroupName(token, groupName, conversationId))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Conversation>> changeConversationGroupThumbnail(String conversationId, File image) {
        RequestBody        requestBody = RequestBody.create(image, MediaType.parse("image/*"));
        MultipartBody.Part part        = MultipartBody.Part.createFormData("image", image.getName(), requestBody);

        return UserTokenUtil.getTokenSingle(user).flatMapObservable(token -> service.changeConversationGroupThumbnail(token, conversationId, part));
    }

    @Override
    public Observable<ApiResponse<Conversation>> addGroupMember(String conversationId, String memberId) {
        return getBearerTokenObservable()
                .flatMap(token -> service.addConversationGroupMember(token, conversationId, memberId))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Conversation>> removeGroupMember(String conversationId, String memberId) {
        return UserTokenUtil.getTokenSingle(user).flatMapObservable(token -> service.removeGroupMember(token, conversationId, memberId));
    }

    @Override
    public Observable<ApiResponse<Conversation>> leaveGroup(String conversationId) {
        return UserTokenUtil.getTokenSingle(user).flatMapObservable(token -> service.leaveGroup(token, conversationId));
    }

    @Override
    public Observable<ApiResponse<List<ConversationOption>>> getAllMuteNotification() {
        return UserTokenUtil.getTokenSingle(user).flatMapObservable(service::getAllMuteNotification);
    }

    @Override
    public Observable<ApiResponse<ConversationOption>> mute(String conversationId, long until) {
        return UserTokenUtil.getTokenSingle(user).flatMapObservable(token -> service.mute(token, conversationId, until));
    }

    @Override
    public Observable<Boolean> umute(String conversationId) {
        return UserTokenUtil.getTokenSingle(user).flatMapObservable(token -> service.unmute(token, conversationId));
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

    @Override
    public Observable<ApiResponse<Conversation>> createGroup(Conversation conversation) {
        return getBearerTokenObservable()
                .flatMap(token -> service.createGroup(token, conversation))
                .subscribeOn(Schedulers.io());
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
