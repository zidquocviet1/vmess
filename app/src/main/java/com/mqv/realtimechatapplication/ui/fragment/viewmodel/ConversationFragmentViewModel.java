package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import static com.mqv.realtimechatapplication.network.model.type.ConversationStatusType.INBOX;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListViewModel;
import com.mqv.realtimechatapplication.data.DatabaseObserver;
import com.mqv.realtimechatapplication.data.repository.ChatRepository;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.dependencies.AppDependencies;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.reactive.RxHelper;
import com.mqv.realtimechatapplication.util.Event;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationFragmentViewModel extends ConversationListViewModel {
    private static final String TAG = ConversationFragmentViewModel.class.getSimpleName();

    private final ConversationRepository                            conversationRepository;
    private final ChatRepository                                    chatRepository;
    private final MutableLiveData<List<RemoteUser>>                 rankUser;
    private final MutableLiveData<Result<List<Conversation>>>       refreshConversationResult;
    private final MutableLiveData<Event<Integer>>                   refreshFailureResult;
    private final MutableLiveData<Event<String>>                    conversationInserted;
    private final DatabaseObserver.ConversationListener             conversationObserver;

    public static final int                                         DEFAULT_PAGE_CHAT_LIST          = 0;
    public static final int                                         DEFAULT_SIZE_CHAT_LIST          = 40;

    @Inject
    public ConversationFragmentViewModel(ConversationRepository conversationRepository,
                                         ChatRepository chatRepository) {
        super(conversationRepository, INBOX);

        this.conversationRepository         = conversationRepository;
        this.chatRepository                 = chatRepository;
        this.refreshConversationResult      = new MutableLiveData<>();
        this.refreshFailureResult           = new MutableLiveData<>();
        this.rankUser                       = new MutableLiveData<>();
        this.conversationInserted           = new MutableLiveData<>();
        this.conversationObserver           = new DatabaseObserver.ConversationListener() {
            @Override
            public void onConversationInserted(@NonNull String conversationId) {
                conversationInserted.postValue(new Event<>(conversationId));
            }

            @Override
            public void onConversationUpdated(@NonNull String conversationId) {
                //noinspection ResultOfMethodCallIgnored
                conversationRepository.conversationAndLastChat(conversationId, INBOX)
                                      .subscribeOn(Schedulers.io())
                                      .observeOn(AndroidSchedulers.mainThread())
                                      .onErrorComplete()
                                      .subscribe(map -> notifyConversationLastChatUpdate(map, conversationId));
            }
        };

        fetchAllConversation();
        AppDependencies.getDatabaseObserver().registerConversationListener(conversationObserver);
    }

    public void onRefresh() {
        Disposable disposable = onRefresh(INBOX).doOnDispose(() -> refreshConversationResult.setValue(Result.Terminate()))
                                                .compose(RxHelper.parseResponseData())
                                                .subscribe(data -> saveCallResult(data, INBOX, () -> refreshConversationResult.setValue(Result.Success(data))),
                                                           t -> refreshFailureResult.setValue(new Event<>(R.string.error_connect_server_fail)));
        cd.add(disposable);
    }

    public LiveData<List<RemoteUser>> getRankUserListSafe() {
        return rankUser;
    }

    public LiveData<Result<List<Conversation>>> getRefreshConversationResult() {
        return refreshConversationResult;
    }

    public LiveData<Event<Integer>> getRefreshFailureResult() {
        return refreshFailureResult;
    }

    public LiveData<List<Conversation>> getConversationListObserver() {
        return conversationListObserver;
    }

    public LiveData<Event<String>> getConversationInserted() {
        return conversationInserted;
    }

    public LiveData<List<String>> getPresenceUserListObserver() {
        return getPresenceUserListObserverDistinct();
    }

    public List<String> getPresenceUserListValue() {
        return getPresenceUserList();
    }

    public LiveData<Boolean> getOneTimeLoadingObserver() {
        return oneTimeLoadingResult;
    }

    private void notifyConversationLastChatUpdate(Map<Conversation, Chat> map, String conversationId) {
        if (!map.isEmpty()) {
            mapToListConversation(map).stream()
                                      .filter(c -> c.getId().equals(conversationId))
                                      .findFirst()
                                      .ifPresent(c2 -> {
                                          List<Conversation> conversations;

                                          if (conversationListObserver.getValue() == null) {
                                              conversations = new ArrayList<>();
                                          } else {
                                              conversations = new ArrayList<>(conversationListObserver.getValue());
                                          }

                                          int index = conversations.indexOf(c2);
                                          if (index != -1) {
                                              conversations.set(index, c2);
                                              conversationListObserver.postValue(conversations);
                                          }
                                      });
        }
    }

    private void fetchAllConversation() {
        Consumer<List<Conversation>> onReceiveData = data -> Logging.debug(TAG, "Receive fresh data");
        Consumer<Throwable>          onError       = Throwable::printStackTrace;

        initializeFetch(INBOX, onReceiveData, onError);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        AppDependencies.getDatabaseObserver().unregisterConversationListener(conversationObserver);
    }
}
