package com.mqv.vmess.ui.fragment.viewmodel;

import static com.mqv.vmess.network.model.type.ConversationStatusType.INBOX;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.ConversationListViewModel;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.util.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationFragmentViewModel extends ConversationListViewModel {
    private static final String TAG = ConversationFragmentViewModel.class.getSimpleName();

    private final MutableLiveData<Result<List<Conversation>>> refreshConversationResult;
    private final MutableLiveData<Event<Result<Boolean>>>     loadingConversationResult;
    private final MutableLiveData<Event<Integer>>             refreshFailureResult;
    private final MutableLiveData<Event<String>>              conversationInserted;
    private final DatabaseObserver.ConversationListener       conversationObserver;

    public static final int DEFAULT_PAGE_CHAT_LIST = 0;
    public static final int DEFAULT_SIZE_CHAT_LIST = 40;

    @Inject
    public ConversationFragmentViewModel(Application application,
                                         ConversationRepository conversationRepository,
                                         ChatRepository chatRepository) {
        super(application, conversationRepository, chatRepository, INBOX);

        this.refreshConversationResult = new MutableLiveData<>();
        this.loadingConversationResult = new MutableLiveData<>();
        this.refreshFailureResult      = new MutableLiveData<>();
        this.conversationInserted      = new MutableLiveData<>();
        this.conversationObserver      = new DatabaseObserver.ConversationListener() {
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

    public LiveData<Result<List<Conversation>>> getRefreshConversationResult() {
        return refreshConversationResult;
    }

    public LiveData<Event<Result<Boolean>>> getLoadingConversationResult() {
        return loadingConversationResult;
    }

    public LiveData<Event<Integer>> getRefreshFailureResult() {
        return refreshFailureResult;
    }

    public LiveData<List<Conversation>> getConversationListObserver() {
        return Transformations.map(conversationListObserver, list -> {
            if (list != null && !list.isEmpty()) {
                return list.stream()
                        .filter(c -> c.getStatus() == INBOX)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        });
    }

    public LiveData<Event<String>> getConversationInserted() {
        return conversationInserted;
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
        Disposable disposable = onRefresh(INBOX).startWith(Completable.fromAction(() -> loadingConversationResult.postValue(new Event<>(Result.Loading()))))
                .doOnDispose(() -> loadingConversationResult.postValue(new Event<>(Result.Terminate())))
                .compose(RxHelper.parseResponseData())
                .subscribe(data -> saveCallResult(data, INBOX, () -> loadingConversationResult.postValue(new Event<>(Result.Success(true)))),
                        t -> loadingConversationResult.postValue(new Event<>(Result.Fail(-1))));

        cd.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        AppDependencies.getDatabaseObserver().unregisterConversationListener(conversationObserver);
    }
}
