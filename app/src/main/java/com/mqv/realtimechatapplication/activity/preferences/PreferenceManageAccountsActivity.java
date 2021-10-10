package com.mqv.realtimechatapplication.activity.preferences;

import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.LoginActivity;
import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.RegisterActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.ManageAccountViewModel;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.data.model.SignInProvider;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceManageAccountsBinding;
import com.mqv.realtimechatapplication.databinding.DialogRemoveItemBinding;
import com.mqv.realtimechatapplication.databinding.DialogSwitchAccountBinding;
import com.mqv.realtimechatapplication.ui.adapter.LoggedInUserAdapter;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceManageAccountsActivity extends
        ToolbarActivity<ManageAccountViewModel, ActivityPreferenceManageAccountsBinding>
        implements View.OnClickListener, TextView.OnEditorActionListener {
    private LoggedInUserAdapter mAdapter;
    private final List<HistoryLoggedInUser> mHistoryUserList = new ArrayList<>();
    private AlertDialog mAlertDialog;
    private DialogSwitchAccountBinding mDialogBinding;
    private static final int DEFAULT_REQUEST_EMAIL = 1;
    private static final int DEFAULT_REQUEST_PHONE = 2;
    private boolean isPendingLogin;
    private boolean isLoginSuccess;
    private FirebaseUser shouldSignInAgainUser;
    private FirebaseUser currentLoginUser;

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
    protected void onStop() {
        super.onStop();
        if (isPendingLogin) {
            shouldSignInAgainUser = mViewModel.getPreviousFirebaseUser();

            currentLoginUser = FirebaseAuth.getInstance().getCurrentUser();
            mViewModel.setLoginUserOnStop(currentLoginUser);
            FirebaseAuth.getInstance().signOut();

            mViewModel.signInAgainFirebaseUser(shouldSignInAgainUser);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isPendingLogin) {
            mViewModel.signInAgainFirebaseUser(currentLoginUser);
        } else {
            if (isLoginSuccess) {
                mViewModel.signInAgainFirebaseUser(currentLoginUser);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mDialogBinding = null;
        if (isPendingLogin)
            mViewModel.signInAgainFirebaseUser(shouldSignInAgainUser);
    }

    @Override
    public void setupObserver() {
        mViewModel.getHistoryUserList().observe(this, userList -> {
            if (userList == null || userList.isEmpty()) {
            } else {
                mHistoryUserList.clear();
                mHistoryUserList.addAll(userList.stream()
                        .sorted((o1, o2) -> o2.getLogin().compareTo(o1.getLogin()))
                .collect(Collectors.toList()));
            }
            mHistoryUserList.add(null);
            mAdapter.submitList(mHistoryUserList);
        });

        mViewModel.getLoginResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();
            showLoadingUi(status == NetworkStatus.LOADING);
            isPendingLogin = status == NetworkStatus.LOADING;

            if (status == NetworkStatus.SUCCESS) {
                isLoginSuccess = true;

                var mainIntent = new Intent(PreferenceManageAccountsActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainIntent);
                finishAffinity();
            } else if (status == NetworkStatus.ERROR) {
                Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.getVerifyResult().observe(this, historyLoggedInUser ->
                showCustomDialog(historyLoggedInUser, DEFAULT_REQUEST_PHONE));
    }

    private void showLoadingUi(boolean isLoading) {
        if (mAlertDialog != null && mDialogBinding != null) {
            if (isLoading) {
                mDialogBinding.progressBarLoading.setVisibility(View.VISIBLE);
                mDialogBinding.layoutInput.setVisibility(View.GONE);
                mDialogBinding.layoutEnterOtp.getRoot().setVisibility(View.GONE);
            } else {
                mAlertDialog.cancel();
            }
        }
    }

    private void setupRecyclerView() {
        mAdapter = new LoggedInUserAdapter(this, mHistoryUserList);
        mAdapter.setOnAddAccountClick(v -> {
            var addAccountIntent = new Intent(PreferenceManageAccountsActivity.this, LoginActivity.class);
            addAccountIntent.putExtra(LoginActivity.EXTRA_ACTION, LoginActivity.EXTRA_ADD_ACCOUNT);
            startActivity(addAccountIntent);
        });
        mAdapter.setOnChangeAccountClick(this::handleChangeLoginUser);
        mAdapter.setOnRemoveUser(user -> mViewModel.deleteHistoryUser(user));
        mBinding.recyclerLoggedInUser.setAdapter(mAdapter);
        mBinding.recyclerLoggedInUser.setLayoutManager(new LinearLayoutManager(this));
        // add item touch listener here
        var itemTouchCallback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                var position = viewHolder.getAdapterPosition();

                // check if this is an add account layout or not
                if ((position + 1) == mHistoryUserList.size())
                    return 0;

                // if current user is login, disable swipe to remove
                var item = mHistoryUserList.get(position);
                if (item.getLogin())
                    return 0;

                return makeMovementFlags(0, LEFT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                var view = getLayoutInflater().inflate(R.layout.dialog_remove_item, null, false);
                var dialog = new MaterialAlertDialogBuilder(PreferenceManageAccountsActivity.this)
                        .setView(view)
                        .setCancelable(false)
                        .create();

                var binding = DialogRemoveItemBinding.bind(view);
                var position = viewHolder.getAdapterPosition();
                var item = mHistoryUserList.get(position);

                binding.textSubtitle.setText(getString(R.string.msg_remove_history_account, item.getDisplayName()));
                binding.buttonDone.setOnClickListener(v -> {
                    dialog.cancel();
                    mAdapter.removeItem(position);
                    /*
                     * Force the Recyclerview don't cached any ViewHolder.
                     * Because we use different ViewTypes. So we need to create a new ViewHolder infinitely
                     * */
                    mBinding.recyclerLoggedInUser.getRecycledViewPool().clear();
                });
                binding.buttonCancel.setOnClickListener(v -> {
                    dialog.cancel();
                    mAdapter.notifyItemChanged(position);
                });

                dialog.show();
            }

            @Override
            public int convertToAbsoluteDirection(int flags, int layoutDirection) {
                return super.convertToAbsoluteDirection(flags, layoutDirection);
            }
        };
        var itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mBinding.recyclerLoggedInUser);
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.includedButton.buttonBottom.getId()) {
            startActivity(RegisterActivity.class);
        } else if (id == mDialogBinding.textForgotPassword.getId()) {
            Logging.show("Text forgot on click");
        } else if (id == mDialogBinding.buttonCancel.getId() || id == mDialogBinding.layoutEnterOtp.buttonCancel.getId()) {
            mAlertDialog.cancel();
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
                showCustomDialog(historyUser, DEFAULT_REQUEST_EMAIL);
            } else if (signInProvider == SignInProvider.PHONE) {
                mViewModel.requestVerifyPhoneAuth(this, historyUser);
            }
        }
    }

    private void showCustomDialog(HistoryLoggedInUser historyUser, int request) {
        var view = getLayoutInflater().inflate(R.layout.dialog_switch_account, null, false);
        mDialogBinding = DialogSwitchAccountBinding.bind(view);

        mAlertDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        /*
         * Session switch account using email and password
         * Register all of the event to handle the necessary input
         * */
        mDialogBinding.editTextPassword.setOnEditorActionListener(this);
        mDialogBinding.textForgotPassword.setOnClickListener(this);
        mDialogBinding.buttonCancel.setOnClickListener(this);
        mDialogBinding.textTitle.setText(getString(R.string.title_continue_as, historyUser.getDisplayName()));
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

        /*
         * Register event in the verification phone number session
         * */
        mDialogBinding.layoutEnterOtp.editTextOtp.setOnEditorActionListener(this);
        mDialogBinding.layoutEnterOtp.buttonCancel.setOnClickListener(this);
        mDialogBinding.layoutEnterOtp.textTitle.setText(getString(R.string.title_continue_as, historyUser.getDisplayName()));
        mDialogBinding.layoutEnterOtp.buttonDone.setOnClickListener(v -> {
            var imm = getSystemService(InputMethodManager.class);
            imm.hideSoftInputFromWindow(mDialogBinding.layoutEnterOtp.editTextOtp.getWindowToken(), 0);

            var verifyCode = mDialogBinding.layoutEnterOtp.editTextOtp.getText().toString().trim();
            if (!verifyCode.equals("")) {
                mViewModel.switchAccountWithPhoneNumber(verifyCode);
            } else {
                mAlertDialog.cancel();
            }
        });
        /*
         * Detect which layout should show for user in order to input
         * */
        if (request == DEFAULT_REQUEST_EMAIL) {
            mDialogBinding.layoutInput.setVisibility(View.VISIBLE);
            mDialogBinding.layoutEnterOtp.getRoot().setVisibility(View.GONE);
        } else if (request == DEFAULT_REQUEST_PHONE) {
            mDialogBinding.layoutInput.setVisibility(View.GONE);
            mDialogBinding.layoutEnterOtp.getRoot().setVisibility(View.VISIBLE);
        }
        mDialogBinding.progressBarLoading.setVisibility(View.GONE);

        mAlertDialog.show();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        EditText editText;

        if (v.getId() == mDialogBinding.editTextPassword.getId()) {
            editText = mDialogBinding.editTextPassword;
        } else {
            editText = mDialogBinding.layoutEnterOtp.editTextOtp;
        }

        if (actionId == EditorInfo.IME_ACTION_DONE) {
            var imm = getSystemService(InputMethodManager.class);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            return true;
        }
        return false;
    }
}