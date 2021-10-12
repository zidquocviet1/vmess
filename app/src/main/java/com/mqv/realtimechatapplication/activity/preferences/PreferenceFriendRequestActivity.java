package com.mqv.realtimechatapplication.activity.preferences;

import static com.mqv.realtimechatapplication.network.firebase.MessagingService.EXTRA_ACTION_NEW_FRIEND;
import static com.mqv.realtimechatapplication.network.firebase.MessagingService.EXTRA_KEY;
import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.CANCEL;
import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.CONFIRM;
import static com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus.PENDING;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.RequestPeopleActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.FriendRequestViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceFriendRequestBinding;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.ui.adapter.FriendRequestAdapter;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.LoadingDialog;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.work.FetchNotificationWorker;
import com.mqv.realtimechatapplication.work.MarkNotificationReadWorker;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceFriendRequestActivity extends ToolbarActivity<FriendRequestViewModel, ActivityPreferenceFriendRequestBinding> {
    private FriendRequestAdapter mAdapter;
    private final List<FriendRequest> mMutableList = new ArrayList<>();
    private int currentItemPosition;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceFriendRequestBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<FriendRequestViewModel> getViewModelClass() {
        return FriendRequestViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_friend_request);

        setupRecyclerView();

        triggerOpenFromNotification(getIntent());
    }

    @Override
    public void setupObserver() {
        mViewModel.getFriendRequestList().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            showLoadingUi(status == NetworkStatus.LOADING);

            switch (status) {
                case ERROR:
                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();

                    mBinding.recyclerViewRequest.setVisibility(View.GONE);
                    mBinding.imageError.setVisibility(View.VISIBLE);
                    mBinding.textError.setVisibility(View.VISIBLE);
                    mBinding.imageError.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.image_server_break_down));
                    mBinding.textError.setText(R.string.msg_oop_something_when_wrong);
                    break;
                case SUCCESS:
                    var list = result.getSuccess();
                    if (list == null || list.isEmpty()) {
                        mAdapter.submitList(new ArrayList<>());
                        mBinding.recyclerViewRequest.setVisibility(View.GONE);
                        mBinding.imageError.setVisibility(View.VISIBLE);
                        mBinding.textError.setVisibility(View.VISIBLE);
                        mBinding.imageError.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.image_no_data));
                        mBinding.textError.setText(R.string.msg_friend_request_empty_list);
                    } else {
                        mMutableList.addAll(list.stream().map(FriendRequest::new).collect(Collectors.toList()));
                        mAdapter.submitList(mMutableList);
                    }
                    break;
            }

        });

        mViewModel.getResponseRequestResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    mAdapter.removeItem(currentItemPosition);

                    setResult(RESULT_OK);
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        mViewModel.getConnectUserResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    var intent = new Intent(this, RequestPeopleActivity.class);
                    intent.putExtra("user", result.getSuccess());

                    activityResultLauncher.launch(intent, data -> {
                        if (data != null && data.getResultCode() == RESULT_OK) {
                            var item = mAdapter.getCurrentList().get(currentItemPosition);

                            confirmFriendRequest(item.getSenderId(), item.getPhotoUrl(), item.getDisplayName());

                            mAdapter.removeItem(currentItemPosition);
                        }
                    });
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void showLoadingUi(boolean isLoading) {
        mBinding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.recyclerViewRequest.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void setupRecyclerView() {
        mAdapter = new FriendRequestAdapter(this, mMutableList,
                confirmPosition -> handleAdapterClicked(confirmPosition, CONFIRM),
                cancelPosition -> handleAdapterClicked(cancelPosition, CANCEL),
                size -> {
                    if (size > 0) {
                        mBinding.recyclerViewRequest.setVisibility(View.VISIBLE);
                        mBinding.imageError.setVisibility(View.GONE);
                        mBinding.textError.setVisibility(View.GONE);
                    } else {
                        mBinding.recyclerViewRequest.setVisibility(View.GONE);
                        mBinding.imageError.setVisibility(View.VISIBLE);
                        mBinding.textError.setVisibility(View.VISIBLE);
                        mBinding.imageError.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.image_no_data));
                        mBinding.textError.setText(R.string.msg_friend_request_empty_list);
                    }
                });
        mAdapter.setOnRequestClicked(position -> handleAdapterClicked(position, PENDING));

        mBinding.recyclerViewRequest.setAdapter(mAdapter);
        mBinding.recyclerViewRequest.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewRequest.setHasFixedSize(true);
    }

    private void handleAdapterClicked(int position, FriendRequestStatus status) {
        var request = mAdapter.getCurrentList().get(position);

        currentItemPosition = position;

        switch (status) {
            case PENDING:
                mViewModel.getConnectUserByUid(request.getSenderId());
                break;
            case CANCEL:
                mViewModel.responseFriendRequest(new FriendRequest(request.getReceiverId(), request.getSenderId(), CANCEL));

                fetchNotificationWithWork();
                break;
            case CONFIRM:
                confirmFriendRequest(request.getSenderId(), request.getPhotoUrl(), request.getDisplayName());

                mViewModel.responseFriendRequest(new FriendRequest(request.getReceiverId(), request.getSenderId(), CONFIRM));

                fetchNotificationWithWork();
                break;
        }
    }

    private void confirmFriendRequest(String uid, String photoUrl, String displayName) {
        var people = new People(uid, displayName, photoUrl, null, LocalDateTime.now().minusMinutes(11));

        mViewModel.confirmFriendRequest(people);
    }

    private void triggerOpenFromNotification(Intent intent) {
        var extra = intent.getStringExtra(EXTRA_KEY);
        if (extra != null && extra.equals(EXTRA_ACTION_NEW_FRIEND)) {
            var uid = intent.getStringExtra(Const.KEY_UID);
            var agentId = intent.getStringExtra(Const.KEY_AGENT_ID);
            var imageUrl = intent.getStringExtra(Const.KEY_IMAGE_URL);

            var constraint = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            var workData = new Data.Builder()
                    .putString(Const.KEY_UID, uid)
                    .putString(Const.KEY_AGENT_ID, agentId)
                    .putString(Const.KEY_IMAGE_URL, imageUrl)
                    .putString("type", Const.DEFAULT_NEW_FRIEND_REQUEST)
                    .build();

            var workRequest = new OneTimeWorkRequest.Builder(MarkNotificationReadWorker.class)
                    .setConstraints(constraint)
                    .setInputData(workData)
                    .build();

            WorkManager.getInstance(this)
                    .enqueueUniqueWork("mark_notification_read", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }

    private void fetchNotificationWithWork() {
        var constraint =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        var workRequest =
                new OneTimeWorkRequest.Builder(FetchNotificationWorker.class)
                        .setConstraints(constraint)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork("notification_worker", ExistingWorkPolicy.REPLACE, workRequest);
    }
}