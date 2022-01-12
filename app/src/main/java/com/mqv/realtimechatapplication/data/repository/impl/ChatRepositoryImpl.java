package com.mqv.realtimechatapplication.data.repository.impl;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.room.rxjava3.EmptyResultSetException;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.data.repository.ChatRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.service.ChatService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatRepositoryImpl implements ChatRepository {
    private final ChatDao           dao;
    private final ChatService       service;
    private       FirebaseUser      user;

    @Inject
    public ChatRepositoryImpl(ChatDao dao, ChatService service) {
        this.dao        = dao;
        this.service    = service;
        this.user       = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> user = firebaseAuth.getCurrentUser());
    }

    @Override
    public Observable<ApiResponse<Chat>> fetchChatRemoteById(String id) {
        return getBearerTokenObservable().flatMap(token -> service.fetchById(token, id));
    }

    @Override
    public Observable<ApiResponse<List<Chat>>> loadMoreChat(@NonNull String conversationId, int page, int size) {
        return getBearerTokenObservable()
                .flatMap(token -> service.loadMoreChat(token, conversationId, page, size))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat) {
        return getBearerTokenObservable()
                .flatMap(token -> service.sendMessage(token, chat))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Chat>> seenMessage(@NonNull Chat chat) {
        updateCached(chat);

        return getBearerTokenObservable()
                .flatMap(token -> service.seenMessage(token, chat))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<ApiResponse<Chat>> seenWelcomeMessage(@NonNull Chat chat) {
        return getBearerTokenObservable()
                .flatMap(token -> service.seenWelcomeMessage(token, chat))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<List<Chat>> pagingCachedByConversation(String id, int page, int size) {
        return dao.fetchChatByConversation(id, page, size)
                  .flatMap(list -> {
                      if (list == null || list.isEmpty())
                          return Single.error(new EmptyResultSetException("Empty list chats"));
                      return Single.just(list);
                  });
    }

    @Override
    public Single<Chat> fetchCached(String id) {
        return dao.findById(id);
    }

    @Override
    public Completable saveCached(Chat chat) {
        return dao.insert(chat)
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.io());
    }

    @Override
    public void saveCached(List<Chat> chat) {
        dao.insert(chat)
           .subscribeOn(Schedulers.io())
           .subscribe();
    }

    @Override
    public void updateCached(Chat chat) {
        dao.update(chat)
           .subscribeOn(Schedulers.io())
           .subscribe();
    }

    @Override
    public void updateCached(List<Chat> chats) {
        dao.update(chats)
           .subscribeOn(Schedulers.io())
           .subscribe();
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
