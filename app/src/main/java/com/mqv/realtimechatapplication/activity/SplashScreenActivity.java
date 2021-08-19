package com.mqv.realtimechatapplication.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.util.Const;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            var intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(Const.EXTRA_USER_INFO, user);
            startActivity(intent);
        }else {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finishAfterTransition();
    }
}