package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_PAGE_CHAT_LIST;
import static com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_SIZE_CHAT_LIST;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.room.rxjava3.EmptyResultSetException;
import androidx.work.Data;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.DatabaseObserver;
import com.mqv.realtimechatapplication.data.repository.ChatRepository;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.MessageStatus;
import com.mqv.realtimechatapplication.ui.data.ConversationMapper;
import com.mqv.realtimechatapplication.ui.data.ConversationMetadata;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.LiveDataUtil;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.work.PushMessageAcknowledgeWorkWrapper;
import com.mqv.realtimechatapplication.work.SendMessageWorkWrapper;
import com.mqv.realtimechatapplication.work.WorkDependency;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.internal.Preconditions;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

@HiltViewModel
public class ConversationViewModel extends AndroidViewModel {
    private final ChatRepository                        chatRepository;
    private final UserRepository                        userRepository;
    private final PeopleRepository                      peopleRepository;
    private final MutableLiveData<User>                 userDetail;
    private final MutableLiveData<Result<List<Chat>>>   moreChatResult;
    private final MutableLiveData<List<Chat>>           cacheChatResult;
    private final MutableLiveData<Chat>                 messageObserver;
    private final MutableLiveData<Boolean>              scrollButtonState;
    private final MutableLiveData<Boolean>              conversationActiveStatus;
    private final MutableLiveData<ConversationMetadata> conversationMetadata;
    private final DatabaseObserver.MessageListener      messageListener;
    private final CompositeDisposable                   cd;

    private int currentChatPage = DEFAULT_PAGE_CHAT_LIST;

    public static final String KEY_CONVERSATION_ID = "conversation";

    @Inject
    public ConversationViewModel(ConversationRepository repository,
                                 UserRepository userRepository,
                                 PeopleRepository peopleRepository,
                                 ChatRepository chatRepository,
                                 SavedStateHandle savedStateHandle,
                                 Application application) {
        super(application);

        this.chatRepository           = chatRepository;
        this.userRepository           = userRepository;
        this.peopleRepository         = peopleRepository;
        this.userDetail               = new MutableLiveData<>();
        this.moreChatResult           = new MutableLiveData<>();
        this.cacheChatResult          = new MutableLiveData<>(Collections.emptyList());
        this.messageObserver          = new MutableLiveData<>();
        this.scrollButtonState        = new MutableLiveData<>(false);
        this.conversationActiveStatus = new MutableLiveData<>(false);
        this.conversationMetadata     = new MutableLiveData<>();
        this.cd                       = new CompositeDisposable();
        this.messageListener          = new DatabaseObserver.MessageListener() {
            @Override
            public void onMessageInserted(@NonNull String messageId) {
                getCacheMessage(messageId);
            }

            @Override
            public void onMessageUpdated(@NonNull String messageId) {
                getCacheMessage(messageId);
            }
        };

        Conversation conversation = savedStateHandle.get(KEY_CONVERSATION_ID);

        if (conversation == null) {
            throw new IllegalArgumentException("Conversation can't be null");
        }
        setupConversationName(conversation);

        cd.add(chatRepository.pagingCachedByConversation(conversation.getId(),
                                                         DEFAULT_PAGE_CHAT_LIST,
                                                         Const.DEFAULT_CHAT_PAGING_SIZE)
                             .subscribeOn(Schedulers.io())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(list -> {
                                 Collections.reverse(list);
                                 cacheChatResult.postValue(list);
                             }, t -> {}));

        AppDependencies.getDatabaseObserver().registerMessageListener(conversation.getId(), messageListener);

        List<String> participants = conversation.getParticipants()
                                                .stream()
                                                .map(User::getUid)
                                                .collect(Collectors.toList());

        Disposable disposable = AppDependencies.getWebSocket()
                                               .getPresenceUserList()
                                               .onErrorComplete()
                                               .map(list -> !list.isEmpty() && !Collections.disjoint(new HashSet<>(list), participants))
                                               .subscribe(conversationActiveStatus::postValue);
        cd.add(disposable);
    }

    //// Getter
    public LiveData<User> getUserDetail() {
        return userDetail;
    }

    public LiveData<Result<List<Chat>>> getMoreChatResult() {
        return Transformations.distinctUntilChanged(moreChatResult);
    }

    public LiveData<Chat> getMessageObserver() {
        return LiveDataUtil.distinctUntilChanged(messageObserver, (prev, cur) -> {
            if (prev == cur) return true;
            if (cur == null || prev.getClass() != cur.getClass()) return false;

            return  prev.getId().equals(cur.getId()) &&
                    prev.getSenderId().equals(cur.getSenderId()) &&
                    prev.getTimestamp().equals(cur.getTimestamp()) &&
                    prev.getContent().equals(cur.getContent()) &&
                    prev.getType().equals(cur.getType()) &&
                    prev.getStatus().equals(cur.getStatus()) &&
                    prev.getSeenBy().equals(cur.getSeenBy());
        });
    }

    public LiveData<Boolean> getShowScrollButton() { return Transformations.distinctUntilChanged(scrollButtonState); }

    public LiveData<Boolean> getConversationActiveStatus() { return Transformations.distinctUntilChanged(conversationActiveStatus); }

    public LiveData<List<Chat>> getCacheChats() {
        return cacheChatResult;
    }

    public LiveData<ConversationMetadata> getConversationMetadata() {
        return Transformations.distinctUntilChanged(conversationMetadata);
    }

    //// Private method
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

    private void loadMoreChatRemote(Conversation conversation) {
        Disposable disposable = chatRepository.loadMoreChat(conversation.getId(), currentChatPage + 1, DEFAULT_SIZE_CHAT_LIST)
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

                                                      chatRepository.saveCached(freshData);
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

    private void getCacheMessage(String messageId) {
        Disposable disposable = chatRepository.fetchCached(messageId)
                                              .subscribeOn(Schedulers.io())
                                              .observeOn(AndroidSchedulers.mainThread())
                                              .onErrorComplete()
                                              .subscribe(messageObserver::postValue);
        cd.add(disposable);
    }

    private void setupConversationName(Conversation conversation) {
        FirebaseUser         firebaseUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
        User                 currentUser  = LoggedInUserManager.getInstance().parseFirebaseUser(firebaseUser);
        ConversationMetadata metadata     = ConversationMapper.mapToMetadata(conversation,
                                                                             currentUser,
                                                                             getApplication().getApplicationContext());

        conversationMetadata.postValue(metadata);
    }
    //// Public method
    public void sendMessage(Context context, Chat chat) {
        Disposable disposable = chatRepository.saveCached(chat)
                                              .subscribeOn(Schedulers.io())
                                              .observeOn(AndroidSchedulers.mainThread())
                                              .subscribe(() -> {
                                                  messageObserver.postValue(chat);
                                                  AppDependencies.getDatabaseObserver().notifyConversationUpdated(chat.getConversationId());
                                              }, t -> {});

        cd.add(disposable);

        Data input = new Data.Builder()
                             .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_ID, chat.getId())
                             .putString(SendMessageWorkWrapper.EXTRA_SENDER_ID, chat.getSenderId())
                             .putString(SendMessageWorkWrapper.EXTRA_CONTENT, chat.getContent())
                             .putString(SendMessageWorkWrapper.EXTRA_CONVERSATION_ID, chat.getConversationId())
                             .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_TYPE, chat.getType().name())
                             .build();

        WorkDependency.enqueue(new SendMessageWorkWrapper(context, input));
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

    public void updateChat(Chat chat) {
        chatRepository.updateCached(chat);
    }

    public void seenWelcomeMessage(Chat chat) {
        updateChat(chat);

        chatRepository.seenWelcomeMessage(chat)
                      .subscribeOn(Schedulers.io())
                      .observeOn(Schedulers.io())
                      .onErrorComplete()
                      .subscribe();
    }

    public void postSeenMessageConversation(Context context, String conversationId, String userId) {
        //noinspection ResultOfMethodCallIgnored
        chatRepository.fetchUnreadChatByConversation(conversationId)
                      .subscribeOn(Schedulers.io())
                      .observeOn(Schedulers.io())
                      .flattenAsObservable(list -> list)
                      .map(c -> updateAndReturn(c, userId))
                      .toList()
                      .subscribe((list, t) -> sendSeenMessage(context, list, conversationId));
    }

    private String updateAndReturn(Chat chat, String userId) {
        chat.setStatus(MessageStatus.SEEN);
        chat.getSeenBy().add(userId);

        updateChat(chat);

        return chat.getId();
    }

    private void sendSeenMessage(Context context, List<String> ids, String conversationId) {
        if (!ids.isEmpty()) {
            Data data = new Data.Builder()
                                .putStringArray(PushMessageAcknowledgeWorkWrapper.EXTRA_LIST_MESSAGE_ID, ids.toArray(new String[0]))
                                .putBoolean(PushMessageAcknowledgeWorkWrapper.EXTRA_MARK_AS_READ, true)
                                .build();
            WorkDependency.enqueue(new PushMessageAcknowledgeWorkWrapper(context, data));

            AppDependencies.getDatabaseObserver().notifyConversationUpdated(conversationId);
        } else {
            Logging.show("No need to push seen messages, because the list unread message is empty");
        }
    }

    public void registerLoadMore(Conversation conversation) {
        Preconditions.checkNotNull(conversation);

        Disposable cachedDisposable = chatRepository.pagingCachedByConversation(conversation.getId(), currentChatPage + 1, Const.DEFAULT_CHAT_PAGING_SIZE)
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

    public void setScrollButtonState(boolean state) {
        scrollButtonState.setValue(state);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        AppDependencies.getDatabaseObserver().unregisterMessageListener(messageListener);
    }
}
