package com.mqv.vmess.work;

import android.content.Context;
import android.util.Pair;

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
import com.mqv.vmess.data.repository.impl.ChatRepositoryImpl;
import com.mqv.vmess.data.repository.impl.StorageRepositoryImpl;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.exception.FileTooLargeException;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.ForwardMessagePayload;
import com.mqv.vmess.network.model.Type;
import com.mqv.vmess.network.model.UploadResult;
import com.mqv.vmess.network.model.type.MessageStatus;
import com.mqv.vmess.network.websocket.WebSocketClient;
import com.mqv.vmess.network.websocket.WebSocketRequestMessage;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.util.FileProviderUtil;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.MessageUtil;
import com.mqv.vmess.util.UserTokenUtil;

import java.io.File;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/*
* Using this worker to send messages, because we can send multiple types of message.
* Like: video, photo, file...etc
* That mean it's long running task
* */
public class SendMessageWorkWrapper extends BaseWorker {
    private final Data mInputData;
    private static final long MAX_VIDEO_SIZE    = 28 * 1024 * 1024; // 28MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_PHOTO_SIZE    = 4 * 1024 * 1024; // 4MB

    public static final String EXTRA_MESSAGE_ID      = "message_id";
    public static final String EXTRA_FORWARD_MESSAGE = "forward_message";

    public SendMessageWorkWrapper(Context context, Data data) {
        super(context);
        mInputData = data;
    }

    @NonNull
    @Override
    public WorkRequest createRequest() {
        return new OneTimeWorkRequest.Builder(SendMessageWorker.class)
                                     .setConstraints(retrieveConstraint())
                                     .keepResultsForAtLeast(Duration.ofDays(1))
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
        private final FirebaseUser user;
        private final ChatDao dao;
        private final ChatRepositoryImpl chatRepository;
        private final StorageRepositoryImpl storageRepository;
        /**
         * @param appContext   The application {@link Context}
         * @param workerParams Parameters to setup the internal state of this worker
         */
        @AssistedInject
        public SendMessageWorker(@Assisted @NonNull Context appContext,
                                 @Assisted @NonNull WorkerParameters workerParams,
                                 ChatDao dao,
                                 ChatRepositoryImpl chatRepository,
                                 StorageRepositoryImpl storageRepository) {
            super(appContext, workerParams);
            this.user              = FirebaseAuth.getInstance().getCurrentUser();
            this.dao               = dao;
            this.chatRepository    = chatRepository;
            this.storageRepository = storageRepository;
        }

        @NonNull
        @Override
        public Single<Result> createWork() {
            return user == null ? Single.just(Result.failure()) : sendRequest();
        }

        private Single<Result> sendRequest() {
            Chat chat = dao.findById(getInputData().getString(EXTRA_MESSAGE_ID))
                           .subscribeOn(Schedulers.io())
                           .blockingGet();
            return preProcessingSending(chat);
        }

        private Single<Result> preProcessingSending(Chat chat) {
            if (MessageUtil.isMultiMediaMessage(chat)) {
                if (getInputData().getBoolean(EXTRA_FORWARD_MESSAGE, false)) {
                    return copyFileBeforeSend(chat);
                } else {
                    return uploadFileBeforeSend(chat);
                }
            } else {
                return sendMessage(chat);
            }
        }

        private Single<Result> copyFileBeforeSend(Chat chat) {
            return Observable.fromIterable(createPairForUrlPath(chat))
                             .flatMap(pair -> requestCopy(chat.getId(), pair.first, pair.second))
                             .toList()
                             .flatMap(list -> onResponseUploading(chat, list));
        }

        private Single<Result> uploadFileBeforeSend(Chat chat) {
            return Observable.fromIterable(createPair(chat))
                             .flatMap(pair -> uploadMedia(getApplicationContext(), chat, pair.first, pair.second))
                             .toList()
                             .flatMap(list -> {
                                 if (chat.getStatus() != MessageStatus.ERROR) return onResponseUploading(chat, list);
                                 else return Single.just(Result.failure());
                             });
        }

        private Single<Result> onResponseUploading(Chat chat, List<Chat.Media> list) {
            if (MessageUtil.isPhotoMessage(chat)) {
                chat.setPhotos(list.stream()
                                   .filter(p -> p.getUri() != null)
                                   .map(media -> new Chat.Photo(media.getUri()))
                                   .collect(Collectors.toList()));
            } else if (MessageUtil.isVideoMessage(chat)) {
                chat.setVideos(list.stream()
                                   .filter(p -> p.getUri() != null)
                                   .map(media -> new Chat.Video(media.getUri(), ((Chat.Video)media).getThumbnail()))
                                   .collect(Collectors.toList()));
            } else {
                chat.setFiles(list.stream()
                                  .filter(p -> p.getUri() != null)
                                  .map(media -> new Chat.File(media.getUri()))
                                  .collect(Collectors.toList()));
            }
            return dao.insert(chat)
                      .subscribeOn(Schedulers.io())
                      .andThen(sendMessage(chat))
                      .doOnError(t -> Logging.show("Can not send message, failure: " + t.getMessage()))
                      .onErrorReturnItem(Result.failure());
        }

        private <T extends Chat.Media> Observable<Chat.Media> uploadMedia(Context context, Chat chat, T raw, File file) {
            return preProcessingFileForUploading(context, chat, file)
                             .flatMap(compress -> {
                                 String conversationId = chat.getConversationId();
                                 String messageId      = chat.getId();

                                 if (MessageUtil.isPhotoMessage(chat)) {
                                     return storageRepository.uploadMessagePhoto(conversationId, messageId, compress);
                                 } else if (MessageUtil.isVideoMessage(chat)) {
                                     return storageRepository.uploadMessageVideo(conversationId, messageId, compress);
                                 } else {
                                     return Observable.error(new IllegalArgumentException());
                                 }
                             })
                             .compose(RxHelper.applyObservableSchedulers())
                             .compose(RxHelper.parseResponseData())
                             .map(result -> handleRequestFileSuccess(result, raw))
                             .onErrorReturn(t -> {
                                 handleUploadFileError(t);
                                 chat.setStatus(MessageStatus.ERROR);
                                 return raw;
                             });
        }

        private List<Pair<? extends Chat.Media, File>> createPair(Chat chat) {
            return convertToFiles(chat).stream()
                                       .map(file -> new Pair<>(convertToMedia(chat, new ForwardMessagePayload(file.getPath(), null, Type.FILE)), file))
                                       .filter(pair -> pair.first != null)
                                       .collect(Collectors.toList());
        }

        private List<Pair<? extends Chat.Media, ForwardMessagePayload>> createPairForUrlPath(Chat chat) {
            return convertToPayload(chat).stream()
                                         .map(payload -> new Pair<>(convertToMedia(chat, payload), payload))
                                         .filter(pair -> pair.first != null)
                                         .collect(Collectors.toList());
        }

        private Chat.Media convertToMedia(Chat chat, ForwardMessagePayload payload) {
            Chat.Media media = null;

            if (MessageUtil.isVideoMessage(chat)) {
                media = new Chat.Video(payload.getUrl(), payload.getThumbnail());
            } else if (MessageUtil.isPhotoMessage(chat)) {
                media = new Chat.Photo(payload.getUrl());
            } else if (MessageUtil.isFileMessage(chat)) {
                media = new Chat.File(payload.getUrl());
            }
            return media;
        }

        private Observable<File> preProcessingFileForUploading(Context context, Chat chat, File file) {
            if (MessageUtil.isVideoMessage(chat)) {
                if (file.length() >= MAX_VIDEO_SIZE) {
                    return Observable.error(FileTooLargeException::new);
                } else {
                    return Observable.just(file);
                }
            } else if (MessageUtil.isPhotoMessage(chat)) {
                if (file.length() >= MAX_PHOTO_SIZE) {
                    return Observable.fromFuture(FileProviderUtil.compressFileFutureForMessage(context, file));
                } else {
                    return Observable.just(file);
                }
            } else if (MessageUtil.isFileMessage(chat)) {
                if (file.length() >= MAX_DOCUMENT_SIZE) {
                    return Observable.error(FileTooLargeException::new);
                } else {
                    return Observable.just(file);
                }
            } else {
                return Observable.error(IllegalArgumentException::new);
            }
        }

        private <T extends Chat.Media> Chat.Media handleRequestFileSuccess(UploadResult result, T raw) {
            if (result.getType() == Type.IMAGE) {
                Chat.Photo photo = (Chat.Photo) raw;
                photo.setUri(result.getUrl());

                return photo;
            } else if (result.getType() == Type.VIDEO) {
                Chat.Video video = (Chat.Video) raw;
                video.setUri(result.getUrl());
                video.setThumbnail(result.getThumbnailUrl());

                return video;
            } else {
                Chat.File fileMedia = (Chat.File) raw;
                fileMedia.setUri(result.getUrl());

                return fileMedia;
            }
        }

        private Chat.Media handleUploadFileError(Throwable t) {
            return null;
        }

        private Observable<Chat.Media> requestCopy(String messageId, Chat.Media raw, ForwardMessagePayload requestPayload) {
            return storageRepository.requestCopyResource(messageId, requestPayload)
                                    .compose(RxHelper.applyObservableSchedulers())
                                    .compose(RxHelper.parseResponseData())
                                    .map(payload -> new UploadResult(payload.getUrl(), payload.getThumbnail(), "", "", 0L, payload.getType()))
                                    .map(result -> handleRequestFileSuccess(result, raw))
                                    .onErrorReturn(t -> {
                                        raw.setUri(null);
                                        return raw;
                                    });
        }

        private Single<Result> sendMessage(Chat chat) {
            if (LifecycleUtil.isAppForeground()) {
                WebSocketClient         webSocket = AppDependencies.getWebSocket();
                WebSocketRequestMessage request   = new WebSocketRequestMessage(new SecureRandom().nextLong(),
                                                                                WebSocketRequestMessage.Status.INCOMING_MESSAGE,
                                                                                chat,
                                                                                user.getUid());

                return webSocket.sendRequest(request)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .doOnError(t -> webSocket.notifyMessageError(request))
                                .flatMap(response -> {
                                    AppDependencies.getIncomingMessageProcessor().process(response);
                                    return Single.just(Result.success());
                                })
                                .onErrorReturnItem(Result.failure());
            } else {
                return UserTokenUtil.getTokenSingle(user)
                                    .flatMapObservable(token -> chatRepository.sendMessage(chat))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .compose(RxHelper.parseResponseData())
                                    .flatMapCompletable(chatRepository::saveCached)
                                    .toSingleDefault(chat)
                                    .map(c -> Result.success())
                                    .onErrorReturnItem(Result.failure());
            }
        }

        private List<File> convertToFiles(Chat chat) {
            if (MessageUtil.isPhotoMessage(chat)) {
                return chat.getPhotos()
                           .stream()
                           .map(p -> new File(p.getUri()))
                           .collect(Collectors.toList());
            } else if (MessageUtil.isVideoMessage(chat)) {
                return chat.getVideos()
                           .stream()
                           .map(p -> new File(p.getUri()))
                           .collect(Collectors.toList());
            } else if (MessageUtil.isFileMessage(chat)) {
                return chat.getFiles()
                           .stream()
                           .map(p -> new File(p.getUri()))
                           .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }

        private List<ForwardMessagePayload> convertToPayload(Chat chat) {
            return getMediaList(chat).stream()
                                     .map(media -> {
                                         ForwardMessagePayload payload = null;

                                         if (MessageUtil.isVideoMessage(chat)) {
                                             payload = new ForwardMessagePayload(media.getUri(), ((Chat.Video)media).getThumbnail(), Type.VIDEO);
                                         } else if (MessageUtil.isPhotoMessage(chat)) {
                                             payload = new ForwardMessagePayload(media.getUri(), null, Type.IMAGE);
                                         } else if (MessageUtil.isFileMessage(chat)) {
                                             payload = new ForwardMessagePayload(media.getUri(), null, Type.FILE);
                                         }

                                         return payload;
                                     })
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toList());
        }

        private List<? extends Chat.Media> getMediaList(Chat chat) {
            if (MessageUtil.isPhotoMessage(chat)) {
                return chat.getPhotos();
            } else if (MessageUtil.isVideoMessage(chat)) {
                return chat.getVideos();
            } else if (MessageUtil.isFileMessage(chat)) {
                return chat.getFiles();
            } else {
                return Collections.emptyList();
            }
        }
    }
}
