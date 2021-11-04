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

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.CompletableObserver;
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

                                                              saveCallResult(freshConversationList);
                                                          }
                                                      }, t -> refreshConversationResult.setValue(Result.Fail(R.string.error_connect_server_fail)));

        cd.add(disposable);
    }

    private void saveCallResult(List<Conversation> conversations) {
        List<Conversation> updatedData = conversations.stream()
                                                      .peek(u -> u.setStatus(INBOX))
                                                      .collect(Collectors.toList());

        List<String> conversationIdList = conversations.stream()
                                                       .map(Conversation::getId)
                                                       .collect(Collectors.toList());

        conversationRepository.saveAll(updatedData)
                              .andThen(conversationRepository.deleteAll(conversationIdList))
                              .subscribeOn(Schedulers.io())
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new CompletableObserver() {
                                  @Override
                                  public void onSubscribe(@NonNull Disposable d) {

                                  }

                                  @Override
                                  public void onComplete() {
                                      refreshConversationResult.setValue(Result.Success(conversations));
                                      inboxConversations.setValue(conversations);
                                  }

                                  @Override
                                  public void onError(@NonNull Throwable e) {
                                      e.printStackTrace();
                                  }
                              });
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
