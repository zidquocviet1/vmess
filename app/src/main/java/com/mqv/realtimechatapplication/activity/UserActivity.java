package com.mqv.realtimechatapplication.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

public class UserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        FirebaseUser user = getIntent().getParcelableExtra(Const.EXTRA_USER_INFO);
        FirebaseAuth auth = FirebaseAuth.getInstance();

        findViewById(R.id.button_update).setOnClickListener(v -> {
            // TODO: call upload photo to the Spring server if success then call reload user
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(getApplicationContext(), "Reload User", Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.button_log_out).setOnClickListener(v -> {
            auth.signOut();

            var loginIntent = new Intent(UserActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(loginIntent);

            finish();
        });

        findViewById(R.id.button_get_token).setOnClickListener(v -> {
            user.getIdToken(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            var result = task.getResult();
                            if (result != null){
                                Logging.show(result.getToken());
                            }
                        }
                    });
        });
    }
}