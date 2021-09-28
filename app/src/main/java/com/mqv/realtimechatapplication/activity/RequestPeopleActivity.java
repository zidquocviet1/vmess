package com.mqv.realtimechatapplication.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Logging;

public class RequestPeopleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_people);

        var intent = getIntent();

        var user = (User) intent.getParcelableExtra("user");

        Logging.show(user.getUid());
    }
}