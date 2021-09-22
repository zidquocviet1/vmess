package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.lifecycle.AndroidViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.UsernameViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceUsernameBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceUsernameActivity extends ToolbarActivity<UsernameViewModel, ActivityPreferenceUsernameBinding> implements TextWatcher {
    private static final int MAX_USERNAME_LENGTH = 20;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceUsernameBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<UsernameViewModel> getViewModelClass() {
        return UsernameViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_username);

        enableSaveButton(handleButtonSaveClicked());

        mBinding.editUserName.addTextChangedListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUsername().observe(this, username -> {
            mBinding.editUserName.setText(username);
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mBinding.textPromptLength.setText(getString(R.string.prompt_bio_length, s.length(), MAX_USERNAME_LENGTH));
    }

    private View.OnClickListener handleButtonSaveClicked() {
        return v -> {
            mViewModel.editUsername("sadfjkl");
        };
    }
}