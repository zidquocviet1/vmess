package com.mqv.realtimechatapplication.work;

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
import com.mqv.realtimechatapplication.data.dao.ChatDao;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.type.MessageStatus;
import com.mqv.realtimechatapplication.network.websocket.WebSocketRequestMessage;
import com.mqv.realtimechatapplication.network.websocket.WebSocketResponse;

import java.io.IOException;
import java.security.SecureRandom;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Entry point class to notify all the messages as read or received, sync with multiple devices
* */
public class PushMessageAcknowledgeWorkWrapper extends BaseWorker {
    private final Data mInputData;

    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_MARK_AS_READ    = "maskAsRead";

    public PushMessageAcknowledgeWorkWrapper(Context context, Data data) {
        super(context);
        mInputData = data;
    }

    @NonNull
    @Override
    public WorkRequest createRequest() {
        return new OneTimeWorkRequest.Builder(PushMessageWorker.class)
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
        private final FirebaseUser user;

        private static final int MARK_AS_READ_CODE = 201;
        private static final int MARK_RECEIVED_CODE = 202;

        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public PushMessageWorker(@Assisted @NonNull Context appContext,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 ChatDao chatDao) {
            super(appContext, workerParams);
            this.chatDao = chatDao;
            this.user = FirebaseAuth.getInstance().getCurrentUser();
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            String conversationId = getInputData().getString(EXTRA_CONVERSATION_ID);
            boolean isMarkAsRead = getInputData().getBoolean(EXTRA_MARK_AS_READ, false);

            if (isMarkAsRead) {
                return conversationId == null ? Single.just(Result.failure()) : markAsReadWork(conversationId);
            } else {
                return responseReceivedWork();
            }
        }

        private Single<Result> markAsReadWork(String conversationId) {
            return chatDao.fetchUnreadChatByConversation(conversationId, user.getUid())
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.io())
                          .flattenAsObservable(list -> list)
                          .flatMapSingle(c -> sendWebsocketRequest(WebSocketRequestMessage.Status.SEEN_MESSAGE, c))
                          .flatMapCompletable(response -> {
                              if (response.getStatus() == MARK_AS_READ_CODE) {
                                  return Completable.complete();
                              } else {
                                  return Completable.error(new IOException());
                              }
                          })
                          .toSingleDefault(Result.success())
                          .onErrorReturnItem(Result.failure());
        }

        private Single<Result> responseReceivedWork() {
            return Single.just(Result.success());
        }

        private Single<WebSocketResponse> sendWebsocketRequest(WebSocketRequestMessage.Status status, Chat body) {
            String userId = user.getUid();

            body.setStatus(MessageStatus.SEEN);
            body.getSeenBy().add(userId);

            updateAndNotifyChanged(body);

            WebSocketRequestMessage request = new WebSocketRequestMessage(new SecureRandom().nextLong(),
                                                                          status,
                                                                          body,
                                                                          userId);
            return AppDependencies.getWebSocket().sendRequest(request);
        }

        private void updateAndNotifyChanged(Chat body) {
            chatDao.update(body)
                   .andThen(Completable.fromAction(() -> AppDependencies.getDatabaseObserver()
                                                                        .notifyMessageUpdated(body.getConversationId(), body.getId())))
                   .subscribeOn(Schedulers.io())
                   .observeOn(Schedulers.io())
                   .onErrorComplete()
                   .subscribe();
        }
    }
}
