package com.mqv.vmess.activity.preferences;

import static com.mqv.vmess.network.model.type.FriendRequestStatus.CANCEL;
import static com.mqv.vmess.network.model.type.FriendRequestStatus.CONFIRM;
import static com.mqv.vmess.network.model.type.FriendRequestStatus.PENDING;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.RequestPeopleActivity;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.activity.br.MarkNotificationReadReceiver;
import com.mqv.vmess.activity.viewmodel.FriendRequestViewModel;
import com.mqv.vmess.databinding.ActivityPreferenceFriendRequestBinding;
import com.mqv.vmess.network.model.FriendRequest;
import com.mqv.vmess.network.model.type.FriendRequestStatus;
import com.mqv.vmess.ui.adapter.FriendRequestAdapter;
import com.mqv.vmess.util.AlertDialogUtil;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.work.NewConversationWorkWrapper;
import com.mqv.vmess.work.WorkDependency;

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
                        mMutableList.clear();
                        mMutableList.addAll(list.stream().map(FriendRequest::new).collect(Collectors.toList()));
                        mAdapter.submitList(mMutableList);
                        mBinding.imageError.setVisibility(View.GONE);
                        mBinding.textError.setVisibility(View.GONE);
                    }
                    break;
            }

        });

        mViewModel.getResponseRequestResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    AlertDialogUtil.finishLoadingDialog();

                    mAdapter.removeItem(currentItemPosition);

                    setResult(RESULT_OK);

                    FriendRequest request = result.getSuccess();

                    if (request.getStatus() == CONFIRM) {
                        confirmFriendRequest(request.getReceiverId());
                        Data data = new Data.Builder()
                                            .putString("otherId", request.getReceiverId())
                                            .build();
                        WorkDependency.enqueue(new NewConversationWorkWrapper(this, data));
                    }
                    removeRequestNotification(request.getReceiverId());
                    break;
                case ERROR:
                    AlertDialogUtil.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        mViewModel.getConnectUserResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    AlertDialogUtil.finishLoadingDialog();

                    var intent = new Intent(this, RequestPeopleActivity.class);
                    intent.putExtra("user", result.getSuccess());

                    activityResultLauncher.launch(intent, data -> {
                        if (data != null && data.getResultCode() == RESULT_OK) {
                            var item = mAdapter.getCurrentList().get(currentItemPosition);
                            mAdapter.removeItem(currentItemPosition);

                            confirmFriendRequest(item.getSenderId());
                            removeRequestNotification(item.getSenderId());
                        }
                    });
                    break;
                case ERROR:
                    AlertDialogUtil.finishLoadingDialog();

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
                break;
            case CONFIRM:
                mViewModel.responseFriendRequest(new FriendRequest(request.getReceiverId(), request.getSenderId(), CONFIRM));
                break;
        }
    }

    private void confirmFriendRequest(String uid) {
        mViewModel.confirmFriendRequest(uid);
    }

    private void removeRequestNotification(String senderId) {
        mViewModel.removeRequestNotification(senderId);
    }

    private void triggerOpenFromNotification(Intent intent) {
        Intent broadcastMarkRead = intent.getParcelableExtra(MarkNotificationReadReceiver.EXTRA_INTENT);

        if (broadcastMarkRead != null) {
            sendBroadcast(broadcastMarkRead);
        }
    }
}