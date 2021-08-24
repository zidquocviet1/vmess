package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.mqv.realtimechatapplication.R;

public class PreferenceManageAccountsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference_manage_accounts);

        findViewById(R.id.button_back).setOnClickListener(v -> onBackPressed());
    }
}