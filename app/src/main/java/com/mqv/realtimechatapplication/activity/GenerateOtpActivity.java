package com.mqv.realtimechatapplication.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ActivityGenerateOtpBinding;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class GenerateOtpActivity extends AppCompatActivity {
    private ActivityGenerateOtpBinding mBinding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityGenerateOtpBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mAuth = FirebaseAuth.getInstance();
//        mAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true); // remove this when release
        mAuth.useAppLanguage(); // To apply the default app language, if you wanna use explicit setting using the setLanguageCode method
        setupEvent();
    }

    private void setupEvent(){
        mBinding.editTextPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                var isCorrect = isPhoneFormatCorrect(s.toString().trim());

                if (!isCorrect){
                    mBinding.editTextPhone.setError(getString(R.string.invalid_phone_format));
                }

                mBinding.buttonSend.setEnabled(isCorrect);
            }
        });

        mBinding.buttonSend.setOnClickListener(v -> {
            var phoneText = mBinding.editTextPhone.getText().toString().trim();

            /*
            * If the app is published for all the country in the world.
            * Change the prefix by their region
            * Maybe modify the UI again*/

            var reformat = "+84" + phoneText.substring(1);

            var phoneOptions = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(reformat)
                    .setTimeout(Const.PHONE_AUTH_TIME_OUT, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                mBinding.buttonSend.setEnabled(true);
                                mBinding.progressBarLoading.setVisibility(View.GONE);
                                Toast.makeText(getApplicationContext(), R.string.error_phone_verification, Toast.LENGTH_SHORT).show();
                            }, 1500);
                        }

                        @Override
                        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            super.onCodeSent(s, forceResendingToken);

                            var intent = new Intent(GenerateOtpActivity.this, VerifyOtpActivity.class);
                            intent.putExtra(Const.EXTRA_VERIFICATION_ID, s);
                            intent.putExtra(Const.EXTRA_RESEND_TOKEN, forceResendingToken);
                            intent.putExtra(Const.EXTRA_RESEND_PHONE_NUMBER, reformat);

                            startActivity(intent);
                            finish();
                        }
                    })
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(phoneOptions);
            mBinding.progressBarLoading.setVisibility(View.VISIBLE);
            mBinding.buttonSend.setEnabled(false);
        });
    }

    private boolean isPhoneFormatCorrect(String phone){
        return Pattern.compile(Const.PHONE_REGEX_PATTERN).matcher(phone).matches();
    }
}