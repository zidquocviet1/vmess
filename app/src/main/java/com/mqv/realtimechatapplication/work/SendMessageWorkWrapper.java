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
import com.mqv.realtimechatapplication.network.model.type.MessageType;
import com.mqv.realtimechatapplication.network.websocket.WebSocketRequestMessage;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Objects;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Using this worker to send messages, because we can send multiple types of message.
* Like: video, photo, file...etc
* That mean it's long running task
* */
public class SendMessageWorkWrapper extends BaseWorker {
    private final Data mInputData;

    public static final String EXTRA_SENDER_ID          = "senderId";
    public static final String EXTRA_CONTENT            = "content";
    public static final String EXTRA_CONVERSATION_ID    = "conversationId";
    public static final String EXTRA_MESSAGE_TYPE       = "messageType";
    public static final String EXTRA_MESSAGE_ID         = "messageId";

    public SendMessageWorkWrapper(Context context, Data data) {
        super(context);
        mInputData = data;
    }

    @NonNull
    @Override
    public WorkRequest createRequest() {
        return new OneTimeWorkRequest.Builder(SendMessageWorker.class)
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
    public static class SendMessageWorker extends RxWorker {
        private final ChatDao         dao;
        private final FirebaseUser    user;
        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public SendMessageWorker(@Assisted @NonNull Context appContext,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 ChatDao dao) {
            super(appContext, workerParams);
            this.dao       = dao;
            this.user      = FirebaseAuth.getInstance().getCurrentUser();
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            return user == null ? Single.just(Result.failure()) : sendRequest();
        }

        private Chat parseChat() {
            Data    data                = getInputData();
            String  messageId           = data.getString(EXTRA_MESSAGE_ID);
            String  senderId            = data.getString(EXTRA_SENDER_ID);
            String  content             = data.getString(EXTRA_CONTENT);
            String  conversationId      = data.getString(EXTRA_CONVERSATION_ID);
            String  messageTypeString   = data.getString(EXTRA_MESSAGE_TYPE);

            return new Chat(Objects.requireNonNull(messageId),
                            senderId,
                            content,
                            conversationId,
                            MessageType.valueOf(messageTypeString));
        }

        private Single<Result> sendRequest() {
            Chat chat = parseChat();

            insertMessage(chat);

            return AppDependencies.getWebSocket().sendRequest(new WebSocketRequestMessage(new SecureRandom().nextLong(),
                                                                    WebSocketRequestMessage.Status.INCOMING_MESSAGE,
                                                                    chat, user.getUid()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .flatMap(response -> {
                                updateMessageStatus(response.getBody());

                                return Single.just(Result.success());
                            })
                            .onErrorReturnItem(Result.failure());
        }

        private void insertMessage(Chat message) {
            dao.insert(Collections.singletonList(message))
               .subscribeOn(Schedulers.io())
               .observeOn(Schedulers.io())
               .onErrorComplete()
               .subscribe();
        }

        private void updateMessageStatus(Chat message) {
            dao.update(message)
               .andThen(Completable.fromAction(() -> AppDependencies.getDatabaseObserver()
                                                                    .notifyMessageUpdated(message.getConversationId(), message.getId())))
               .subscribeOn(Schedulers.io())
               .observeOn(Schedulers.io())
               .subscribe();
        }
    }
}
