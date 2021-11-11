package com.mqv.realtimechatapplication.activity.preferences;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListRequestViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceMessageRequestBinding;

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