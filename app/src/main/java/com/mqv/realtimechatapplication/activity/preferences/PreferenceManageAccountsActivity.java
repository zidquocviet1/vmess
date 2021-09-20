package com.mqv.realtimechatapplication.activity.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.LoginActivity;
import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.RegisterActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.ManageAccountViewModel;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.SignInProvider;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceManageAccountsBinding;
import com.mqv.realtimechatapplication.databinding.DialogSwitchAccountBinding;
import com.mqv.realtimechatapplication.ui.adapter.LoggedInUserAdapter;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceManageAccountsActivity extends
        ToolbarActivity<ManageAccountViewModel, ActivityPreferenceManageAccountsBinding> implements View.OnClickListener {
    private LoggedInUserAdapter mAdapter;
    private final List<HistoryLoggedInUser> mHistoryUserList = new ArrayList<>();
    private AlertDialog mAlertDialog;
    private DialogSwitchAccountBinding mDialogBinding;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceManageAccountsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ManageAccountViewModel> getViewModelClass() {
        return ManageAccountViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_switch_accounts);

        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBinding.includedButton.buttonBottom.setText(R.string.action_create_new_account);
        mBinding.includedButton.buttonBottom.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getHistoryUserList().observe(this, userList -> {
            if (userList == null || userList.isEmpty()) {
            } else {
                mHistoryUserList.clear();
                mHistoryUserList.addAll(userList);
            }
            mHistoryUserList.add(null);
            mAdapter.submitList(mHistoryUserList);
        });

        mViewModel.getLoginResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();
            showLoadingUi(status == NetworkStatus.LOADING);

            if (status == NetworkStatus.SUCCESS) {
                var mainIntent = new Intent(PreferenceManageAccountsActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainIntent);
                finishAffinity();
            } else if (status == NetworkStatus.ERROR) {
                Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingUi(boolean isLoading) {
        if (mAlertDialog != null && mDialogBinding != null) {
            if (isLoading) {
                mDialogBinding.progressBarLoading.setVisibility(View.VISIBLE);
                mDialogBinding.layoutInput.setVisibility(View.GONE);
            } else {
                mAlertDialog.cancel();
            }
        }
    }

    private void setupRecyclerView() {
        mAdapter = new LoggedInUserAdapter(this, mHistoryUserList);
        mAdapter.setOnAddAccountClick(v -> startActivity(LoginActivity.class));
        mAdapter.setOnChangeAccountClick(this::handleChangeLoginUser);
        mBinding.recyclerLoggedInUser.setAdapter(mAdapter);
        mBinding.recyclerLoggedInUser.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.includedButton.buttonBottom.getId()) {
            startActivity(RegisterActivity.class);
        }
    }

    private void startActivity(Class<?> target) {
        startActivity(new Intent(PreferenceManageAccountsActivity.this, target));
    }

    private void handleChangeLoginUser(HistoryLoggedInUser historyUser) {
        if (historyUser.getLogin()) {
            Toast.makeText(this, R.string.msg_current_login_user, Toast.LENGTH_SHORT).show();
        } else {
            var signInProvider = historyUser.getProvider();

            if (signInProvider == SignInProvider.EMAIL) {
                showCustomDialog(historyUser);
            } else if (signInProvider == SignInProvider.PHONE) {

            }
        }
    }

    private void showCustomDialog(HistoryLoggedInUser historyUser) {
        var view = getLayoutInflater().inflate(R.layout.dialog_switch_account, null, false);
        mDialogBinding = DialogSwitchAccountBinding.bind(view);

        mAlertDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        mDialogBinding.editTextPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE){
                var imm = getSystemService(InputMethodManager.class);
                imm.hideSoftInputFromWindow(mDialogBinding.editTextPassword.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        mDialogBinding.layoutInput.setVisibility(View.VISIBLE);
        mDialogBinding.progressBarLoading.setVisibility(View.GONE);
        mDialogBinding.textTitle.setText(getString(R.string.title_continue_as, historyUser.getDisplayName()));
        mDialogBinding.textForgotPassword.setOnClickListener(v -> {

        });
        mDialogBinding.buttonDone.setOnClickListener(v -> {
            var imm = getSystemService(InputMethodManager.class);
            imm.hideSoftInputFromWindow(mDialogBinding.editTextPassword.getWindowToken(), 0);

            var password = mDialogBinding.editTextPassword.getText().toString().trim();
            if (!password.equals("")) {
                var email = historyUser.getEmail();
                if (email != null) {
                    mViewModel.switchAccountWithEmailAndPassword(email, password);
                }
            } else {
                mAlertDialog.cancel();
            }
        });
        mDialogBinding.buttonCancel.setOnClickListener(v -> mAlertDialog.cancel());

        mAlertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mDialogBinding = null;
    }
}