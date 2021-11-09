package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_PAGE_CHAT_LIST;
import static com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_SIZE_CHAT_LIST;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.rxjava3.EmptyResultSetException;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.MessageStatus;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.internal.Preconditions;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

@HiltViewModel
public class ConversationViewModel extends CurrentUserViewModel {
    private final ConversationRepository                        repository;
    private final UserRepository                                userRepository;
    private final PeopleRepository                              peopleRepository;
    private final MutableLiveData<Pair<Chat, Chat>>             sendMessageStatus;
    private final MutableLiveData<User>                         userDetail;
    private final MutableLiveData<Chat>                         seenMessageResult;
    private final MutableLiveData<Boolean>                      serverReadyResult;
    private final MutableLiveData<Result<List<Chat>>>           moreChatResult;

    private final Executor                                      sendMessageExecutors = Executors.newFixedThreadPool(3);
    private final Executor                                      seenMessageExecutors = Executors.newFixedThreadPool(5);
    private int                                                 currentChatPage      = DEFAULT_PAGE_CHAT_LIST;

    @Inject
    public ConversationViewModel(ConversationRepository repository,
                                 UserRepository userRepository,
                                 PeopleRepository peopleRepository) {
        this.repository         = repository;
        this.userRepository     = userRepository;
        this.peopleRepository   = peopleRepository;
        this.sendMessageStatus  = new MutableLiveData<>();
        this.userDetail         = new MutableLiveData<>();
        this.seenMessageResult  = new MutableLiveData<>();
        this.serverReadyResult  = new MutableLiveData<>();
        this.moreChatResult     = new MutableLiveData<>();
    }

    public LiveData<Pair<Chat, Chat>> getSendMessage() {
        return sendMessageStatus;
    }

    public LiveData<User> getUserDetail() {
        return userDetail;
    }

    public LiveData<Chat> getSeenMessageResult() {
        return seenMessageResult;
    }

    public LiveData<Boolean> getServerReadyResult() {
        return serverReadyResult;
    }

    public LiveData<Result<List<Chat>>> getMoreChatResult() {
        return moreChatResult;
    }

    public void resetSendMessageResult() {
        sendMessageStatus.setValue(null);
    }

    public void sendMessage(@NonNull Chat chat) {
        Disposable disposable = repository.sendMessage(chat)
                                          .subscribeOn(Schedulers.from(sendMessageExecutors))
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .doOnSubscribe(d -> handleSendMessageStatus(chat, MessageStatus.NOT_RECEIVED))
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  Chat freshChat = response.getSuccess();

                                                  sendMessageStatus.setValue(new Pair<>(chat, freshChat));
                                              } else {
                                                  handleSendMessageStatus(chat, MessageStatus.ERROR);
                                              }
                                          }, t -> handleSendMessageStatus(chat, MessageStatus.ERROR));

        cd.add(disposable);
    }

    private void handleSendMessageStatus(Chat oldChat, MessageStatus status) {
        oldChat.setStatus(status);
        sendMessageStatus.setValue(new Pair<>(oldChat, oldChat));
    }

    public void loadUserDetail(@NonNull String uid) {
        /*
        * check the friend list from cached. If it present push it to UI. If not make a remote call.
        * */
        Disposable disposable = peopleRepository.getCachedByUid(uid)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(people -> {
                                                    User user = new User.Builder()
                                                                        .setUid(people.getUid())
                                                                        .setDisplayName(people.getDisplayName())
                                                                        .setPhotoUrl(people.getPhotoUrl())
                                                                        .setUsername(people.getUsername())
                                                                        .create();

                                                    userDetail.setValue(user);
                                                }, t -> {
                                                    if (t instanceof EmptyResultSetException){
                                                        Logging.show("The current user is not have friend relationship." +
                                                                " Make a remote call to get information");

                                                        fetchRemoteUser(uid);
                                                    } else {
                                                        t.printStackTrace();
                                                    }
                                                });

        cd.add(disposable);
    }

    private void fetchRemoteUser(@NonNull String uid) {
        Disposable userDisposable = userRepository.fetchUserFromRemote(uid)
                                                  .subscribeOn(Schedulers.io())
                                                  .observeOn(AndroidSchedulers.mainThread())
                                                  .subscribe(response -> {
                                                      if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                          userDetail.setValue(response.getSuccess());
                                                      }
                                                  }, t -> Logging.show("Fetch user remote fails."));

        cd.add(userDisposable);
    }

    public void updateChat(Chat chat) {
        repository.updateChat(chat);
    }

    public void saveChat(Chat chat) {
        repository.saveChat(Collections.singletonList(chat));
    }

    public void seenMessage(List<Chat> chats) {
        Disposable disposable = Observable.fromIterable(chats)
                                          .flatMap(repository::seenMessage)
                                          .subscribeOn(Schedulers.from(seenMessageExecutors))
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  Chat chat = response.getSuccess();

                                                  seenMessageResult.setValue(chat);
                                              }
                                          }, t -> {});

        cd.add(disposable);
    }

    public void seenWelcomeMessage(Chat chat) {
        updateChat(chat);

        repository.seenWelcomeMessage(chat)
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.io())
                  .subscribe();
    }

    public void isServerReadyForFirestoreSubscribe() {
        Disposable disposable = repository.isServerAlive()
                                          .subscribeOn(Schedulers.io())
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  serverReadyResult.setValue(Boolean.TRUE);
                                              } else {
                                                  serverReadyResult.setValue(Boolean.FALSE);
                                              }
                                          }, t -> serverReadyResult.setValue(Boolean.FALSE));

        cd.add(disposable);
    }

    public void registerLoadMore(Conversation conversation) {
        Preconditions.checkNotNull(conversation);

        Disposable cachedDisposable = repository.fetchChatByConversation(conversation.getId(), currentChatPage + 1, DEFAULT_SIZE_CHAT_LIST)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .doOnSubscribe(d -> {
                                                    if (!d.isDisposed())
                                                        moreChatResult.setValue(Result.Loading());
                                                })
                                                .subscribe(cacheData -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    Collections.reverse(cacheData);

                                                    moreChatResult.setValue(Result.Success(cacheData));

                                                    currentChatPage += 1;
                                                }, 400), t -> loadMoreChatRemote(conversation));
        cd.add(cachedDisposable);
    }

    private void loadMoreChatRemote(Conversation conversation) {
        Disposable disposable = repository.loadMoreChat(conversation.getId(), currentChatPage + 1, DEFAULT_SIZE_CHAT_LIST)
                                          .subscribeOn(Schedulers.io())
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .onErrorResumeNext(throwable2 -> {
                                              if (throwable2 instanceof HttpException) {
                                                  if (((HttpException)throwable2).code() == HttpURLConnection.HTTP_NOT_FOUND)
                                                      return Observable.error(new Exception("The conversation is not exists"));
                                                  else
                                                      return Observable.error(new Exception("Unknown Error"));
                                              } else {
                                                  return Observable.error(throwable2);
                                              }
                                          })
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  List<Chat> freshData = response.getSuccess();

                                                  Collections.reverse(freshData);

                                                  moreChatResult.setValue(Result.Success(freshData));

                                                  currentChatPage += 1;

                                                  repository.saveChat(freshData);
                                              } else if (response.getStatusCode() == HttpURLConnection.HTTP_CONFLICT) {
                                                  handleLoadChatError(R.string.error_user_not_in_conversation);
                                              }
                                          }, t -> {
                                              Logging.show("Load more chats error: " + t.getMessage());

                                              if (t instanceof FirebaseNetworkException) {
                                                  handleLoadChatError(R.string.error_network_connection);
                                              } else if (t instanceof ConnectException || t instanceof SocketTimeoutException){
                                                  handleLoadChatError(R.string.error_connect_server_fail);
                                              } else {
                                                  handleLoadChatError(-1);
                                              }
                                          });

        cd.add(disposable);
    }

    private void handleLoadChatError(int error) {
        moreChatResult.setValue(Result.Fail(error));
    }
}
