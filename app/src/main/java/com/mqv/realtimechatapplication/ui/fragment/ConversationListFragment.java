package com.mqv.realtimechatapplication.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mqv.realtimechatapplication.activity.ConversationActivity;
import com.mqv.realtimechatapplication.activity.MainActivity;
import com.mqv.realtimechatapplication.activity.SearchConversationActivity;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter;
import com.mqv.realtimechatapplication.ui.adapter.RankUserConversationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel;
import com.mqv.realtimechatapplication.util.MyActivityForResult;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationListFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding> {
    private ConversationListAdapter mConversationAdapter;
    private RankUserConversationAdapter mRankUserAdapter;
    private final List<Conversation> mConversationList = new ArrayList<>();

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        registerEvent();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setupObserver() {
        mViewModel.getRankUserListSafe().observe(this, remoteUsers -> {
            if (remoteUsers != null && !remoteUsers.isEmpty()) {
                mRankUserAdapter.submitList(remoteUsers);

                stopRefresh();
            }
        });

        mViewModel.getInboxConversation().observe(this, inbox -> {
            if (inbox != null) {
                if (inbox.isEmpty()) {
                    mConversationList.clear();
                    mConversationAdapter.notifyDataSetChanged();
                } else {
                    mConversationList.clear();
                    mConversationList.addAll(inbox.stream()
                                                  .sorted((o1, o2) -> o2.getLastChat()
                                                                        .getTimestamp()
                                                                        .compareTo(o1.getLastChat().getTimestamp()))
                                                  .collect(Collectors.toList()));
                    mConversationAdapter.notifyItemRangeChanged(0, inbox.size());
                }
            }
        });

        mViewModel.getCachedConversation().observe(this, conversation -> {
            if (conversation == null)
                return;

            int index = mConversationList.indexOf(conversation);

            mConversationList.set(index, conversation);
            mConversationAdapter.notifyItemChanged(index, "last_chat");
        });

        mViewModel.getRefreshConversationResult().observe(this, result -> {
            if (result == null)
                return;

            NetworkStatus status = result.getStatus();

            switch (status) {
                case ERROR:
                    mBinding.swipeMessages.setRefreshing(false);

                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                case TERMINATE:
                    mBinding.swipeMessages.setRefreshing(false);
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
        mConversationAdapter.submitList(mConversationList);

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

        mConversationAdapter.registerOnConversationClick(position -> {
            Conversation conversation = mConversationList.get(position);

            Intent conversationIntent = new Intent(requireContext(), ConversationActivity.class);
            conversationIntent.putExtra("conversation", conversation);

            MainActivity activity = ((MainActivity) requireActivity());

            activity.activityResultLauncher.launch(conversationIntent, result -> {
                if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();

                    if (intent != null) {
                        Conversation updated = intent.getParcelableExtra("conversation");

                        mViewModel.fetchCachedConversation(updated);
                    }
                }
            });
        });
    }
}
