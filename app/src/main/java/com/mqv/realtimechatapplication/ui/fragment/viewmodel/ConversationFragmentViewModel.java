package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import static com.mqv.realtimechatapplication.network.model.type.ConversationStatusType.INBOX;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.data.repository.ConversationRepository;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationFragmentViewModel extends AbstractMainViewModel {
    private final ConversationRepository                            conversationRepository;

    private final MutableLiveData<List<Conversation>>               inboxConversations;
    private final MutableLiveData<List<RemoteUser>>                 rankUser;
    private final MutableLiveData<Result<List<Conversation>>>       refreshConversationResult;
    private final MutableLiveData<Conversation>                     cachedConversation;

    public static final int                                         DEFAULT_PAGE_CHAT_LIST          = 0;
    public static final int                                         DEFAULT_SIZE_CHAT_LIST          = 40;

    @Inject
    public ConversationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository,
                                         ConversationRepository conversationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.conversationRepository         = conversationRepository;
        this.inboxConversations             = new MutableLiveData<>();
        this.cachedConversation             = new MutableLiveData<>();
        this.refreshConversationResult      = new MutableLiveData<>();
        this.rankUser                       = new MutableLiveData<>();

        newData();
        fetchAllConversation();
    }

    @Override
    public void onRefresh() {
        refreshConversation();
    }

    public LiveData<List<RemoteUser>> getRankUserListSafe() {
        return rankUser;
    }

    public LiveData<List<Conversation>> getInboxConversation() {
        return inboxConversations;
    }

    public LiveData<Conversation> getCachedConversation() {
        return cachedConversation;
    }

    public LiveData<Result<List<Conversation>>> getRefreshConversationResult() {
        return refreshConversationResult;
    }

    // Submit new conversation to current list, when accepted new friend or added to new group
    public void submitConversation(Conversation conversation) {
        List<Conversation> conversations = inboxConversations.getValue();

        if (conversations == null)
            conversations = new ArrayList<>();
        conversations.add(0, conversation);

        inboxConversations.postValue(conversations);
    }

    public void submitRemoveConversation(String userId, String unFriendUserId) {
        List<String> idList = Arrays.asList(userId, unFriendUserId);
        List<Conversation> conversations = inboxConversations.getValue();

        if (conversations == null)
            return;

        Optional<Conversation> conversation = conversations.stream()
                                                           .filter(c -> c.getType() == ConversationType.NORMAL)
                                                           .filter(c -> c.getParticipants()
                                                                         .stream()
                                                                         .map(User::getUid)
                                                                         .collect(Collectors.toList()).containsAll(idList))
                                                           .findFirst();

        if (conversation.isPresent()) {
            conversations.remove(conversation.get());

            inboxConversations.postValue(conversations);
        }

        Completable.fromAction(() -> conversationRepository.deleteNormalByParticipantId(userId, unFriendUserId))
                   .subscribeOn(Schedulers.io())
                   .observeOn(Schedulers.io())
                   .subscribe();
    }

    private void refreshConversation() {
        Disposable disposable = conversationRepository.fetchByUid(INBOX,
                                                                  DEFAULT_PAGE_CHAT_LIST,
                                                                  DEFAULT_SIZE_CHAT_LIST)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .doOnDispose(() -> refreshConversationResult.setValue(Result.Terminate()))
                                                      .subscribe(response -> {
                                                          if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                                              List<Conversation> freshConversationList = response.getSuccess();

                                                              saveCallResult(freshConversationList, INBOX);
                                                          }
                                                      }, t -> refreshConversationResult.setValue(Result.Fail(R.string.error_connect_server_fail)));

        cd.add(disposable);
    }

    private void saveCallResult(List<Conversation> conversations, ConversationStatusType type) {
        Disposable disposable = conversationRepository.saveAll(conversations, type)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(() -> {
                                                          refreshConversationResult.setValue(Result.Success(conversations));
                                                          inboxConversations.setValue(conversations);
                                                      });
        cd.add(disposable);
    }

    private void fetchAllConversation() {
        Runnable onDataChanged = () -> {
            Disposable cacheDisposable = conversationRepository.fetchCached(INBOX,
                                                                            DEFAULT_PAGE_CHAT_LIST,
                                                                            DEFAULT_SIZE_CHAT_LIST)
                                                               .subscribeOn(Schedulers.io())
                                                               .observeOn(AndroidSchedulers.mainThread())
                                                               .subscribe(inboxConversations::setValue, Throwable::printStackTrace);
            cd.add(cacheDisposable);
        };

        Disposable disposable = conversationRepository.fetchByUidNBR(INBOX,
                                                                     DEFAULT_PAGE_CHAT_LIST,
                                                                     DEFAULT_SIZE_CHAT_LIST,
                                                                     onDataChanged)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(inboxConversations::setValue, Throwable::printStackTrace);

        cd.add(disposable);
    }

    public void fetchCachedConversation(@NonNull Conversation conversation) {
        Disposable disposable = conversationRepository.fetchCachedById(conversation)
                                                      .subscribeOn(Schedulers.io())
                                                      .observeOn(AndroidSchedulers.mainThread())
                                                      .subscribe(cachedConversation::setValue, Throwable::printStackTrace);

        cd.add(disposable);
    }

    private void newData() {
        var listRemoteUser = Arrays.asList(
                new RemoteUser("1", "Thảo Duyên", "Phạm"),
                new RemoteUser("2", "Băng Băng", "Phạm"),
                new RemoteUser("3", "Lệ Dĩnh", "Triệu"),
                new RemoteUser("4", "Diệc Phàm", "Ngô"),
                new RemoteUser("5", "Đức Hoa", "Lưu"),
                new RemoteUser("6", "Viet", "Mai"),
                new RemoteUser("7", "Han", "Ngoc"),
                new RemoteUser("8", "Nhu", "Tran"),
                new RemoteUser("9", "Mai", "Phuong"),
                new RemoteUser("10", "Tuyen", "Huyen")
        );
        rankUser.setValue(listRemoteUser);
    }
}
