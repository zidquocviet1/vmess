package com.mqv.vmess.activity.preferences;

import android.os.Bundle;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.activity.viewmodel.ConversationListRequestViewModel;
import com.mqv.vmess.databinding.ActivityPreferenceMessageRequestBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceMessageRequestActivity extends ToolbarActivity<ConversationListRequestViewModel, ActivityPreferenceMessageRequestBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceMessageRequestBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConversationListRequestViewModel> getViewModelClass() {
        return ConversationListRequestViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_messages_requests);
    }

    @Override
    public void setupObserver() {

    }
}