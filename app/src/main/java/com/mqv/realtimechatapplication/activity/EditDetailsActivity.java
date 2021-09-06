package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditDetailsViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityEditDetailsBinding;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditDetailsActivity extends ToolbarActivity<EditDetailsViewModel, ActivityEditDetailsBinding> {
    private EditText edtName, edtGender, edtBirthday, edtCurrentAddress, edtFrom;

    @Override
    public void binding() {
        mBinding = ActivityEditDetailsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<EditDetailsViewModel> getViewModelClass() {
        return EditDetailsViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_edit_details);

        edtName = Objects.requireNonNull(mBinding.textLayoutDisplayName.getEditText());
        edtGender = Objects.requireNonNull(mBinding.textLayoutGender.getEditText());
        edtBirthday = Objects.requireNonNull(mBinding.textLayoutBirthday.getEditText());
        edtCurrentAddress = Objects.requireNonNull(mBinding.textLayoutCurrentAddress.getEditText());
        edtFrom = Objects.requireNonNull(mBinding.textLayoutComeFrom.getEditText());
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, this::showUserUi);
    }

    private void showUserUi(FirebaseUser user){
        if (user == null) return;

        // TODO Get user from backend server here
        edtName.setText(user.getDisplayName());
    }
}