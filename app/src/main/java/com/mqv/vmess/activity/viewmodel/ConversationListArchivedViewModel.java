package com.mqv.vmess.activity.viewmodel;

import static com.mqv.vmess.network.model.type.ConversationStatusType.ARCHIVED;

import androidx.lifecycle.LiveData;

import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.network.model.Conversation;

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

    public List<String> getPresenceUserListValue() {
        return getPresenceUserList();
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
