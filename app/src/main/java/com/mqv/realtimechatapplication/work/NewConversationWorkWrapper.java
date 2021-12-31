package com.mqv.realtimechatapplication.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Start worker to retrieve new conversation when user accepted a friend request. Or got accept friend request.
* Maybe the user is added into a group.
* Send the NewConversationBroadcastReceiver when the app is running in foreground.
* */
public class NewConversationWorkWrapper extends BaseWorker {
    private final ExistingWorkPolicy mWorkPolicy;
    private final Data mData;

    public NewConversationWorkWrapper(Context context) {
        this(context, new Data.Builder().build());
    }

    public NewConversationWorkWrapper(Context context, Data data) {
        this(context, ExistingWorkPolicy.REPLACE, data);
    }

    public NewConversationWorkWrapper(Context context, ExistingWorkPolicy workPolicy, Data data) {
        super(context);
        mWorkPolicy = workPolicy;
        mData = data;
    }

    @NonNull
    @Override
    public ExistingWorkPolicy getOneTimeWorkPolicy() {
        return mWorkPolicy;
    }

    @NonNull
    @Override
    public WorkRequest createRequest() {
        return new OneTimeWorkRequest.Builder(NewConversationWorker.class)
                                     .setConstraints(retrieveConstraint())
                                     .setInputData(mData)
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
    public static class NewConversationWorker extends RxWorker {
        private final ConversationService mService;
        private final ConversationDao mDao;
        private final FirebaseUser mUser;
        private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
        private final UserUtil mUserUtil;

        @AssistedInject
        public NewConversationWorker(@Assisted @NonNull Context context,
                                     @Assisted @NonNull WorkerParameters workerParams,
                                     ConversationService service,
                                     ConversationDao dao,
                                     UserUtil userUtil) {
            super(context, workerParams);
            mService = service;
            mDao = dao;
            mUser = FirebaseAuth.getInstance().getCurrentUser();
            mUserUtil = userUtil;
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            Data data = getInputData();

            String otherId = data.getString("otherId");
            Boolean isCallFromNotification = data.getBoolean("from_notification", false);

            return mUser == null ? Single.just(Result.failure()) : createCall(otherId, isCallFromNotification);
        }

        @NonNull
        @Override
        protected Scheduler getBackgroundScheduler() {
            return Schedulers.io();
        }

        private Single<Result> createCall(String otherId, Boolean isCallFromNotification) {
            if (isCallFromNotification) {
                return mUserUtil.isRecentLogin()
                                .flatMap(isRecentLogin -> {
                                    if (isRecentLogin) {
                                        return makeCallRemote(otherId);
                                    } else {
                                        return Single.just(Result.failure());
                                    }
                                });
            }
            return makeCallRemote(otherId);
        }

        private Single<Result> makeCallRemote(String otherId) {
            return getBearerTokenObservable().flatMap(token -> mService.findNormalByParticipantId(token, otherId))
                                             .subscribeOn(getBackgroundScheduler())
                                             .observeOn(getBackgroundScheduler())
                                             .flatMap(response -> {
                                                 if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                     Conversation conversation = response.getSuccess();

                                                     conversation.setStatus(ConversationStatusType.INBOX);

                                                     backgroundExecutor.execute(() -> mDao.saveConversationList(Collections.singletonList(conversation)));

                                                     return Observable.just(conversation);
                                                 } else {
                                                     throw new IllegalArgumentException();
                                                 }
                                             })
                                             .singleOrError()
                                             .map(c -> Result.success(new Data.Builder().putString("id", c.getId()).build()))
                                             .onErrorReturnItem(Result.failure());
        }

        private Observable<String> getBearerTokenObservable() {
            return Observable.fromCallable(this::token)
                             .flatMap(optionalToken -> {
                                 if (optionalToken.isPresent()) {
                                     String token = optionalToken.get();
                                     String bearerToken = Const.PREFIX_TOKEN + token;

                                     return Observable.just(bearerToken);
                                 } else {
                                     return Observable.create(emitter -> {
                                         if (!emitter.isDisposed()) {
                                             emitter.onError(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                                         }
                                     });
                                 }
                             });
        }

        private Optional<String> token() {
            try {
                return UserTokenUtil.getToken(mUser);
            } catch (Throwable t) {
                return Optional.empty();
            }
        }
    }
}
