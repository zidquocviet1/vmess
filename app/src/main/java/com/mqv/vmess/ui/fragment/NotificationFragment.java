package com.mqv.vmess.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.vmess.databinding.FragmentNotificationBinding;
import com.mqv.vmess.ui.adapter.NotificationAdapter;
import com.mqv.vmess.ui.data.FriendNotificationState;
import com.mqv.vmess.ui.fragment.viewmodel.NotificationFragmentViewModel;
import com.mqv.vmess.util.NetworkStatus;

import java.util.stream.Collectors;

public class NotificationFragment extends BaseSwipeFragment<NotificationFragmentViewModel, FragmentNotificationBinding> {
    private NotificationAdapter mAdapter;

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
        setupRecyclerView();
        registerEvent();
        registerFragmentResult();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setupObserver() {
        mViewModel.getRefreshResultSafe().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            if (result.getStatus() != NetworkStatus.LOADING)
                stopRefresh();
        });

        mViewModel.getListFriendNotificationState().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                emptyListUi(list.isEmpty());

                mAdapter.submitList(list.stream()
                        .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                        .collect(Collectors.toList()));
            }
        });

        mViewModel.getOneTimeEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                Integer result = event.getContentIfNotHandled();

                if (result != null) {
                    Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show();
                }
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

    private void emptyListUi(boolean isEmpty) {
        mBinding.textNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.imageNoData.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recyclerNotification.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupRecyclerView() {
        mAdapter = new NotificationAdapter(requireContext());
        mBinding.recyclerNotification.setAdapter(mAdapter);
        mBinding.recyclerNotification.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.recyclerNotification.setNestedScrollingEnabled(false);
    }

    private void registerEvent() {
        mAdapter.setOnItemClick(position -> {
            FriendNotificationState notification = mAdapter.getCurrentList().get(position);
            startActivity(notification.getIntentByType(requireContext()));
        });

        mAdapter.setOnChangeItem(position -> {
            FriendNotificationState notification = mAdapter.getCurrentList().get(position);
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
                        String action = result.getString(EXTRA_KEY_ACTION);
                        FriendNotificationState item = (FriendNotificationState) result.getParcelable(EXTRA_NOTIFICATION);

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

    private void onRemove(FriendNotificationState notification) {
        mViewModel.removeNotification(notification);
    }

    private void onReportProblem(FriendNotificationState notification) {
        Toast.makeText(requireContext(), "Your report has been sent to our Notification Team", Toast.LENGTH_SHORT).show();
    }

    private void onMarkRead(FriendNotificationState notification) {
        mViewModel.markAsRead(notification);
    }
}