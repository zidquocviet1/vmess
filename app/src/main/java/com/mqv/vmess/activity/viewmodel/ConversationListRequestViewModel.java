package com.mqv.vmess.activity.viewmodel;

import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.network.model.type.ConversationStatusType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ConversationListRequestViewModel extends ConversationListViewModel{

    @Inject
    public ConversationListRequestViewModel(ConversationRepository conversationRepository) {
        super(conversationRepository, ConversationStatusType.REQUEST);
    }

}
