package com.mqv.vmess.activity.preferences;

import android.os.Bundle;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.activity.viewmodel.ConversationListArchivedViewModel;
import com.mqv.vmess.databinding.ActivityPreferenceArchivedChatBinding;
import com.mqv.vmess.ui.fragment.ConversationListArchivedFragment;

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