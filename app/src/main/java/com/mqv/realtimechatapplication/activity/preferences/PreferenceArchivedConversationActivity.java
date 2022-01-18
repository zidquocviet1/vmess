package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListArchivedViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceArchivedChatBinding;
import com.mqv.realtimechatapplication.ui.fragment.ConversationListArchivedFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceArchivedConversationActivity extends ToolbarActivity<ConversationListArchivedViewModel,
        ActivityPreferenceArchivedChatBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceArchivedChatBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConversationListArchivedViewModel> getViewModelClass() {
        return ConversationListArchivedViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_archived_chats);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_conversation_list, ConversationListArchivedFragment.newInstance())
                .commit();
    }

    @Override
    public void setupObserver() {
    }
}