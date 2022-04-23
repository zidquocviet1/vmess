package com.mqv.vmess.activity.viewmodel;

import static com.mqv.vmess.network.model.type.ConversationStatusType.ARCHIVED;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.network.model.Conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.functions.Consumer;

@HiltViewModel
public class ConversationListArchivedViewModel extends ConversationListViewModel {
    private int currentPage;

    @Inject
    public ConversationListArchivedViewModel(Application application,
                                             ConversationRepository repository,
                                             ChatRepository chatRepository) {
        super(application, repository, chatRepository, ARCHIVED);

        this.currentPage = 0;

        fetchArchivedChat();
    }

    public LiveData<List<Conversation>> getListObserver() {
        return Transformations.map(conversationListObserver, list -> {
            if (list != null && !list.isEmpty()) {
                return list.stream()
                           .filter(c -> c.getStatus() == ARCHIVED)
                           .collect(Collectors.toList());
            }
            return new ArrayList<>();
        });
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
