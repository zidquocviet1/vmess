package com.mqv.vmess.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.data.dao.ChatDao;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.service.ChatService;
import com.mqv.vmess.network.websocket.WebSocketClient;
import com.mqv.vmess.network.websocket.WebSocketRequestMessage;
import com.mqv.vmess.network.websocket.WebSocketResponse;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.util.UserTokenUtil;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Entry point class to notify all the messages as read or received, sync with multiple devices
* */
public class PushMessageAcknowledgeWorkWrapper extends BaseWorker {
    private final Data mInputData;

    public static final String EXTRA_MARK_AS_READ    = "maskAsRead";
    public static final String EXTRA_LIST_MESSAGE_ID = "message_id";

    public PushMessageAcknowledgeWorkWrapper(Context context, Data data) {
        super(context);
        mInputData = data;
    }

    @NonNull
    @Override
    public WorkRequest createRequest() {
        return new OneTimeWorkRequest.Builder(PushMessageWorker.class)
                                     .setConstraints(retrieveConstraint())
                                     .setInputData(mInputData)
                                     .build();
    }

    @Override
    public Constraints retrieveConstraint() {
        return new Constraints.Builder()
                              .setRequiredNetworkType(NetworkType.CONNECTED)
                              .build();
    }

    @Override
    public boolean isUniqueWork() {
        return false;
    }

    @HiltWorker
    public static class PushMessageWorker extends RxWorker {
        private final ChatDao chatDao;
        private final ChatService chatService;
        private final FirebaseUser user;

        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public PushMessageWorker(@Assisted @NonNull Context appContext,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 ChatDao chatDao,
                                 ChatService chatService) {
            super(appContext, workerParams);
            this.chatDao = chatDao;
            this.chatService = chatService;
            this.user = FirebaseAuth.getInstance().getCurrentUser();
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            boolean isMarkAsRead = getInputData().getBoolean(EXTRA_MARK_AS_READ, false);
            String[] messageIds = getInputData().getStringArray(EXTRA_LIST_MESSAGE_ID);

            if (isMarkAsRead) {
                return messageIds == null ? Single.just(Result.failure()) : markAsReadWork(Arrays.asList(messageIds));
            } else {
                return messageIds == null ? Single.just(Result.failure()) : responseReceivedWork(Arrays.asList(messageIds));
            }
        }

        private Single<Result> markAsReadWork(List<String> messageIds) {
            if (LifecycleUtil.isAppForeground()) {
                return Observable.fromIterable(messageIds)
                                 .subscribeOn(Schedulers.io())
                                 .observeOn(Schedulers.io())
                                 .concatMapSingle(chatDao::findById)
                                 .concatMapSingle(c -> sendWebsocketRequest(WebSocketRequestMessage.Status.SEEN_MESSAGE, c))
                                 .flatMapCompletable(response -> {
                                     AppDependencies.getIncomingMessageProcessor().process(response);
                                     return Completable.complete();
                                 })
                                 .toSingleDefault(Result.success())
                                 .onErrorReturnItem(Result.failure());
            } else {
                return sendSeenMessageBackground(messageIds);
            }
        }

        private Single<Result> responseReceivedWork(List<String> messageIds) {
            if (LifecycleUtil.isAppForeground()) {
                return Observable.fromIterable(messageIds)
                                 .subscribeOn(Schedulers.io())
                                 .observeOn(Schedulers.io())
                                 .concatMapSingle(chatDao::findById)
                                 .concatMapSingle(c -> sendWebsocketRequest(WebSocketRequestMessage.Status.ACCEPTED_MESSAGE, c))
                                 .flatMapCompletable(response -> {
                                     AppDependencies.getIncomingMessageProcessor().process(response);
                                     return Completable.complete();
                                 })
                                 .toSingleDefault(Result.success())
                                 .onErrorReturnItem(Result.failure());
            } else {
                return sendReceivedMessageBackground(messageIds);
            }
        }

        private Single<WebSocketResponse> sendWebsocketRequest(WebSocketRequestMessage.Status status, Chat body) {
            WebSocketRequestMessage request = new WebSocketRequestMessage(new SecureRandom().nextLong(),
                                                                          status,
                                                                          body,
                                                                          user.getUid());
            WebSocketClient webSocket = AppDependencies.getWebSocket();
            webSocket.connect();

            return webSocket.sendRequest(request)
                            .doOnError(t -> webSocket.notifyMessageError(request));
        }

        private Single<Result> sendSeenMessageBackground(List<String> messageIds) {
            return UserTokenUtil.getTokenSingle(user)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .flatMapObservable(token -> Observable.fromIterable(messageIds)
                                                                      .flatMap(id -> chatDao.findById(id).toObservable())
                                                                      .flatMap(chat -> chatService.seenMessage(token, chat)))
                                .singleOrError()
                                .compose(RxHelper.parseSingleResponseData())
                                .flatMap(updated -> Single.just(Result.success()))
                                .doOnError(Throwable::printStackTrace)
                                .onErrorReturnItem(Result.failure());
        }

        private Single<Result> sendReceivedMessageBackground(List<String> messageIds) {
            return UserTokenUtil.getTokenSingle(user)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .flatMapObservable(token -> Observable.fromIterable(messageIds)
                                                                      .flatMap(chat -> chatService.notifyReceiveMessage(token, chat)))
                                .singleOrError()
                                .compose(RxHelper.parseSingleResponseData())
                                .flatMap(updated -> Single.just(Result.success()))
                                .doOnError(Throwable::printStackTrace)
                                .onErrorReturnItem(Result.failure());
        }
    }
}
