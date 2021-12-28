package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationListViewModel extends ViewModel {
    private   final ConversationRepository      conversationRepository;
    protected final CompositeDisposable         cd;

    private static final int                    INITIALIZE_PAGE             = 0;

    public ConversationListViewModel(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
        this.cd                     = new CompositeDisposable();
    }

    // Only call with the activity has swipe refresh layout
    public Observable<ApiResponse<List<Conversation>>> onRefresh(ConversationStatusType type) {
        return conversationRepository.fetchByUid(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                     .subscribeOn(Schedulers.io())
                                     .observeOn(AndroidSchedulers.mainThread());
    }

    protected void initializeFetch(ConversationStatusType type,
                                   Consumer<List<Conversation>> onReceiveData,
                                   Consumer<Throwable> onError) {
        Runnable onDataChanged = () -> {
            Disposable cacheDisposable = conversationRepository.fetchCached(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
                                                               .subscribeOn(Schedulers.io())
                                                               .observeOn(AndroidSchedulers.mainThread())
                                                               .subscribe(onReceiveData, onError);
            cd.add(cacheDisposable);
        };

        Disposable disposable = conversationRepository.fetchByUidNBR(type, INITIALIZE_PAGE, Const.DEFAULT_CONVERSATION_PAGING_SIZE, onDataChanged)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(onReceiveData, onError);

        cd.add(disposable);
    }

    // Save new refresh conversation list and notify on data save successfully
    protected void saveCallResult(List<Conversation> conversations, ConversationStatusType type, Action onSaveSuccess) {
        Disposable disposable = conversationRepository.saveAll(conversations, type)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(onSaveSuccess);
        cd.add(disposable);
    }

    protected void fetchCachedConversation(@NonNull Conversation conversation, Consumer<Conversation> onReceive) {
        Disposable disposable = conversationRepository.fetchCachedById(conversation)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
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

    public void createGroup(Conversation conversation) {

    }

    public void markAsUnread(Conversation conversation) {

    }

    public void ignore(Conversation conversation) {

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
