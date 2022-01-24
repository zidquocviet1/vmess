package com.mqv.realtimechatapplication.activity.viewmodel;

import static com.mqv.realtimechatapplication.network.model.type.ConversationStatusType.ARCHIVED;

import androidx.lifecycle.LiveData;

import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.network.model.Conversation;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.functions.Consumer;

@HiltViewModel
public class ConversationListArchivedViewModel extends ConversationListViewModel {
    private final ConversationRepository                        repository;
    private int                                                 currentPage;

    @Inject
    public ConversationListArchivedViewModel(ConversationRepository repository) {
        super(repository, ARCHIVED);

        this.repository             = repository;
        this.currentPage            = 0;

        fetchArchivedChat();
    }

    public LiveData<List<Conversation>> getListObserver() {
        return conversationListObserver;
    }

    public LiveData<List<String>> getPresenceUserListObserver() {
        return getPresenceUserListObserverDistinct();
    }

    private void fetchArchivedChat() {
        Consumer<List<Conversation>> onReceiveData = data -> {};
        Consumer<Throwable>          onError       = Throwable::printStackTrace;

        initializeFetch(ARCHIVED, onReceiveData, onError);
    }

    public void loadMore() {
        int nextPage = currentPage + 1;

        if (true) {
            currentPage = nextPage;
        }
    }
}
