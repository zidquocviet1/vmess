package com.mqv.realtimechatapplication.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.hilt.work.HiltWorker;
import androidx.room.rxjava3.EmptyResultSetException;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.preferences.AppPreferences;
import com.mqv.realtimechatapplication.data.dao.ConversationDao;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.ConversationGroup;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.network.service.ConversationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Single;

public class ConversationNotificationWorkWrapper extends BaseWorker{
    private final ExistingWorkPolicy mWorkPolicy;
    private final Data mData;

    // Conversation notification key data
    public static final String KEY_CONVERSATION_ID = "conversation_id";
    public static final String KEY_MESSAGE_ID = "message_id";
    public static final String KEY_MESSAGE_BODY = "message_body";
    public static final String KEY_SENDER_ID = "sender_id";
    public static final String KEY_TYPE = "type";

    enum ConversationNotificationType {
        ADDED_TO_GROUP,
        RECEIVE_REQUEST_MESSAGE,
        RECEIVE_MESSAGE
    }

    public ConversationNotificationWorkWrapper(Context context, Data data) {
        this(context, ExistingWorkPolicy.REPLACE, data);
    }

    public ConversationNotificationWorkWrapper(Context context, ExistingWorkPolicy workPolicy, Data data) {
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
        return new OneTimeWorkRequest.Builder(ConversationNotificationWorker.class)
                                     .setInputData(mData)
                                     .setConstraints(retrieveConstraint())
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
    public static class ConversationNotificationWorker extends RxWorker {
        private final ConversationDao           dao;
        private final ConversationService       service;
        private final FirebaseUser              user;
        private final ExecutorService           executorService;
        private final AppPreferences            preferences;
        private final Context                   context;

        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public ConversationNotificationWorker(@Assisted @NonNull Context appContext,
                                              @Assisted @NonNull WorkerParameters workerParams,
                                              ConversationDao dao,
                                              ConversationService service,
                                              AppPreferences preferences) {
            super(appContext, workerParams);
            this.dao                = dao;
            this.service            = service;
            this.user               = FirebaseAuth.getInstance().getCurrentUser();
            this.executorService    = Executors.newSingleThreadExecutor();
            this.preferences        = preferences;
            this.context            = appContext;
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            return user == null ? Single.just(Result.failure()) : showNotificationResult(getInputData());
        }

        private Single<Result> showNotificationResult(Data data) {
            String conversationId = data.getString(KEY_CONVERSATION_ID);
            String messageId = data.getString(KEY_MESSAGE_ID);
            String messageBody = data.getString(KEY_MESSAGE_BODY);
            String senderId = data.getString(KEY_SENDER_ID);
            String type = data.getString(KEY_TYPE);

            ConversationNotificationType enumType = ConversationNotificationType.valueOf(type);

            switch (enumType) {
                case ADDED_TO_GROUP:
                    return null;
                case RECEIVE_REQUEST_MESSAGE:
                    return null;
                case RECEIVE_MESSAGE:
                    return Single.fromCallable(() -> dao.conversationAndChat(conversationId))
                                 .flatMap(map -> {
                                     Optional<Conversation> optionalConversation = map.keySet()
                                             .stream()
                                             .filter(c -> c.getId().equals(conversationId))
                                             .findFirst();

                                     if (optionalConversation.isPresent()) {
                                         Conversation conversation = optionalConversation.get();
                                         conversation.setChats(map.get(conversation));
                                         return Single.just(conversation);
                                     }
                                     return Single.error(new EmptyResultSetException("empty"));
                                 })
                                 .onErrorResumeNext(t -> {
                                     if (t instanceof EmptyResultSetException)
                                         return fetchRemoteConversation(conversationId);
                                     return Single.error(t);
                                 })
                              .flatMap(conversation -> {
                                  List<User> participants = conversation.getParticipants();

                                  Optional<User> userOptional = participants.stream()
                                                                            .filter(u -> u.getUid().equals(senderId))
                                                                            .findFirst();

                                  if (userOptional.isPresent()) {
                                      User sender = userOptional.get();
                                      String thumbnail;
                                      String title;

                                      if (conversation.getType() == ConversationType.GROUP) {
                                          // With a group it has name and thumbnail itself
                                          ConversationGroup group = conversation.getGroup();

                                          thumbnail = group.getThumbnail();
                                          title = group.getName();
                                      } else {
                                          // Not supported self conversation yet

                                          thumbnail = sender.getPhotoUrl();
                                          title = sender.getDisplayName();
                                      }

                                      // If the app is in foreground don't show the notification
                                      if (LifecycleUtil.isAppForeground()) {
                                          Intent broadcastIntent = new Intent("com.mqv.tac.NEW_CONVERSATION");
                                          broadcastIntent.putExtra("new_message", true);
                                          broadcastIntent.putExtra("message_id", messageId);
                                          broadcastIntent.putExtra("data", conversation);
                                          context.sendBroadcast(broadcastIntent);
                                      } else {
                                          // Push conversation notification
                                          // Also check if this user turn on notification
                                          if (preferences.getNotificationStatus()) {
                                              //TODO: Check whether the user is mute this conversation notification or not
                                              pushNotification(title, messageBody, thumbnail);
                                          }
                                      }
                                      return Single.just(Result.success());
                                  }
                                  return Single.just(Result.failure());
                              });
                default:
                    throw new IllegalArgumentException();
            }
        }

        private Single<Conversation> fetchRemoteConversation(String id) {
            return Single.fromCallable(this::getToken)
                         .flatMap(optionalToken -> {
                            if (optionalToken.isPresent()) {
                                String token = optionalToken.get();
                                String bearerToken = Const.PREFIX_TOKEN + token;
                                return service.findById(bearerToken, id);
                            } else {
                                return Single.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                            }
                         }).flatMap(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                Conversation conversation = response.getSuccess();

                                executorService.execute(() -> dao.saveConversationList(Collections.singletonList(conversation)));

                                return Single.just(conversation);
                            } else {
                                return Single.error(new ResourceNotFoundException());
                            }
                         });
        }

        private Optional<String> getToken() {
            try {
                return UserTokenUtil.getToken(user);
            } catch (Throwable throwable) {
                return Optional.empty();
            }
        }

        private void pushNotification(String title, String body, String thumbnail) {
            NotificationChannel channel = new NotificationChannel(Const.CONVERSATION_CHANNEL_ID,
                                                                  Const.CONVERSATION_CHANNEL_NAME,
                                                                  NotificationManager.IMPORTANCE_HIGH);

            NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(0, "Mark Read", null).build();
            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(0, "Reply", null).build();

            Notification notification = new NotificationCompat.Builder(context, Const.CONVERSATION_CHANNEL_ID)
                                                              .setContentTitle(title)
                                                              .setContentText(body)
                                                              .setLargeIcon(getThumbnailBitmap(thumbnail))
                                                              .setAutoCancel(true)
                                                              .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                                              .addAction(markAsReadAction)
                                                              .addAction(replyAction)
                                                              .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                              .build();

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
            notificationManager.notify(0, notification);
        }

        private Bitmap getThumbnailBitmap(String thumbnail) {
            String photoUrl = thumbnail == null ? null : thumbnail.replace("localhost", Const.BASE_IP);

            try {
                return GlideApp.with(context)
                               .asBitmap()
                               .load(photoUrl)
                               .error(R.drawable.ic_account_undefined)
                               .fallback(R.drawable.ic_round_account)
                               .diskCacheStrategy(DiskCacheStrategy.ALL)
                               .submit()
                               .get();
            } catch (ExecutionException | InterruptedException e) {
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_round_account);
            }
        }
    }
}
