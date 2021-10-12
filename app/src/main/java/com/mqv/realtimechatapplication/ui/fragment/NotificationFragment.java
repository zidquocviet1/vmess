package com.mqv.realtimechatapplication.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.preferences.PreferenceFriendRequestActivity;
import com.mqv.realtimechatapplication.databinding.FragmentNotificationBinding;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.ui.adapter.NotificationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.NotificationFragmentViewModel;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationFragment extends BaseSwipeFragment<NotificationFragmentViewModel, FragmentNotificationBinding> {
    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter mAdapter;
    private int mCurrentPosition;
    private boolean isPendingMarkRead;

    public static final String REQUEST_KEY = "notification_option";
    public static final String EXTRA_KEY_ACTION = "action";
    public static final String EXTRA_NOTIFICATION = "notification_item";
    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_REPORT = "report";
    public static final String ACTION_MARK_READ = "mark_read";

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentNotificationBinding.inflate(inflater, container, false);
    }

    @Override
    public Class<NotificationFragmentViewModel> getViewModelClass() {
        return NotificationFragmentViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel.loadNotificationUsingNBR();

        setupRecyclerView();
        registerEvent();
        registerFragmentResult();
    }

    @Override
    public void setupObserver() {
        mViewModel.getNotificationListResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status) {
                case ERROR:
                    break;
                case SUCCESS:
                    var data = result.getSuccess();
                    var deleteItemList = mViewModel.getDeleteItemList();

                    data.removeAll(deleteItemList);

                    retrieveNotificationList(result.getSuccess());
                    break;
            }
        });

        mViewModel.getRefreshResultSafe().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            if (status != NetworkStatus.LOADING)
                stopRefresh();

            switch (status) {
                case ERROR:
                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    retrieveNotificationList(result.getSuccess());
                    break;
            }
        });
    }

    @NonNull
    @Override
    public SwipeRefreshLayout getSwipeLayout() {
        return mBinding.swipeMessages;
    }

    @Override
    public void onRefresh() {
        mViewModel.onRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.forceClearDispose();
        mViewModel.forceDispose();
    }

    private void retrieveNotificationList(List<Notification> data) {
        emptyListUi(data == null || data.isEmpty());

        if (data != null) {
            var sorted = data.stream()
                    .sorted((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()))
                    .collect(Collectors.toList());

            notifications.clear();
            notifications.addAll(sorted);
            mAdapter.notifyItemRangeChanged(0, sorted.size());
        }
    }

    private void emptyListUi(boolean isEmpty) {
        mBinding.textNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.imageNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recyclerNotification.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupRecyclerView() {
        mAdapter = new NotificationAdapter(requireContext(), notifications);
        mAdapter.submitList(notifications); // Use submit list only one time when Adapter is created

        mBinding.recyclerNotification.setAdapter(mAdapter);
        mBinding.recyclerNotification.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.recyclerNotification.setNestedScrollingEnabled(false);
    }

    private void registerEvent() {
        mAdapter.setOnItemClick(position -> {
            var notification = mAdapter.getCurrentList().get(position);

            if (!notification.getHasRead()) {
                mCurrentPosition = position;
                isPendingMarkRead = true;
            }

            switch (notification.getType()) {
                case NEW_FRIEND_REQUEST:
                    var intent = new Intent(requireContext(), PreferenceFriendRequestActivity.class);

                    ((MainActivity) requireActivity()).activityResultLauncher
                            .launch(intent, result -> {
                                if (result != null && result.getResultCode() == Activity.RESULT_OK) {

                                } else {
                                    if (isPendingMarkRead) {
                                        var oldItem = mAdapter.getCurrentList().get(mCurrentPosition);
                                        oldItem.setHasRead(true);

                                        mAdapter.notifyItemChanged(mCurrentPosition);

                                        mViewModel.markAsRead(notification);

                                        isPendingMarkRead = false;
                                    }
                                }
                            });
                    break;
                case ACCEPTED_FRIEND_REQUEST:
                    break;
            }
        });

        mAdapter.setOnChangeItem(position -> {
            var notification = mAdapter.getCurrentList().get(position);

            NotificationOptionDialogFragment.newInstance(notification)
                    .show(requireActivity().getSupportFragmentManager(), "NOTIFICATION");
        });

        mAdapter.setOnDatasetChange(this::emptyListUi);
    }

    private void registerFragmentResult() {
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (requestKey.equals(REQUEST_KEY)) {
                        var action = result.getString(EXTRA_KEY_ACTION);
                        var item = (Notification) result.getParcelable(EXTRA_NOTIFICATION);

                        switch (action) {
                            case ACTION_REMOVE:
                                onRemove(item);
                                break;
                            case ACTION_REPORT:
                                onReportProblem(item);
                                break;
                            case ACTION_MARK_READ:
                                onMarkRead(item);
                                break;
                        }
                    }
                });
    }

    private void onRemove(Notification notification) {
        mAdapter.removeItem(notification);
        mViewModel.removeNotification(notification);

        mViewModel.addRemoveItemList(notification);

        Toast.makeText(requireContext(), "Notification removed", Toast.LENGTH_SHORT).show();
    }

    private void onReportProblem(Notification notification) {
        Toast.makeText(requireContext(), "Your report has been sent to our Notification Team", Toast.LENGTH_SHORT).show();
    }

    private void onMarkRead(Notification notification) {
        notification.setHasRead(true);
        mAdapter.notifyItemChanged(mAdapter.getCurrentList().indexOf(notification));
        mViewModel.markAsRead(notification);
    }
}