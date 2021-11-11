package com.mqv.realtimechatapplication.activity.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ConversationActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListArchivedViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceArchivedChatBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter;
import com.mqv.realtimechatapplication.ui.fragment.ConversationDialogFragment;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceArchivedChatActivity extends ToolbarActivity<ConversationListArchivedViewModel, ActivityPreferenceArchivedChatBinding>
        implements ConversationDialogFragment.ConversationOptionListener {
    private final List<Conversation> mConversations = new ArrayList<>();
    private ConversationListAdapter mAdapter;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceArchivedChatBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConversationListArchivedViewModel> getViewModelClass() {
        return ConversationListArchivedViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_archived_chats);

        setupRecyclerView();
    }

    @Override
    public void setupObserver() {
        mViewModel.getArchivedChatResult().observe(this, result -> {
            if (result == null) return;

            mBinding.progressBarLoading.setVisibility(result.getStatus() == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);

            switch (result.getStatus()) {
                case SUCCESS:
                    mConversations.clear();
                    mConversations.addAll(result.getSuccess());
                    mAdapter.submitList(new ArrayList<>(mConversations));
                    break;
                case ERROR:
                    break;
            }
        });
    }

    private void setupRecyclerView() {
        mAdapter = new ConversationListAdapter(mConversations, this);
        mAdapter.registerOnConversationClick(onConversationClick());

        mBinding.recyclerViewArchivedChats.setAdapter(mAdapter);
        mBinding.recyclerViewArchivedChats.setHasFixedSize(true);
        mBinding.recyclerViewArchivedChats.setLayoutManager(new LinearLayoutManager(this));
    }

    private BiConsumer<Integer, Boolean> onConversationClick() {
        return (pos, isLongClick) -> {
            Conversation conversation = mAdapter.getCurrentList().get(pos);

            if (isLongClick) {
                ConversationDialogFragment dialog = ConversationDialogFragment.newInstance(this, conversation);
                dialog.show(getSupportFragmentManager(), null);
            } else {
                Intent conversationIntent = new Intent(this, ConversationActivity.class);
                conversationIntent.putExtra("conversation", conversation);

                activityResultLauncher.launch(conversationIntent, result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();

                        if (intent != null) {
                            // Change the conversation status to INBOX if updated
                            Conversation updated = intent.getParcelableExtra("conversation");

//                            mViewModel.fetchCachedConversation(updated);
                        }
                    }
                });
            }
        };
    }

    @Override
    public void onUnArchive(Conversation conversation) {
        mViewModel.changeConversationStatusType(conversation, ConversationStatusType.INBOX);
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
    public void onLeaveGroup(Conversation conversation) {

    }

    @Override
    public void onAddMember(Conversation conversation) {

    }

    @Override
    public void onMarkUnread(Conversation conversation) {

    }

    @Override
    public void onIgnore(Conversation conversation) {

    }
}