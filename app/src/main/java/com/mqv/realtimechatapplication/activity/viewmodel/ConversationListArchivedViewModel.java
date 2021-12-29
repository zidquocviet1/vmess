package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.network.model.type.ConversationStatusType.ARCHIVED;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.Conversation;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.functions.Consumer;

@HiltViewModel
public class ConversationListArchivedViewModel extends ConversationListViewModel {
    private final ConversationRepository                        repository;
    private final MutableLiveData<Result<List<Conversation>>>   archivedChatResult;
    private int                                                 currentPage;

    @Inject
    public ConversationListArchivedViewModel(ConversationRepository repository) {
        super(repository, ARCHIVED);

        this.repository             = repository;
        this.archivedChatResult     = new MutableLiveData<>();
        this.currentPage            = 0;

        fetchArchivedChat();
    }

    public LiveData<Result<List<Conversation>>> getArchivedChatResult() {
        return archivedChatResult;
    }

    public LiveData<List<Conversation>> getListObserver() {
        return conversationListObserver;
    }

    private void fetchArchivedChat() {
        archivedChatResult.setValue(Result.Loading());

        Consumer<List<Conversation>> onReceiveData = data -> archivedChatResult.setValue(Result.Success(data));
        Consumer<Throwable>          onError       = t -> {
            archivedChatResult.setValue(Result.Fail(R.string.error_authentication_fail));
        };

        initializeFetch(ARCHIVED, onReceiveData, onError);
    }

    public void loadMore() {
        int nextPage = currentPage + 1;

        if (true) {
            currentPage = nextPage;
        }
    }
}
