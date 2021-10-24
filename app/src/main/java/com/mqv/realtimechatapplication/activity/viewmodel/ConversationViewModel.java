package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.rxjava3.EmptyResultSetException;

import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.MessageStatus;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationViewModel extends CurrentUserViewModel {
    private final ConversationRepository                        repository;
    private final UserRepository                                userRepository;
    private final PeopleRepository                              peopleRepository;
    private final MutableLiveData<Pair<Chat, Chat>>             sendMessageStatus;
    private final MutableLiveData<User>                         userDetail;

    private final Executor                                      sendMessageExecutors = Executors.newFixedThreadPool(3);

    @Inject
    public ConversationViewModel(ConversationRepository repository,
                                 UserRepository userRepository,
                                 PeopleRepository peopleRepository) {
        this.repository         = repository;
        this.userRepository     = userRepository;
        this.peopleRepository   = peopleRepository;
        this.sendMessageStatus  = new MutableLiveData<>();
        this.userDetail         = new MutableLiveData<>();
    }

    public LiveData<Pair<Chat, Chat>> getSendMessage() {
        return sendMessageStatus;
    }

    public LiveData<User> getUserDetail() {
        return userDetail;
    }

    public void resetSendMessageResult() {
        sendMessageStatus.setValue(null);
    }

    public void sendMessage(@NonNull Chat chat) {
        Disposable disposable = repository.sendMessage(chat)
                                          .subscribeOn(Schedulers.from(sendMessageExecutors))
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(response -> {
                                              if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                  Chat freshChat = response.getSuccess();

                                                  sendMessageStatus.setValue(new Pair<>(chat, freshChat));
                                              } else {
                                                  handleSendMessageError(chat);
                                              }
                                          }, t -> handleSendMessageError(chat));

        cd.add(disposable);
    }

    private void handleSendMessageError(Chat oldChat) {
        oldChat.setStatus(MessageStatus.ERROR);
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

    /*
    * Update the conversation with some new chats are added.
    * */
    public void updateConversation(Conversation conversation) {
        repository.updateConversation(conversation)
                  .subscribeOn(Schedulers.io())
                  .subscribe(new DisposableCompletableObserver() {
                      @Override
                      public void onComplete() {

                      }

                      @Override
                      public void onError(@NonNull Throwable e) {
                          e.printStackTrace();
                      }
                  });
    }
}
