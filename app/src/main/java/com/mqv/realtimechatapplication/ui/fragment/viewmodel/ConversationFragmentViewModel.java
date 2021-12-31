package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import static com.mqv.realtimechatapplication.network.model.type.ConversationStatusType.INBOX;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListViewModel;
import com.mqv.realtimechatapplication.data.repository.ChatRepository;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

@HiltViewModel
public class ConversationFragmentViewModel extends ConversationListViewModel {
    private static final String TAG = ConversationFragmentViewModel.class.getSimpleName();

    private final ConversationRepository                            conversationRepository;
    private final ChatRepository                                    chatRepository;
    private final MutableLiveData<List<RemoteUser>>                 rankUser;
    private final MutableLiveData<Result<List<Conversation>>>       refreshConversationResult;

    public static final int                                         DEFAULT_PAGE_CHAT_LIST          = 0;
    public static final int                                         DEFAULT_SIZE_CHAT_LIST          = 40;

    @Inject
    public ConversationFragmentViewModel(ConversationRepository conversationRepository,
                                         ChatRepository chatRepository) {
        super(conversationRepository, INBOX);

        this.conversationRepository         = conversationRepository;
        this.chatRepository                 = chatRepository;
        this.refreshConversationResult      = new MutableLiveData<>();
        this.rankUser                       = new MutableLiveData<>();

        fetchAllConversation();
    }

    public void onRefresh() {
        Disposable disposable = onRefresh(INBOX).doOnDispose(() -> refreshConversationResult.setValue(Result.Terminate()))
                                                .subscribe(response -> {
                                                    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                        List<Conversation> freshConversationList = response.getSuccess();

                                                        saveCallResult(freshConversationList, INBOX, () ->
                                                                refreshConversationResult.setValue(Result.Success(freshConversationList)));
                                                    }
                                                }, t -> refreshConversationResult.setValue(Result.Fail(R.string.error_connect_server_fail)));

        cd.add(disposable);
    }

    public LiveData<List<RemoteUser>> getRankUserListSafe() {
        return rankUser;
    }

    public LiveData<Result<List<Conversation>>> getRefreshConversationResult() {
        return refreshConversationResult;
    }

    public LiveData<List<Conversation>> getConversationListObserver() {
        return conversationListObserver;
    }

    private void fetchAllConversation() {
        Consumer<List<Conversation>> onReceiveData = data -> Logging.debug(TAG, "Receive fresh data");
        Consumer<Throwable>          onError       = Throwable::printStackTrace;

        initializeFetch(INBOX, onReceiveData, onError);
    }
}
