package com.mqv.vmess.activity;

import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.DISPLAY_NAME_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.EMAIL_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.PASSWORD_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.RE_PASSWORD_ERROR;
import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.SUCCESS;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.RegisterViewModel;
import com.mqv.vmess.databinding.ActivityRegisterBinding;
import com.mqv.vmess.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends BaseActivity<RegisterViewModel, ActivityRegisterBinding> {
    private EditText emailEdit, displayNameEdit, passwordEdit, rePasswordEdit;
    private AlertDialog loadingDialog;
    private static final String STATE_EMAIL = "email";
    private static final String STATE_DISPLAY_NAME = "display_name";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_RE_PASSWORD = "re-password";

    @Override
    public void binding() {
        mBinding = ActivityRegisterBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<RegisterViewModel> getViewModelClass() {
        return RegisterViewModel.class;
    }

    @Override
    protected void onCreate(Bundle outState) {
        super.onCreate(outState);

        emailEdit = Objects.requireNonNull(mBinding.textLayoutUsername.getEditText());
        displayNameEdit = Objects.requireNonNull(mBinding.textLayoutDisplayName.getEditText());
        passwordEdit = Objects.requireNonNull(mBinding.textLayoutPassword.getEditText());
        rePasswordEdit = Objects.requireNonNull(mBinding.textLayoutRePassword.getEditText());

        setupEvent();

        if (outState != null) {
            emailEdit.setText(outState.getString(STATE_EMAIL));
            displayNameEdit.setText(outState.getString(STATE_DISPLAY_NAME));
            passwordEdit.setText(outState.getString(STATE_PASSWORD));
            rePasswordEdit.setText(outState.getString(STATE_RE_PASSWORD));
        }
    }

    @Override
    protected void onDestroy() {
        finishLoadingDialog();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_EMAIL, emailEdit.getText().toString().trim());
        outState.putString(STATE_DISPLAY_NAME, displayNameEdit.getText().toString().trim());
        outState.putString(STATE_PASSWORD, passwordEdit.getText().toString().trim());
        outState.putString(STATE_RE_PASSWORD, rePasswordEdit.getText().toString().trim());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setupObserver() {
        mViewModel.getRegisterValidationResult().observe(this, result -> {
            if (result == null)
                return;

            mBinding.buttonRegister.setEnabled(result == SUCCESS);

            if (result == EMAIL_ERROR) {
                mBinding.textLayoutUsername.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutUsername.setErrorEnabled(false);
            }

            if (result == DISPLAY_NAME_ERROR) {
                mBinding.textLayoutDisplayName.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutDisplayName.setErrorEnabled(false);
            }

            if (result == PASSWORD_ERROR) {
                mBinding.textLayoutPassword.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutPassword.setErrorEnabled(false);
            }

            if (result == RE_PASSWORD_ERROR) {
                mBinding.textLayoutRePassword.setError(getString(result.getMessage()));
            } else {
                mBinding.textLayoutRePassword.setErrorEnabled(false);
            }
        });

        mViewModel.getRegisterResult().observe(this, result -> {
            if (result == null) return;

            if (result.getStatus() == NetworkStatus.LOADING) {
                startLoadingDialog();
            } else if (result.getStatus() == NetworkStatus.SUCCESS) {
                finishLoadingDialog();

                Toast.makeText(this, result.getSuccess(), Toast.LENGTH_SHORT).show();

                finish();
            } else {
                finishLoadingDialog();

                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();

                emailEdit.requestFocus();
                if (mBinding != null)
                    mBinding.textLayoutUsername.setError(getString(R.string.invalid_email_exists));
            }
        });
    }

    private void setupEvent() {
        var textChanged = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mViewModel.registerDataChanged(
                        emailEdit.getText().toString().trim(),
                        displayNameEdit.getText().toString().trim(),
                        passwordEdit.getText().toString().trim(),
                        rePasswordEdit.getText().toString().trim()
                );
            }
        };

        emailEdit.addTextChangedListener(textChanged);
        displayNameEdit.addTextChangedListener(textChanged);
        passwordEdit.addTextChangedListener(textChanged);
        rePasswordEdit.addTextChangedListener(textChanged);

        mBinding.buttonRegister.setOnClickListener((v) -> {
            var email = emailEdit.getText().toString().trim();
            var password = passwordEdit.getText().toString().trim();
            var displayName = displayNameEdit.getText().toString().trim();

            mViewModel.createUserWithEmailAndPassword(email, password, displayName);
        });
    }

    private void startLoadingDialog() {
        var builder = new MaterialAlertDialogBuilder(this);
        var view = getLayoutInflater().inflate(R.layout.dialog_loading_with_text, null, false);
        var textUploading = (TextView) view.findViewById(R.id.text_uploading);
        var animBlink = AnimationUtils.loadAnimation(this, R.anim.blink);

        textUploading.setText(R.string.action_creating);
        textUploading.startAnimation(animBlink);
        builder.setView(view);

        loadingDialog = builder.create();
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
    }

    private void finishLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing())
            loadingDialog.dismiss();
    }
}