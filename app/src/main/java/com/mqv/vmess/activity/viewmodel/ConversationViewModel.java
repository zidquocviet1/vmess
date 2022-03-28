package com.mqv.vmess.activity.viewmodel;

import static com.mqv.vmess.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_PAGE_CHAT_LIST;
import static com.mqv.vmess.ui.fragment.viewmodel.ConversationFragmentViewModel.DEFAULT_SIZE_CHAT_LIST;

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
import com.mqv.vmess.R;
import com.mqv.vmess.activity.ConversationActivity;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.exception.NetworkException;
import com.mqv.vmess.network.exception.PermissionDeniedException;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationStatusType;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.network.model.type.MessageStatus;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.ConversationOptionHandler;
import com.mqv.vmess.ui.data.ConversationMapper;
import com.mqv.vmess.ui.data.ConversationMetadata;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Event;
import com.mqv.vmess.util.FileProviderUtil;
import com.mqv.vmess.util.LiveDataUtil;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper;
import com.mqv.vmess.work.SendMessageWorkWrapper;
import com.mqv.vmess.work.WorkDependency;

import java.io.File;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.internal.Preconditions;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

@HiltViewModel
public class ConversationViewModel extends AndroidViewModel {
    private final ConversationRepository                conversationRepository;
    private final ChatRepository                        chatRepository;
    private final UserRepository                        userRepository;
    private final PeopleRepository                      peopleRepository;
    private final FirebaseUser                          currentUser;
    private final MutableLiveData<User>                 userDetail;
    private final MutableLiveData<Result<List<Chat>>>   moreChatResult;
    private final MutableLiveData<Chat>                 messageObserver;
    private final MutableLiveData<Boolean>              scrollButtonState;
    private final MutableLiveData<Boolean>              conversationActiveStatus;
    private final MutableLiveData<ConversationMetadata> conversationMetadata;
    private final MutableLiveData<Result<Conversation>> conversationObserver;
    private final MutableLiveData<Result<Conversation>> singleRequestCall;
    private final MutableLiveData<Event<Integer>>       eventToast;
    private final DatabaseObserver.MessageListener      messageListener;
    private final CompositeDisposable                   cd;

    private Conversation mConversation;
    private int currentChatPage = DEFAULT_PAGE_CHAT_LIST;

    @Inject
    public ConversationViewModel(ConversationRepository repository,
                                 UserRepository userRepository,
                                 PeopleRepository peopleRepository,
                                 ChatRepository chatRepository,
                                 SavedStateHandle savedStateHandle,
                                 Application application) {
        super(application);

        this.conversationRepository   = repository;
        this.chatRepository           = chatRepository;
        this.userRepository           = userRepository;
        this.peopleRepository         = peopleRepository;
        this.currentUser              = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
        this.userDetail               = new MutableLiveData<>();
        this.moreChatResult           = new MutableLiveData<>();
        this.messageObserver          = new MutableLiveData<>();
        this.scrollButtonState        = new MutableLiveData<>(false);
        this.conversationActiveStatus = new MutableLiveData<>(false);
        this.conversationMetadata     = new MutableLiveData<>();
        this.conversationObserver     = new MutableLiveData<>();
        this.singleRequestCall        = new MutableLiveData<>();
        this.eventToast               = new MutableLiveData<>();
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

        String conversationId = savedStateHandle.get(ConversationActivity.EXTRA_CONVERSATION_ID);
        String participantId  = savedStateHandle.get(ConversationActivity.EXTRA_PARTICIPANT_ID);

        if (conversationId != null) {
            getCacheConversationById(conversationId);
        } else if (participantId != null) {
            getCacheConversationByParticipantId(participantId);
        } else {
            throw new IllegalArgumentException("Can't specific the conversation to fetch from local or remote");
        }
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
                    prev.getSeenBy().equals(cur.getSeenBy()) &&
                    prev.isUnsent() == cur.isUnsent();
        });
    }

    public LiveData<Boolean> getShowScrollButton() { return Transformations.distinctUntilChanged(scrollButtonState); }

    public LiveData<Boolean> getConversationActiveStatus() { return Transformations.distinctUntilChanged(conversationActiveStatus); }

    public LiveData<ConversationMetadata> getConversationMetadata() {
        return Transformations.distinctUntilChanged(conversationMetadata);
    }

    public LiveData<Result<Conversation>> getConversationObserver() {
        return conversationObserver;
    }

    public LiveData<Result<Conversation>> getSingleRequestCall() {
        return singleRequestCall;
    }

    public LiveData<Event<Integer>> getEventToast() {
        return eventToast;
    }

    //// Private method
    private void getCacheConversationById(String conversationId) {
        Disposable disposable = conversationRepository.fetchCachedById(conversationId)
                                                      .startWith(Completable.fromAction(() -> conversationObserver.postValue(Result.Loading())))
                                                      .singleOrError()
                                                      .zipWith(chatRepository.pagingCachedByConversation(conversationId, currentChatPage, Const.DEFAULT_CHAT_PAGING_SIZE), (c, messages) -> {
                                                          Collections.reverse(messages);
                                                          c.setChats(messages);

                                                          return c;
                                                      })
                                                      .onErrorResumeNext(t -> {
                                                          if (t instanceof EmptyResultSetException) {
                                                              return conversationRepository.fetchById(conversationId)
                                                                                           .compose(RxHelper.parseResponseData())
                                                                                           .map(c -> {
                                                                                               c.setStatus(ConversationStatusType.UNKNOWN);
                                                                                               return c;
                                                                                           })
                                                                                           .singleOrError();
                                                          }
                                                          return Single.error(new IllegalArgumentException());
                                                      })
                                                      .compose(RxHelper.applySingleSchedulers())
                                                      .subscribe(this::onLoadConversationComplete, this::onLoadConversationError);

        cd.add(disposable);
    }

    private void getCacheConversationByParticipantId(String participantId) {
        User user        = new User.Builder().setUid(this.currentUser.getUid()).create();
        User participant = new User.Builder().setUid(participantId).create();

        List<User> expectedParticipants = new ArrayList<>(){{
            add(user);
            add(participant);
        }};

        Disposable disposable = conversationRepository.fetchAllWithoutMessages()
                                                      .startWith(Completable.fromAction(() -> conversationObserver.postValue(Result.Loading())))
                                                      .singleOrError()
                                                      .map(list -> list.stream()
                                                                       .filter(c -> c.getType() == ConversationType.NORMAL &&
                                                                                    c.getParticipants().containsAll(expectedParticipants))
                                                                       .findFirst())
                                                      .flatMap(optional -> {
                                                          if (optional.isPresent()) {
                                                              return conversationRepository.fetchCachedById(optional.get().getId());
                                                          } else {
                                                              return conversationRepository.findNormalByParticipantId(participantId)
                                                                                           .compose(RxHelper.parseSingleResponseData())
                                                                                           .map(c -> {
                                                                                               c.setStatus(ConversationStatusType.UNKNOWN);
                                                                                               return c;
                                                                                           });
                                                          }
                                                      })
                                                      .compose(RxHelper.applySingleSchedulers())
                                                      .subscribe(this::onLoadConversationComplete, this::onLoadConversationError);
        cd.add(disposable);
    }

    private void onLoadConversationComplete(Conversation conversation) {
        mConversation = conversation;

        conversationRepository.save(conversation)
                              .subscribeOn(Schedulers.io())
                              .observeOn(Schedulers.io())
                              .doOnError(t -> Logging.show("Can't insert conversation, cause: " + t.getMessage()))
                              .doOnSubscribe(d -> Logging.show("Insert new conversation successfully"))
                              .onErrorComplete()
                              .subscribe();

        conversationObserver.postValue(Result.Success(conversation));

        setupConversationMetadata(conversation);

        AppDependencies.getDatabaseObserver().registerMessageListener(conversation.getId(), messageListener);
        AppDependencies.getDatabaseObserver().registerConversationListener(new DatabaseObserver.ConversationListener() {
            @Override
            public void onConversationInserted(@NonNull String conversationId) {
                // Default implementation
            }

            @Override
            public void onConversationUpdated(@NonNull String conversationId) {
                //noinspection ResultOfMethodCallIgnored
                conversationRepository.fetchCachedById(conversationId)
                                      .compose(RxHelper.applySingleSchedulers())
                                      .subscribe(c -> setupConversationMetadata(c), t -> {});
            }
        });

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

    private void onLoadConversationError(Throwable t) {
        t.printStackTrace();

        conversationObserver.postValue(Result.Fail(R.string.error_conversation_not_found));
    }

    private void fetchRemoteUser(@NonNull String uid) {
        Disposable userDisposable = userRepository.fetchUserFromRemote(uid)
                                                  .compose(RxHelper.parseResponseData())
                                                  .flatMapSingle(user -> {
                                                      People people = new People(user.getUid(),
                                                                                 user.getBiographic(),
                                                                                 user.getDisplayName(),
                                                                                 user.getPhotoUrl(),
                                                                                 user.getUsername(),
                                                                                 false,
                                                                                 user.getAccessedDate());
                                                      return peopleRepository.save(people)
                                                                             .subscribeOn(Schedulers.io())
                                                                             .observeOn(AndroidSchedulers.mainThread())
                                                                             .toSingleDefault(user);
                                                  })
                                                  .compose(RxHelper.applyObservableSchedulers())
                                                  .subscribe(userDetail::postValue, t -> Logging.show("Fetch user remote fails. Cause: " + t.getMessage()));

        cd.add(userDisposable);
    }

    private void loadMoreChatRemote(Conversation conversation) {
        Disposable disposable = chatRepository.loadMoreChat(conversation.getId(), currentChatPage + 1, DEFAULT_SIZE_CHAT_LIST)
                                              .compose(RxHelper.applyObservableSchedulers())
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
                                              .compose(RxHelper.applySingleSchedulers())
                                              .onErrorComplete()
                                              .subscribe(messageObserver::postValue);
        cd.add(disposable);
    }

    private void setupConversationMetadata(Conversation conversation) {
        User                 currentUser = LoggedInUserManager.getInstance().parseFirebaseUser(this.currentUser);
        ConversationMetadata metadata    = ConversationMapper.mapToMetadata(conversation,
                                                                            currentUser,
                                                                            getApplication().getApplicationContext());

        conversationMetadata.postValue(metadata);
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

    private void onGroupOptionChangedComplete(Observable<ApiResponse<Conversation>> observable) {
        Disposable disposable = observable.startWith(Completable.fromAction(() -> singleRequestCall.postValue(Result.Loading())))
                                          .compose(RxHelper.parseResponseData())
                                          .flatMapSingle(c -> {
                                              c.setStatus(ConversationStatusType.INBOX);

                                              singleRequestCall.postValue(Result.Success(c));
                                              setupConversationMetadata(c);

                                              return conversationRepository.save(c).toSingleDefault(c.getChats());
                                          })
                                          .compose(RxHelper.applyObservableSchedulers())
                                          .subscribe(list -> {
                                              if (list.size() == 1) {
                                                  Chat message = list.get(0);
                                                  AppDependencies.getDatabaseObserver().notifyMessageInserted(message.getConversationId(), message.getId());
                                              }
                                          }, t -> {
                                              if (t instanceof ConnectException) {
                                                  eventToast.postValue(new Event<>(R.string.error_connect_server_fail));
                                              } else if (t instanceof FirebaseNetworkException) {
                                                  eventToast.postValue(new Event<>(R.string.error_network_connection));
                                              } else if (t instanceof PermissionDeniedException) {
                                                  eventToast.postValue(new Event<>(R.string.msg_user_dont_allow_added));
                                              } else {
                                                  eventToast.postValue(new Event<>(R.string.error_unknown));
                                              }
                                              singleRequestCall.postValue(Result.Fail(R.string.msg_permission_denied));
                                          });

        cd.add(disposable);
    }

    //// Public method
    public void sendMessage(Context context, Chat chat) {
        Disposable disposable = conversationRepository.isExists(chat.getConversationId())
                                                      .flatMapCompletable(isExists -> isExists ? chatRepository.saveCached(chat) : Completable.error(new EmptyResultSetException("empty")))
                                                      .compose(RxHelper.applyCompleteSchedulers())
                                                      .subscribe(() -> {
                                                          messageObserver.postValue(chat);
                                                          AppDependencies.getDatabaseObserver().notifyConversationUpdated(chat.getConversationId());
                                                      }, t -> {
                                                          if (t instanceof EmptyResultSetException) {
                                                              eventToast.postValue(new Event<>(R.string.error_conversation_has_been_deleted));
                                                          }
                                                      });

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
                                                .compose(RxHelper.applySingleSchedulers())
                                                .subscribe(people -> {
                                                    User user = new User.Builder()
                                                                        .setUid(people.getUid())
                                                                        .setDisplayName(people.getDisplayName())
                                                                        .setBiographic(people.getBiographic())
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

    public void seenDummyMessage(List<Chat> messages) {
        Observable.fromIterable(messages)
                  .flatMapSingle(c -> Completable.fromAction(() -> chatRepository.updateCached(c)).toSingleDefault(c))
                  .flatMap(chatRepository::seenWelcomeMessage)
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

    public void registerLoadMore(Conversation conversation) {
        Preconditions.checkNotNull(conversation);

        Disposable cachedDisposable = chatRepository.pagingCachedByConversation(conversation.getId(), currentChatPage + 1, Const.DEFAULT_CHAT_PAGING_SIZE)
                                                    .compose(RxHelper.applySingleSchedulers())
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

    public void changeGroupName(String groupName) {
        onGroupOptionChangedComplete(conversationRepository.changeConversationGroupName(mConversation.getId(), groupName));
    }

    public void addGroupMember(List<String> memberIds) {
        onGroupOptionChangedComplete(Observable.fromIterable(memberIds)
                                               .flatMap(memberId -> conversationRepository.addGroupMember(mConversation.getId(), memberId)));
    }

    public void removeGroupMember(String memberId) {
        onGroupOptionChangedComplete(conversationRepository.removeGroupMember(mConversation.getId(), memberId));
    }

    public void leaveGroup() {
        Disposable disposable = ConversationOptionHandler.leaveGroup(conversationRepository, mConversation.getId())
                                                         .compose(RxHelper.applyObservableSchedulers())
                                                         .subscribe(singleRequestCall::postValue, t -> singleRequestCall.postValue(Result.Fail(-1)));
        cd.add(disposable);
    }

    public void changeGroupThumbnail(Context context, File file) {
        onGroupOptionChangedComplete(Observable.fromFuture(FileProviderUtil.compressFileFuture(context, file))
                                               .flatMap(compress -> conversationRepository.changeConversationGroupThumbnail(mConversation.getId(), compress)));
    }

    public void unsentMessage(Chat message) {
        Disposable disposable = conversationRepository.isExists(message.getConversationId())
                                                      .startWith(Completable.fromAction(() -> singleRequestCall.postValue(Result.Loading())))
                                                      .flatMapCompletable(isExists -> isExists ?
                                                              Completable.fromAction(() -> chatRepository.updateCached(message)) :
                                                              Completable.error(new EmptyResultSetException("empty")))
                                                      .andThen(chatRepository.unsentMessage(message))
                                                      .compose(RxHelper.parseResponseData())
                                                      .flatMapSingle(c -> chatRepository.saveCached(c).toSingleDefault(c))
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .subscribe(data -> {
                                                          messageObserver.setValue(data);
                                                          AppDependencies.getDatabaseObserver().notifyConversationUpdated(data.getConversationId());
                                                          singleRequestCall.postValue(Result.Success(null));
                                                      }, t -> {
                                                          if (t instanceof EmptyResultSetException) {
                                                              eventToast.postValue(new Event<>(R.string.error_conversation_has_been_deleted));
                                                          } else if (t instanceof NetworkException) {
                                                              eventToast.postValue(new Event<>(((NetworkException)t).getStringRes()));
                                                          }
                                                          singleRequestCall.postValue(Result.Fail(-1));
                                                      });
        cd.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        AppDependencies.getDatabaseObserver().unregisterMessageListener(messageListener);
    }
}
