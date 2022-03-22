package com.mqv.vmess.activity;

import androidx.lifecycle.AndroidViewModel;

import android.os.Bundle;

import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ActivityAddConversationBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddConversationActivity extends ToolbarActivity<AndroidViewModel, ActivityAddConversationBinding> {

    @Override
    public void binding() {
        mBinding = ActivityAddConversationBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_new_message);
    }

    @Override
    public void setupObserver() {

    }
}