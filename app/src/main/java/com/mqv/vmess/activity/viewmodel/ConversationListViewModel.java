package com.mqv.vmess.activity.viewmodel;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.vmess.R;
import com.mqv.vmess.data.model.ConversationNotificationOption;
import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.exception.PermissionDeniedException;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.ConversationGroup;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationStatusType;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.ConversationOptionHandler;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Event;
import com.mqv.vmess.util.LiveDataUtil;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Retriever;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationListViewModel extends MessageHandlerViewModel {
    private   final ConversationRepository                                conversationRepository;
    private   final ConversationStatusType                                statusType;
    protected final MutableLiveData<List<Conversation>>                   conversationListObserver;
    protected final MutableLiveData<List<String>>                         presenceUserListObserver;
    protected final MutableLiveData<List<ConversationNotificationOption>> notificationOptionObserver;
    protected final MutableLiveData<Pair<Boolean, Integer>>               oneTimeLoadingResult;
    protected final MutableLiveData<Event<Integer>>                       errorEmitter;
    protected final MutableLiveData<Event<Result<List<Conversation>>>>    pagingResult;
    protected final MutableLiveData<List<User>>                           userLeftGroup;
    protected final CompositeDisposable                                   cd;

    private static final String TAG = ConversationListViewModel.class.getSimpleName();

    private static final int INITIALIZE_PAGE = 0;

    private int        currentPage = 0;
    private boolean    canLoadMore = true;
    private Disposable conversationDisposable;

    public ConversationListViewModel(Application application,
                                     ConversationRepository conversationRepository,
                                     ChatRepository chatRepository,
                                     PeopleRepository peopleRepository,
                                     FriendRequestRepository friendRequestRepository,
                                     ConversationStatusType status) {
        super(application, conversationRepository, chatRepository, peopleRepository, friendRequestRepository);

        this.conversationRepository     = conversationRepository;
        this.statusType                 = status;
        this.cd                         = new CompositeDisposable();
        this.conversationListObserver   = new MutableLiveData<>(AppDependencies.getMemoryManager().getConversationListCache());
        this.presenceUserListObserver   = new MutableLiveData<>(Collections.emptyList());
        this.oneTimeLoadingResult       = new MutableLiveData<>();
        this.errorEmitter               = new MutableLiveData<>();
        this.pagingResult               = new MutableLiveData<>();
        this.notificationOptionObserver = new MutableLiveData<>();
        this.userLeftGroup              = new MutableLiveData<>(Collections.emptyList());

        registerConversationListObserver(status, Const.DEFAULT_CONVERSATION_PAGING_SIZE);

        //noinspection ResultOfMethodCallIgnored
        conversationRepository.observeNotificationOption()
                              .compose(RxHelper.applyFlowableSchedulers())
                              .subscribe(notificationOptionObserver::postValue);

        //noinspection ResultOfMethodCallIgnored
        AppDependencies.getWebSocket()
                       .getPresenceUserList()
                       .onErrorComplete()
                       .subscribe(presenceUserListObserver::postValue);

        fetchAllMuteNotification();
        setupConversationParticipants(userLeftGroup::postValue);
    }

    private void registerConversationListObserver(ConversationStatusType status, int size) {
        conversationDisposable = conversationRepository.conversationAndLastChat(status, size)
                                                       .compose(RxHelper.applyFlowableSchedulers())
                                                       .map(this::mapToListConversation)
                                                       .subscribe(conversationListObserver::postValue);
    }

    public LiveData<Event<Integer>> getOneTimeErrorObserver() {
        return errorEmitter;
    }

    public LiveData<Pair<Boolean, Integer>> getOneTimeLoadingResult() {
        return oneTimeLoadingResult;
    }

    public LiveData<Event<Result<List<Conversation>>>> getPagingResult() {
        return pagingResult;
    }

    public LiveData<List<ConversationNotificationOption>> getConversationNotificationOption() {
        return notificationOptionObserver;
    }

    public LiveData<List<String>> getPresenceUserListObserverDistinct() {
        return LiveDataUtil.distinctUntilChanged(presenceUserListObserver, (oldList, newList) -> {
            if (newList == null) return false;
            if (oldList.isEmpty() && !newList.isEmpty()) return false;
            if (!oldList.isEmpty() && newList.isEmpty()) return false;

            return newList.containsAll(oldList) && oldList.containsAll(newList);
        });
    }

    public LiveData<List<User>> getUserLeftGroup() {
        return userLeftGroup;
    }

    public List<String> getPresenceUserList() {
        return Retriever.getOrDefault(presenceUserListObserver.getValue(), Collections.emptyList());
    }

    protected List<Conversation> mapToListConversation(Map<Conversation, Chat> map) {
        Iterator<Map.Entry<Conversation, Chat>> iterator = map.entrySet().iterator();
        List<Conversation>                      result   = new LinkedList<>();

        while (iterator.hasNext()) {
            Map.Entry<Conversation, Chat> entry        = iterator.next();
            Conversation                  conversation = entry.getKey();

            conversation.setChats(Collections.singletonList(entry.getValue()));
            result.add(conversation);
        }
        return result;
    }

    // Only call with the activity has swipe refresh layout
    public Observable<ApiResponse<List<Conversation>>> onRefresh(ConversationStatusType type) {
        return conversationRepository.fetchByUid(type, currentPage, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                     .compose(RxHelper.applyObservableSchedulers());
    }

    public void emitterOneTimeErrorToast(int errorRes) {
        errorEmitter.postValue(new Event<>(errorRes));
    }

    protected void initializeFetch(ConversationStatusType type,
                                   Consumer<List<Conversation>> onReceiveData,
                                   Consumer<Throwable> onError) {
        Runnable onDataChanged = () -> {
            Disposable cacheDisposable = conversationRepository.fetchCached(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                                               .compose(RxHelper.applySingleSchedulers())
                                                               .subscribe(onReceiveData, onError);
            cd.add(cacheDisposable);
        };

        Disposable disposable = conversationRepository.fetchByUidNBR(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE, onDataChanged)
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .subscribe(onReceiveData, onError);

        cd.add(disposable);
    }

    // Save new refresh conversation list and notify on data save successfully
    protected void saveCallResult(List<Conversation> conversations, ConversationStatusType type, Action onSaveSuccess) {
        Disposable disposable = conversationRepository.saveAll(conversations, type)
                                                      .compose(RxHelper.applyCompleteSchedulers())
                                                      .subscribe(onSaveSuccess);
        cd.add(disposable);
    }

    protected void fetchAllMuteNotification() {
        Disposable disposable = conversationRepository.getAllMuteNotification()
                                                      .compose(RxHelper.parseResponseData())
                                                      .flatMapCompletable(option -> {
                                                          List<ConversationNotificationOption> notificationOptions = option.stream()
                                                                                                                           .map(ConversationNotificationOption::fromConversationOption)
                                                                                                                           .collect(Collectors.toList());
                                                          return conversationRepository.insertNotificationOption(notificationOptions);
                                                      })
                                                      .compose(RxHelper.applyCompleteSchedulers())
                                                      .onErrorComplete()
                                                      .subscribe();
        cd.add(disposable);
    }

    public void delete(Conversation conversation) {
        conversationRepository.deleteConversationChatRemote(conversation);
    }

    public void changeConversationStatusType(Conversation conversation, ConversationStatusType type) {
        conversation.setStatus(type);

        conversationRepository.changeConversationStatus(conversation)
                              .andThen(conversationRepository.changeConversationStatusRemote(conversation.getId(), type.ordinal()))
                              .subscribeOn(Schedulers.io())
                              .observeOn(Schedulers.io())
                              .onErrorComplete()
                              .subscribe();
    }

    public void muteNotification(Conversation conversation, long until) {
        long                                 currentMillis = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        long                                 muteUntil     = Long.MAX_VALUE == until ? until : currentMillis + until;
        List<ConversationNotificationOption> option        = Collections.singletonList(new ConversationNotificationOption(null, conversation.getId(), muteUntil, LocalDateTime.now()));

        Disposable disposable = conversationRepository.mute(conversation.getId(), muteUntil)
                                                      .startWith(conversationRepository.insertNotificationOption(option))
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .map(data -> Collections.singletonList(ConversationNotificationOption.fromConversationOption(data)))
                                                      .flatMapCompletable(conversationRepository::insertNotificationOption)
                                                      .onErrorComplete()
                                                      .subscribe();

        cd.add(disposable);
    }

    public void unMuteNotification(Conversation conversation) {
        //noinspection ResultOfMethodCallIgnored
        conversationRepository.umute(conversation.getId())
                              .startWith(conversationRepository.deleteNotificationOption(conversation.getId()))
                              .compose(RxHelper.applyObservableSchedulers())
                              .onErrorComplete()
                              .subscribe(isSuccess -> Logging.debug(TAG, "Unmute notification successfully, conversationId: " + conversation.getId()));
    }

    public void createGroup(@NonNull User creator, @NonNull List<User> participants) {
        LocalDateTime now = LocalDateTime.now();
        String        id  = "-1";

        ConversationGroup group = new ConversationGroup(id,
                                                        null,
                                                        creator.getUid(),
                                                        creator.getUid(),
                                                        null,
                                                        now,
                                                        creator.getUid(),
                                                        now);
        Conversation conversation = new Conversation(id,
                                                     participants,
                                                     ConversationType.GROUP,
                                                     ConversationStatusType.INBOX,
                                                     now);
        conversation.setGroup(group);

        Disposable disposable = conversationRepository.createGroup(conversation)
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .startWith(Completable.fromAction(() ->
                                                              oneTimeLoadingResult.postValue(Pair.create(true, R.string.action_creating_3_dot))))
                                                      .compose(RxHelper.parseResponseData())
                                                      .subscribe(data -> {
                                                          oneTimeLoadingResult.postValue(Pair.create(false, R.string.action_creating_3_dot));

                                                          data.setStatus(ConversationStatusType.INBOX);

                                                          conversationRepository.save(data)
                                                                                .compose(RxHelper.applyCompleteSchedulers())
                                                                                .onErrorComplete()
                                                                                .subscribe();

                                                          AppDependencies.getDatabaseObserver().notifyConversationInserted(data.getId());
                                                      }, t -> {
                                                          oneTimeLoadingResult.postValue(Pair.create(false, R.string.action_creating_3_dot));
                                                          emitterOneTimeErrorToast(R.string.error_create_group_conversation_fail);
                                                      });

        cd.add(disposable);
    }

    public void markAsRead(Conversation conversation) {
        seenUnreadMessageInConversation(conversation.getId());
    }

    public void ignore(Conversation conversation) {

    }

    public void leaveGroup(Conversation conversation) {
        Disposable disposable = ConversationOptionHandler.leaveGroup(conversationRepository, conversation.getId())
                                                         .compose(RxHelper.applyObservableSchedulers())
                                                         .subscribe(result -> {
                                                             oneTimeLoadingResult.postValue(Pair.create(result.getStatus() == NetworkStatus.LOADING, R.string.action_loading));
                                                         }, t -> {
                                                             oneTimeLoadingResult.postValue(Pair.create(false, R.string.action_loading));

                                                             if (t instanceof ConnectException) {
                                                                 errorEmitter.postValue(new Event<>(R.string.error_connect_server_fail));
                                                             } else if (t instanceof FirebaseNetworkException) {
                                                                 errorEmitter.postValue(new Event<>(R.string.error_network_connection));
                                                             } else {
                                                                 errorEmitter.postValue(new Event<>(R.string.msg_permission_denied));
                                                             }
                                                         });
        cd.add(disposable);
    }

    public void addMember(String conversationId, List<String> memberIds) {
        Disposable disposable = Observable.fromIterable(memberIds)
                                          .flatMap(memberId -> ConversationOptionHandler.addMemberObservable(conversationRepository, conversationId, memberId))
                                          .compose(RxHelper.applyObservableSchedulers())
                                          .subscribe(result -> {
                                              oneTimeLoadingResult.postValue(Pair.create(result.getStatus() == NetworkStatus.LOADING, R.string.action_loading));
                                          }, t -> {
                                              oneTimeLoadingResult.postValue(Pair.create(false, R.string.action_loading));

                                              if (t instanceof ConnectException) {
                                                  errorEmitter.postValue(new Event<>(R.string.error_connect_server_fail));
                                              } else if (t instanceof FirebaseNetworkException) {
                                                  errorEmitter.postValue(new Event<>(R.string.error_network_connection));
                                              } else if (t instanceof PermissionDeniedException) {
                                                  errorEmitter.postValue(new Event<>(R.string.msg_user_dont_allow_added));
                                              } else {
                                                  errorEmitter.postValue(new Event<>(R.string.msg_permission_denied));
                                              }
                                          });

        cd.add(disposable);
    }

    public void loadMore() {
        if (!canLoadMore) return;

        Disposable disposable = conversationRepository.fetchCachePaging(statusType, currentPage + 1, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                                      .doOnDispose(() -> pagingResult.postValue(new Event<>(Result.Terminate())))
                                                      .startWith(Completable.fromAction(() -> pagingResult.postValue(new Event<>(Result.Loading()))))
                                                      .compose(RxHelper.applyFlowableSchedulers())
                                                      .subscribe(mapper -> {
                                                          List<Conversation> data = mapToListConversation(mapper);

                                                          if (data.size() < Const.DEFAULT_CONVERSATION_PAGING_SIZE) {
                                                              loadMoreConversationRemote(data);
                                                          } else {
                                                              handlePagingSuccess(data);
                                                          }
                                                      }, t -> loadMoreConversationRemote(Collections.emptyList()));
        cd.add(disposable);
    }

    private void loadMoreConversationRemote(List<Conversation> cache) {
        Disposable disposable = conversationRepository.fetchByUid(statusType, currentPage + 1, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .subscribe(data -> {
                                                          canLoadMore = !data.isEmpty();

                                                          handlePagingSuccess(data.stream().peek(c -> {
                                                              Collections.reverse(c.getChats());
                                                              c.setStatus(statusType);
                                                          }).collect(Collectors.toList()));
                                                      }, t -> {
                                                          Logging.debug(TAG, "Fetching conversation list from remote failed: " + t.getMessage());

                                                          if (!cache.isEmpty()) {
                                                              handlePagingSuccess(cache);
                                                          } else {
                                                              pagingResult.postValue(new Event<>(Result.Fail(-1)));
                                                          }
                                                      });

        cd.add(disposable);
    }

    private void handlePagingSuccess(List<Conversation> data) {
        currentPage++;
        pagingResult.postValue(new Event<>(Result.Success(data)));
    }

    public void saveConversation(List<Conversation> data) {
        Logging.debug(TAG, "Dispose the current observer for fetch bunch of old conversation");

        conversationDisposable.dispose();

        cd.add(conversationRepository.saveConversationWithoutNotify(data, statusType)
                                     .compose(RxHelper.applyCompleteSchedulers())
                                     .onErrorComplete()
                                     .doOnError(t -> Logging.debug(TAG, "Insert new conversation not complete: " + t.getMessage()))
                                     .subscribe());

        Logging.debug(TAG, "Register new observer for fetch bunch of conversation with size = " + (currentPage + 1) * Const.DEFAULT_CONVERSATION_PAGING_SIZE);

        // Register new observer with the size is the current list is listed for user
        List<Conversation> currentConversationList      = conversationListObserver.getValue();
        int                numberOfElementNeedToObserve = currentConversationList == null ?
                                                          Const.DEFAULT_CONVERSATION_PAGING_SIZE :
                                                          currentConversationList.size() + data.size();

        registerConversationListObserver(statusType, numberOfElementNeedToObserve);
    }

    public void updateCurrentList(List<Conversation> conversations) {
        conversationListObserver.postValue(conversations);
    }

    public void loadUserLeftGroup() {
        setupConversationParticipants(userLeftGroup::postValue);
    }

    public void forceClearDispose() {
        cd.clear();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.clear();
    }
}
