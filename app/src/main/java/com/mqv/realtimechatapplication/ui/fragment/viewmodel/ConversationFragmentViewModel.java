package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.MessageStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ConversationFragmentViewModel extends ViewModel {
    private final MutableLiveData<List<Conversation>> conversationList;

    public ConversationFragmentViewModel() {
        Logging.show("Conversation Fragment View Model created");
        conversationList = new MutableLiveData<>();

        newData();
    }

    public LiveData<List<Conversation>> getConversationList() {
        return conversationList;
    }

    /// test
    public void newData(){
        var listConversation = Arrays.asList(
                new Conversation(1L, "Phạm Thảo Duyên", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(2L, "Phạm Băng Băng", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(3L, "Triệu Lệ Dĩnh", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(4L, "Ngô Diệc Phàm", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(5L, "Lưu Đức Hoa", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(6L, "Pham thao duyen 1", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(7L, "Pham Bang Bang 1", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(8L, "Trieu Le Dinh 1", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(9L, "Ngo Diec Pham 1", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(10L, "Luu Duc Hoa 1", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(11L, "Pham thao duyen 2", "You: ok", LocalDateTime.now(), MessageStatus.RECEIVED),
                new Conversation(12L, "Pham Bang Bang 2", "You: haha", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(13L, "Trieu Le Dinh 2", "You: wo ai ni", LocalDateTime.now(), MessageStatus.SEEN),
                new Conversation(14L, "Ngo Diec Pham 2", "You: 2 phut hon", LocalDateTime.now(), MessageStatus.NOT_RECEIVED),
                new Conversation(15L, "Luu Duc Hoa 2", "You: hao le", LocalDateTime.now(), MessageStatus.RECEIVED)
        );
        conversationList.setValue(listConversation);
    }
}
