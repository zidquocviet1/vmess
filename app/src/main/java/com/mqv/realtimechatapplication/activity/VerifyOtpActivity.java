package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.VerifyOtpViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityVerifyOtpBinding;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class VerifyOtpActivity extends BaseActivity<VerifyOtpViewModel, ActivityVerifyOtpBinding> implements View.OnClickListener {
    private InputMethodManager mIMM;
    private String mVerificationId;
    private String mResendPhoneNumber;
    private EditText editText1, editText2, editText3, editText4, editText5, editText6;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private long timeOut = Const.PHONE_AUTH_TIME_OUT * 1000;
    private Timer timer;
    private Snackbar mSnackbar;
    private static final int PERIOD = 1000;

    @Override
    public void binding() {
        mBinding = ActivityVerifyOtpBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<VerifyOtpViewModel> getViewModelClass() {
        return VerifyOtpViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupEvent();

        mIMM = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mIMM.showSoftInput(mBinding.editText1, InputMethodManager.SHOW_IMPLICIT);

        mAuth = FirebaseAuth.getInstance();
        mVerificationId = getIntent().getStringExtra(Const.EXTRA_VERIFICATION_ID);
        mResendToken = getIntent().getParcelableExtra(Const.EXTRA_RESEND_TOKEN);
        mResendPhoneNumber = getIntent().getStringExtra(Const.EXTRA_RESEND_PHONE_NUMBER);
        Logging.show("verification id: " + mVerificationId);
        Logging.show("old resend token: " + mResendToken.toString());

        countDownTimeOut();
    }

    @Override
    public void setupObserver() {
        mViewModel.getOtpCodeFormState().observe(this, state -> mBinding.buttonVerify.setEnabled(state.isValid()));

        mViewModel.getTimeOut().observe(this, value -> {
            if (value >= 0){
                timeOut = value;

                mBinding.textTitle.setText(Html.fromHtml(getString(R.string.msg_verify_otp_title_dummy,
                        value / PERIOD),
                                Html.FROM_HTML_MODE_COMPACT));

                if (value == 0){
                    var snackBarDuration = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE?
                            Snackbar.LENGTH_SHORT : Snackbar.LENGTH_INDEFINITE;

                    mSnackbar = Snackbar.make(mBinding.constraintLayoutMain,
                            R.string.msg_verification_code_not_available, snackBarDuration)
                            .setActionTextColor(getColor(R.color.purple_500))
                            .setAction(R.string.action_resend, v -> resendCode());
                    mSnackbar.show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();
        if (id == mBinding.buttonVerify.getId()){
            var code = editText1.getText().toString()
                    + editText2.getText().toString()
                    + editText3.getText().toString()
                    + editText4.getText().toString()
                    + editText5.getText().toString()
                    + editText6.getText().toString();

            var credential = PhoneAuthProvider.getCredential(mVerificationId, code);

            mBinding.progressBarLoading.setVisibility(View.VISIBLE);
            mBinding.buttonVerify.setEnabled(false);
            mBinding.textErrorVerifyCode.setVisibility(View.GONE);

            signInWithPhoneAuthCredential(credential);
        }else if (id == mBinding.textResend.getId()){
            resendCode();
        }
    }

    private void countDownTimeOut(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mBinding == null) return;
                    if (mViewModel == null) return;

                    if (timeOut >= 0) {
                        mViewModel.timeOutChanged(timeOut);
                        timeOut -= PERIOD;
                    }
                });
            }
        }, 0, PERIOD);
    }

    private void restartCountDown(){
        mViewModel.timeOutChanged(Const.PHONE_AUTH_TIME_OUT * 1000);
        timer.cancel();
        countDownTimeOut();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // TODO: check the snippet code again
                        // Sign in success, update UI with the signed-in user's information
                        Logging.show("signInWithCredential:success");

                        FirebaseUser user = task.getResult().getUser();

                        // Update UI
                        var intent = new Intent(VerifyOtpActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            mBinding.progressBarLoading.setVisibility(View.GONE);
                            mBinding.buttonVerify.setEnabled(true);

                            // Sign in failed, display a message and update the UI
                            Log.w("TAC", "signInWithCredential:failure", task.getException());

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mBinding.textErrorVerifyCode.setVisibility(View.VISIBLE);
                            }
                        }, 1500);
                    }
                });
    }

    private void resendCode(){
        if (mSnackbar != null) mSnackbar.dismiss();
        mBinding.progressBarLoading.setVisibility(View.VISIBLE);

        var options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(mResendPhoneNumber)
                .setTimeout(Const.PHONE_AUTH_TIME_OUT, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                mBinding.progressBarLoading.setVisibility(View.GONE);
                                mBinding.buttonVerify.setEnabled(true);
                                mBinding.textErrorVerifyCode.setVisibility(View.VISIBLE);
                            }, 1500);
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            // The SMS quota for the project has been exceeded
                            Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        Logging.show("new code sent: " + s);

                        restartCountDown();
                        mBinding.progressBarLoading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        Logging.show("onCodeAutoRetrievalTimeOut: " + s);
                    }
                })
                .setForceResendingToken(mResendToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void setupEvent() {
        editText1 = mBinding.editText1;
        editText2 = mBinding.editText2;
        editText3 = mBinding.editText3;
        editText4 = mBinding.editText4;
        editText5 = mBinding.editText5;
        editText6 = mBinding.editText6;
        editText1.requestFocus();

        editText1.addTextChangedListener(new GenericTextWatcher(null, editText1, editText2));
        editText2.addTextChangedListener(new GenericTextWatcher(editText1, editText2, editText3));
        editText3.addTextChangedListener(new GenericTextWatcher(editText2, editText3, editText4));
        editText4.addTextChangedListener(new GenericTextWatcher(editText3, editText4, editText5));
        editText5.addTextChangedListener(new GenericTextWatcher(editText4, editText5, editText6));
        editText6.addTextChangedListener(new GenericTextWatcher(editText5, editText6, null));
        mBinding.textResend.setOnClickListener(this);
        mBinding.buttonVerify.setOnClickListener(this);
    }

    private class GenericTextWatcher implements TextWatcher{
        private final EditText prev, current, next;

        public GenericTextWatcher(EditText prev, EditText current, EditText next){
            this.prev = prev;
            this.current = current;
            this.next = next;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mViewModel.otpCodeChanged(editText1.getText().toString(),
                    editText2.getText().toString(),
                    editText3.getText().toString(),
                    editText4.getText().toString(),
                    editText5.getText().toString(),
                    editText6.getText().toString());

            if (TextUtils.isEmpty(s) && prev != null){
                prev.requestFocus();

                mIMM.showSoftInput(prev, InputMethodManager.SHOW_IMPLICIT);
                return;
            }else if (TextUtils.isEmpty(s) && prev == null)
                return;

            if (next != null)
                next.requestFocus();
            else {
                mIMM.hideSoftInputFromWindow(current.getWindowToken(), 0);

                current.clearFocus();
            }
        }
    }
}