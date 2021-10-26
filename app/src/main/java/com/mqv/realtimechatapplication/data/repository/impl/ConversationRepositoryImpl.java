package com.mqv.realtimechatapplication.data.repository.impl;

import static com.mqv.realtimechatapplication.util.Const.DEFAULT_AUTHORIZER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
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
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationService service;
    private final ConversationDao     dao;
    private       FirebaseUser        user;

    @Inject
    public ConversationRepositoryImpl(ConversationService service,
                                      ConversationDao dao) {
        this.service = service;
        this.dao     = dao;
        this.user    = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> user = firebaseAuth.getCurrentUser());
    }

    @Override
    public Observable<ApiResponse<List<Conversation>>> fetchByUid(ConversationStatusType type) {
        return getBearerTokenObservable()
                .flatMap(token -> service.fetchConversation(token,
                                                            DEFAULT_AUTHORIZER,
                                                            type))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<Conversation>> fetchByUidNBR(ConversationStatusType type) {
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

                        List<Conversation> updatedData = freshData.stream()
                                .peek(u -> u.setStatus(type))
                                .collect(Collectors.toList());

                        List<String> conversationListId = freshData.stream()
                                .map(Conversation::getId)
                                .collect(Collectors.toList());

                        dao.saveAll(updatedData)
                                .andThen(dao.deleteAll(conversationListId))
                                .subscribeOn(Schedulers.io())
                                .subscribe(new CompletableObserver() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onComplete() {
                                        isSaveFreshSuccess = true;
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
                return true;
            }

            @Override
            protected Flowable<List<Conversation>> loadFromDb() {
                return dao.fetchAllByStatus(type);
            }

            @Override
            protected Observable<ApiResponse<List<Conversation>>> createCall() {
                return fetchByUid(type);
            }

            @Override
            protected void callAndSaveResult() {

            }
        }.asObservable();
    }

    @Override
    public Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat) {
        return getBearerTokenObservable()
                .flatMap(token -> service.sendMessage(token,
                                                      DEFAULT_AUTHORIZER,
                                                      chat))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Chat>> seenMessage(@NonNull Chat chat) {
        return getBearerTokenObservable()
                .flatMap(token -> service.seenMessage(token,
                                                      DEFAULT_AUTHORIZER,
                                                      chat))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable updateConversation(Conversation conversation) {
        return dao.update(conversation);
    }

    private Observable<String> getBearerTokenObservable() {
        return Observable.fromCallable(() -> UserTokenUtil.getToken(this.user))
                         .flatMap(tokenOptional -> {
                             if (tokenOptional.isPresent()) {
                                 String token = tokenOptional.get();
                                 String bearerToken = Const.PREFIX_TOKEN + token;

                                 return Observable.just(bearerToken);
                             } else {
                                 return Observable.create(emitter -> {
                                     if (!emitter.isDisposed())
                                         emitter.onError(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                                 });
                             }
                         });
    }
}
