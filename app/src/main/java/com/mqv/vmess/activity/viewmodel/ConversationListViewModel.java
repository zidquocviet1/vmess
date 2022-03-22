package com.mqv.vmess.activity.viewmodel;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.ConversationRepository;
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
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Retriever;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationListViewModel extends ViewModel {
    private   final ConversationRepository                  conversationRepository;
    protected final MutableLiveData<List<Conversation>>     conversationListObserver;
    protected final MutableLiveData<List<String>>           presenceUserListObserver;
    protected final MutableLiveData<Pair<Boolean, Integer>> oneTimeLoadingResult;
    protected final MutableLiveData<Event<Integer>>         errorEmitter;
    protected final CompositeDisposable                     cd;

    private static final int INITIALIZE_PAGE = 0;

    public ConversationListViewModel(ConversationRepository conversationRepository, ConversationStatusType status) {
        this.conversationRepository   = conversationRepository;
        this.cd                       = new CompositeDisposable();
        this.conversationListObserver = new MutableLiveData<>(AppDependencies.getMemoryManager().getConversationListCache());
        this.presenceUserListObserver = new MutableLiveData<>(Collections.emptyList());
        this.oneTimeLoadingResult     = new MutableLiveData<>();
        this.errorEmitter             = new MutableLiveData<>();

        //noinspection ResultOfMethodCallIgnored
        conversationRepository.conversationAndLastChat(status)
                              .compose(RxHelper.applyFlowableSchedulers())
                              .map(this::mapToListConversation)
                              .subscribe(conversationListObserver::postValue);

        //noinspection ResultOfMethodCallIgnored
        AppDependencies.getWebSocket()
                       .getPresenceUserList()
                       .onErrorComplete()
                       .subscribe(presenceUserListObserver::postValue);
    }

    public LiveData<Event<Integer>> getOneTimeErrorObserver() {
        return errorEmitter;
    }

    public LiveData<Pair<Boolean, Integer>> getOneTimeLoadingResult() {
        return oneTimeLoadingResult;
    }

    protected LiveData<List<String>> getPresenceUserListObserverDistinct() {
        return LiveDataUtil.distinctUntilChanged(presenceUserListObserver, (oldList, newList) -> {
            if (newList == null) return false;
            if (oldList.isEmpty() && !newList.isEmpty()) return false;
            if (!oldList.isEmpty() && newList.isEmpty()) return false;

            return newList.containsAll(oldList) && oldList.containsAll(newList);
        });
    }

    protected List<String> getPresenceUserList() {
        return Retriever.getOrDefault(presenceUserListObserver.getValue(), Collections.emptyList());
    }

    protected List<Conversation> mapToListConversation(Map<Conversation, Chat> map) {
        Iterator<Map.Entry<Conversation, Chat>> iterator = map.entrySet().iterator();
        List<Conversation>                      result   = new ArrayList<>();

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
        return conversationRepository.fetchByUid(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
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

    protected void fetchCachedConversation(@NonNull String conversationId, Consumer<Conversation> onReceive) {
        Disposable disposable = conversationRepository.fetchCachedById(conversationId)
                                                      .compose(RxHelper.applySingleSchedulers())
                                                      .subscribe(onReceive, Throwable::printStackTrace);

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

    public void muteNotification(Conversation conversation) {

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

    public void markAsUnread(Conversation conversation) {

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

    public void loadMore(int page, ConversationStatusType statusType) {

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
