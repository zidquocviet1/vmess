package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditProfileBioViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityEditProfileBioBinding;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileBioActivity extends ToolbarActivity<EditProfileBioViewModel, ActivityEditProfileBioBinding>
        implements TextWatcher {
    private static final int MAX_BIO_LENGTH = 120;
    private EditText editBio;
    private String originalBio;

    @Override
    public void binding() {
        mBinding = ActivityEditProfileBioBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<EditProfileBioViewModel> getViewModelClass() {
        return EditProfileBioViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_edit_bio);

        enableSaveButton(v -> {
            var bio = editBio.getText().toString().trim();
            if (bio.equals(originalBio)){
                Toast.makeText(this, R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();

                finish();
            }else
                mViewModel.updateRemoteUser(bio);
        });

        editBio = Objects.requireNonNull(mBinding.textLayoutBio.getEditText());
        editBio.addTextChangedListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserBio().observe(this, bio -> {
            editBio.setText(bio);
            originalBio = bio;
        });

        mViewModel.getUpdateResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            mBinding.includedAppbar.buttonSave.setEnabled(status != NetworkStatus.LOADING);
            mBinding.loading.setVisibility(status == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);

            if (status == NetworkStatus.SUCCESS) {
                updateLoggedInUser(result.getSuccess());

                Toast.makeText(this, R.string.msg_update_user_info_successfully, Toast.LENGTH_SHORT).show();

                finish();
            } else if (status == NetworkStatus.ERROR) {
                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
            }
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
        mBinding.textLength.setText(getString(R.string.prompt_bio_length, s.length(), MAX_BIO_LENGTH));
    }
}