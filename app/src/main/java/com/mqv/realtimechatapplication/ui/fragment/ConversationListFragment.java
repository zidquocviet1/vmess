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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ConversationActivity;
import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.SearchConversationActivity;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter;
import com.mqv.realtimechatapplication.ui.adapter.RankUserConversationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.util.RingtoneUtil;

import java.util.ArrayList;
import java.util.List;

public class ConversationListFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding>
        implements ConversationDialogFragment.ConversationOptionListener {
    private List<Conversation> mConversationList = new ArrayList<>();

    private ConversationListAdapter mConversationAdapter;
    private RankUserConversationAdapter mRankUserAdapter;

    private static final String MESSAGE_RINGTONE = "message_receive.mp3";

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentConversationBinding.inflate(inflater, container, false);
    }

    @NonNull
    @Override
    public Class<ConversationFragmentViewModel> getViewModelClass() {
        return ConversationFragmentViewModel.class;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupRecyclerView();
        registerEvent();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setupObserver() {
        mViewModel.getRankUserListSafe().observe(this, remoteUsers -> {
            if (remoteUsers != null && !remoteUsers.isEmpty()) {
                mRankUserAdapter.submitList(remoteUsers);

                stopRefresh();
            }
        });

        mViewModel.getRefreshConversationResult().observe(this, result -> {
            if (result == null) return;

            mBinding.swipeMessages.setRefreshing(result.getStatus() == NetworkStatus.LOADING);

            if (result.getStatus() == NetworkStatus.ERROR) {
                Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.getConversationListObserver().observe(this, updatedList -> {
            if (updatedList == null) return;

            mConversationList = updatedList;
            mConversationAdapter.submitList(new ArrayList<>(mConversationList));
        });

        mViewModel.getConversationInserted().observe(this, id -> {
            if (id != null) {
                RingtoneUtil.open(requireContext(), MESSAGE_RINGTONE);
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
        MainActivity activity = (MainActivity) requireActivity();

        if (activity.getNetworkStatus()) {
            mViewModel.onRefresh();
        } else {
            Toast.makeText(requireContext(), "Fetching new conversation when you are offline.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.forceClearDispose();
    }

    private void setupRecyclerView() {
        mConversationAdapter = new ConversationListAdapter(mConversationList, getContext());
        mRankUserAdapter = new RankUserConversationAdapter(getContext());

        mBinding.recyclerMessages.setAdapter(mConversationAdapter);
        mBinding.recyclerMessages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerMessages.setItemAnimator(new DefaultItemAnimator());
        mBinding.recyclerMessages.setNestedScrollingEnabled(false);
        mBinding.recyclerMessages.setHasFixedSize(false);

        mBinding.recyclerRankChat.setAdapter(mRankUserAdapter);
        mBinding.recyclerRankChat.setLayoutManager(new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false));
        mBinding.recyclerRankChat.setItemAnimator(new DefaultItemAnimator());
    }

    private void registerEvent() {
        mBinding.searchButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), SearchConversationActivity.class)));

        mConversationAdapter.registerOnConversationClick((pos, isLongClicked) -> {
            Conversation conversation = mConversationList.get(pos);

            if (isLongClicked) {
                onConversationLongClicked(conversation);
            } else {
                onConversationClicked(conversation);
            }
        });
    }

    private void onConversationClicked(Conversation conversation) {
        Intent conversationIntent = new Intent(requireContext(), ConversationActivity.class);
        conversationIntent.putExtra("conversation", conversation);

        MainActivity activity = ((MainActivity) requireActivity());

        activity.activityResultLauncher.launch(conversationIntent, result -> {
            if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();

                if (intent != null) {
                    Conversation updated = intent.getParcelableExtra(ConversationActivity.EXTRA_CONVERSATION);

                    mConversationList.set(mConversationList.indexOf(updated), updated);
                    mConversationAdapter.submitList(new ArrayList<>(mConversationList));
                }
            }
        });
    }

    private void onConversationLongClicked(Conversation conversation) {
        ConversationDialogFragment dialog = ConversationDialogFragment.newInstance(this, conversation);
        dialog.show(requireActivity().getSupportFragmentManager(), null);
    }

    @Override
    public void onArchive(Conversation conversation) {
        removeConversationAdapterUI(conversation);
        mViewModel.changeConversationStatusType(conversation, ConversationStatusType.ARCHIVED);
    }

    @Override
    public void onDelete(Conversation conversation) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_delete_conversation_title)
                .setMessage(R.string.msg_delete_conversation_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    dialog.dismiss();
                    removeConversationAdapterUI(conversation);
                    mViewModel.delete(conversation);
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.background_alert_dialog_corner_radius))
                .create()
                .show();
    }

    @Override
    public void onMuteNotification(Conversation conversation) {

    }

    @Override
    public void onCreateGroup(Conversation conversation) {

    }

    @Override
    public void onMarkUnread(Conversation conversation) {

    }

    @Override
    public void onIgnore(Conversation conversation) {

    }

    @Override
    public void onLeaveGroup(Conversation conversation) {

    }

    @Override
    public void onAddMember(Conversation conversation) {

    }

    private void removeConversationAdapterUI(Conversation conversation) {
        mConversationList.remove(conversation);
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
    }
}