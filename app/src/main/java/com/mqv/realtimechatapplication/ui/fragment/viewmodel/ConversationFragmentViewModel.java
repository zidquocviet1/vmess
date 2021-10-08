package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.util.MessageStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ConversationFragmentViewModel extends AbstractMainViewModel {

    @Inject
    public ConversationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        newData();
    }

    @Override
    public void onRefresh() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            var listConversation = Arrays.asList(
                    new Conversation(1L, "Phạm Thảo Duyên", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                    new Conversation(2L, "Phạm Băng Băng", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                    new Conversation(3L, "Triệu Lệ Dĩnh", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                    new Conversation(4L, "Ngô Diệc Phàm", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                    new Conversation(5L, "Lưu Đức Hoa", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED)
            );

            getConversationList().postValue(listConversation);
        }, 2000);
    }

    public LiveData<List<Conversation>> getConversationListSafe(){
        return getConversationList();
    }

    public LiveData<List<RemoteUser>> getRemoteUserListSafe(){
        return getRemoteUserList();
    }
}
