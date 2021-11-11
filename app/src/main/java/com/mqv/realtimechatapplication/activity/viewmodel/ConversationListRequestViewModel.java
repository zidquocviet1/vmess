package com.mqv.realtimechatapplication.activity.viewmodel;

import com.mqv.realtimechatapplication.data.repository.ConversationRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ConversationListRequestViewModel extends ConversationListViewModel{

    @Inject
    public ConversationListRequestViewModel(ConversationRepository conversationRepository) {
        super(conversationRepository);
    }

}
