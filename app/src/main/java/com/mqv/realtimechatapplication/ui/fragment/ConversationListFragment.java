package com.mqv.realtimechatapplication.ui.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.mqv.realtimechatapplication.activity.br.ConversationBroadcastReceiver;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter;
import com.mqv.realtimechatapplication.ui.adapter.RankUserConversationAdapter;
import com.mqv.realtimechatapplication.ui.fragment.viewmodel.ConversationFragmentViewModel;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.util.RingtoneUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConversationListFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding> implements ConversationDialogFragment.ConversationOptionListener {
    private final List<Conversation> mConversationList = new ArrayList<>();

    private ConversationListAdapter mConversationAdapter;
    private RankUserConversationAdapter mRankUserAdapter;

    private boolean isNewConversationAdded = false;

    private static final String FILE_MESSAGE_RINGTONE = "message_receive.mp3";

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

        ConversationBroadcastReceiver.OnConversationListener listener = new ConversationBroadcastReceiver.OnConversationListener() {
            @Override
            public void onNewConversationReceive(Conversation conversation) {
                mViewModel.submitConversation(conversation);

                isNewConversationAdded = true;
            }

            @Override
            public void onDeleteConversation(String unFriendUserId) {
                User user = Objects.requireNonNull(LoggedInUserManager.getInstance().getLoggedInUser());

                mViewModel.submitRemoveConversation(user.getUid(), unFriendUserId);
            }
        };

        // Load more conversation when the user is current this stage. And got accepted friend request
        BroadcastReceiver newConversationReceiver = new ConversationBroadcastReceiver(listener);

        requireContext().registerReceiver(newConversationReceiver, new IntentFilter("com.mqv.tac.NEW_CONVERSATION"));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupRecyclerView();
        registerEvent();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isNewConversationAdded) {
            RingtoneUtil.open(requireContext(), FILE_MESSAGE_RINGTONE);

            isNewConversationAdded = false;
        }
    }

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
                mConversationList.clear();

                if (!inbox.isEmpty()) {
                    mConversationList.addAll(inbox);
                }

                mConversationAdapter.submitList(new ArrayList<>(mConversationList));
            }
        });

        mViewModel.getCachedConversation().observe(this, conversation -> {
            if (conversation == null)
                return;

            onConversationUpdate(conversation);
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
                    List<Conversation> freshConversations = result.getSuccess();

                    if (freshConversations != null) {
                        mConversationList.removeAll(freshConversations);
                        if (!freshConversations.isEmpty()) {
                            mConversationList.addAll(0, freshConversations);
                        }
                        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
                    }
                case TERMINATE:
                    mBinding.swipeMessages.setRefreshing(false);
                    break;
            }
        });

        mViewModel.getNewChatConversation().observe(this, chat -> {
            if (chat != null) {
                mConversationList.stream()
                        .filter(c -> c.getId().equals(chat.getConversationId()))
                        .findFirst()
                        .ifPresent(c2 -> {
                            Conversation conversation = new Conversation(c2);
                            List<Chat> chats = conversation.getChats();

                            if (!chats.contains(chat)) {
                                chats.add(chat);
                                onConversationUpdate(conversation);
                            }
                        });
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
                    Conversation updated = intent.getParcelableExtra("conversation");

                    mViewModel.fetchCachedConversation(updated);
                }
            }
        });
    }

    private void onConversationLongClicked(Conversation conversation) {
        ConversationDialogFragment dialog = ConversationDialogFragment.newInstance(this, conversation);
        dialog.show(requireActivity().getSupportFragmentManager(), null);
    }

    private void onConversationUpdate(Conversation updatedConversation) {
        int index = mConversationList.indexOf(updatedConversation);

        if (index < 0 || index > mConversationList.size()) return;

        // Get the to check last chat and sort by the last chat
        List<Conversation> shouldSortList = mConversationList.subList(0, index);

        if (shouldSortList.isEmpty()) {
            mConversationList.set(0, updatedConversation);
        } else {
            shouldSortList.add(updatedConversation);
            List<Conversation> sortedList = shouldSortList.stream()
                                                          .sorted((o1, o2) -> o2.getLastChat()
                                                                  .getTimestamp()
                                                                  .compareTo(o1.getLastChat().getTimestamp()))
                                                          .collect(Collectors.toList());
            mConversationList.removeAll(sortedList);
            mConversationList.addAll(0, sortedList);
        }
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
    }

    @Override
    public void onArchive(Conversation conversation) {
        mConversationList.remove(conversation);
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));

        mViewModel.changeConversationStatusType(conversation, ConversationStatusType.ARCHIVED);
    }

    @Override
    public void onDelete(Conversation conversation) {

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

    @Override
    public void onNewConversationReceive(Conversation conversation) {
        mViewModel.submitConversation(conversation);
        isNewConversationAdded = true;
    }

    @Override
    public void onDeleteConversation(String unFriendUserId) {
        User user = Objects.requireNonNull(LoggedInUserManager.getInstance().getLoggedInUser());
        mViewModel.submitRemoveConversation(user.getUid(), unFriendUserId);
    }

    @Override
    public void onConversationNewMessage(String newMessageId, Conversation conversation) {
        List<String> chatIdList = conversation.getChats()
                                              .stream()
                                              .map(Chat::getId)
                                              .collect(Collectors.toList());

        if (!chatIdList.contains(newMessageId)) {
            // Reload conversation from remote
            mViewModel.fetchChatRemoteById(newMessageId);
        } else {
            onConversationUpdate(conversation);
        }
    }

    private void removeConversationAdapterUI(Conversation conversation) {
        mConversationList.remove(conversation);
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
    }
}
