package com.mqv.vmess.activity.viewmodel;

import android.app.Application;

import com.mqv.vmess.data.repository.ChatRepository;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.network.model.type.ConversationStatusType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ConversationListRequestViewModel extends ConversationListViewModel{

    @Inject
    public ConversationListRequestViewModel(Application application,
                                            ConversationRepository conversationRepository,
                                            ChatRepository chatRepository) {
        super(application, conversationRepository, chatRepository, ConversationStatusType.REQUEST);
    }

}
