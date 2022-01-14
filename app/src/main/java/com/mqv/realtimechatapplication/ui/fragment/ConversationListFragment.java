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
import com.mqv.realtimechatapplication.activity.br.ConversationBroadcastReceiver;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.Chat;
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
import java.util.stream.Collectors;

public class ConversationListFragment extends BaseSwipeFragment<ConversationFragmentViewModel, FragmentConversationBinding>
        implements ConversationDialogFragment.ConversationOptionListener,
                   ConversationBroadcastReceiver.OnConversationListener {
    private List<Conversation> mConversationList = new ArrayList<>();

    private ConversationListAdapter mConversationAdapter;
    private RankUserConversationAdapter mRankUserAdapter;

    private boolean isNewConversationAdded = false;

    private static final String FILE_MESSAGE_RINGTONE = "message_receive.mp3";
    private static final String ACTION_NEW_CONVERSATION = "com.mqv.tac.NEW_CONVERSATION";

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

        // Load more conversation when the user is current this stage. And got accepted friend request
        BroadcastReceiver newConversationReceiver = new ConversationBroadcastReceiver(this);

        requireContext().registerReceiver(newConversationReceiver, new IntentFilter(ACTION_NEW_CONVERSATION));
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

        mViewModel.getRefreshConversationResult().observe(this, result -> {
            if (result == null) return;

            mBinding.swipeMessages.setRefreshing(result.getStatus() == NetworkStatus.LOADING);

            if (result.getStatus() == NetworkStatus.ERROR) {
                Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
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

        mViewModel.getConversationListObserver().observe(this, updatedList -> {
            if (updatedList == null) return;

            mConversationList.removeAll(updatedList);
            mConversationList.addAll(updatedList);
            mConversationAdapter.submitList(new ArrayList<>(mConversationList));
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
                    boolean isNewChatAdded = intent.getBooleanExtra(ConversationActivity.EXTRA_NEW_CHAT_ADDED, false);
                    boolean isSeenChat = intent.getBooleanExtra(ConversationActivity.EXTRA_SEEN_CHAT, false);

//                    mViewModel.fetchCachedConversation(updated);
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
        // test
        mViewModel.fetchChatRemoteById(newMessageId);

//        List<String> chatIdList = conversation.getChats()
//                                              .stream()
//                                              .map(Chat::getId)
//                                              .collect(Collectors.toList());
//
//        if (!chatIdList.contains(newMessageId)) {
//            // Reload conversation from remote
//            mViewModel.fetchChatRemoteById(newMessageId);
//        } else {
//            onConversationUpdate(conversation);
//        }
    }

    @Override
    public void onUnarchiveConversation(Conversation conversation) {
        mConversationList.add(conversation);
        List<Conversation> sortedList = sortConversationList(mConversationList);

        mConversationList.clear();
        mConversationList.addAll(sortedList);
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
    }

    /*
    * Sort new list with the last chat recent first
    * */
    private List<Conversation> sortConversationList(List<Conversation> oldList) {
        if (oldList == null)
            return new ArrayList<>();

        return oldList.stream()
                      .sorted((o1, o2) -> o2.getLastChat()
                              .getTimestamp()
                              .compareTo(o1.getLastChat().getTimestamp()))
                      .collect(Collectors.toList());
    }

    private void removeConversationAdapterUI(Conversation conversation) {
        mConversationList.remove(conversation);
        mConversationAdapter.submitList(new ArrayList<>(mConversationList));
    }
}
