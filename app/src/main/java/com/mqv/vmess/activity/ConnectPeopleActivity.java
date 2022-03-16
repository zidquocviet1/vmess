package com.mqv.vmess.activity;

import static com.mqv.vmess.network.firebase.MessagingService.EXTRA_ACTION_ACCEPTED;
import static com.mqv.vmess.network.firebase.MessagingService.EXTRA_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.vmess.databinding.ActivityConnectPeopleBinding;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.AlertDialogUtil;
import com.mqv.vmess.work.MarkNotificationReadWorker;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectPeopleActivity extends BaseActivity<ConnectPeopleViewModel, ActivityConnectPeopleBinding> {

    @Override
    public void binding() {
        mBinding = ActivityConnectPeopleBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConnectPeopleViewModel> getViewModelClass() {
        return ConnectPeopleViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        var navController = Objects.requireNonNull(navHostFragment).getNavController();
        NavigationUI.setupWithNavController(mBinding.bottomNav, navController);

        triggerOpenFromNotification(getIntent());
    }

    @Override
    public void setupObserver() {
        mViewModel.getConnectUserIdResult().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
                    break;
                case SUCCESS:
                    AlertDialogUtil.finishLoadingDialog();

                    var user = result.getSuccess();
                    var firebaseUser = mViewModel.getFirebaseUser().getValue();

                    if (firebaseUser != null) {
                        if (user.getUid().equals(firebaseUser.getUid())) {
                            Toast.makeText(this, R.string.msg_request_yourself, Toast.LENGTH_SHORT).show();
                        } else {
                            var intent = new Intent(this, RequestPeopleActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                        }
                        this.finish();
                    }
                    this.finish();
                    break;
                case ERROR:
                    AlertDialogUtil.finishLoadingDialog();

                    Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();

                    this.finish();
                    break;
            }
        });
    }

    private void triggerOpenFromNotification(Intent intent) {
        var extra = intent.getStringExtra(EXTRA_KEY);

        if (extra != null && extra.equals(EXTRA_ACTION_ACCEPTED)) {
            var uid = intent.getStringExtra(Const.KEY_UID);
            var agentId = intent.getStringExtra(Const.KEY_AGENT_ID);
            var imageUrl = intent.getStringExtra(Const.KEY_IMAGE_URL);

            mViewModel.getConnectUserByUid(agentId);

            var constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            var workData = new Data.Builder()
                    .putString(Const.KEY_UID, uid)
                    .putString(Const.KEY_AGENT_ID, agentId)
                    .putString(Const.KEY_IMAGE_URL, imageUrl)
                    .putString("type", Const.DEFAULT_ACCEPTED_FRIEND_REQUEST)
                    .build();

            var workRequest = new OneTimeWorkRequest.Builder(MarkNotificationReadWorker.class)
                    .setConstraints(constraint)
                    .setInputData(workData)
                    .build();

            WorkManager.getInstance(this)
                    .enqueueUniqueWork("mark_notification_read", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }
}