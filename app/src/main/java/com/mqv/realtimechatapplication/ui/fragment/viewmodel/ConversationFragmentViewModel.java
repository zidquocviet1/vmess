package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationFragmentViewModel extends AbstractMainViewModel {
    private final ConversationRepository              conversationRepository;

    private final MutableLiveData<List<Conversation>> inboxConversations;

    @Inject
    public ConversationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository,
                                         ConversationRepository conversationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.conversationRepository = conversationRepository;
        this.inboxConversations     = new MutableLiveData<>();

        newData();
        fetchAllConversation();
    }

    @Override
    public void onRefresh() {
    }

    public LiveData<List<Conversation>> getConversationListSafe() {
        return getConversationList();
    }

    public LiveData<List<RemoteUser>> getRemoteUserListSafe() {
        return getRemoteUserList();
    }

    public LiveData<List<Conversation>> getInboxConversation() {
        return Transformations.distinctUntilChanged(inboxConversations);
    }

    private void fetchAllConversation() {
        Disposable disposable = conversationRepository.fetchByUidNBR(ConversationStatusType.INBOX)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(inboxConversations::setValue, Throwable::printStackTrace);

        cd.add(disposable);
    }
}
