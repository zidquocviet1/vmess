package com.mqv.realtimechatapplication.ui.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.FragmentConversationBottomDialogBinding;
import com.mqv.realtimechatapplication.databinding.ItemPreferenceContentBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;

public class ConversationDialogFragment extends BottomSheetDialogFragment {
    private FragmentConversationBottomDialogBinding mBinding;
    private Conversation mConversation;
    private ConversationOptionListener mListener;
    private static final String EXTRA_CONVERSATION = "extra_conversation";

    public interface ConversationOptionListener {
        void onArchive(Conversation conversation);

        void onDelete(Conversation conversation);

        void onMuteNotification(Conversation conversation);

        void onCreateGroup(Conversation conversation);

        void onLeaveGroup(Conversation conversation);

        void onAddMember(Conversation conversation);

        void onMarkUnread(Conversation conversation);

        void onIgnore(Conversation conversation);
    }

    public ConversationDialogFragment(ConversationOptionListener listener) {
        mListener = listener;
    }

    public static ConversationDialogFragment newInstance(ConversationOptionListener listener, Conversation conversation) {
        ConversationDialogFragment dialog = new ConversationDialogFragment(listener);

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONVERSATION, conversation);

        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args !=  null) {
            mConversation = args.getParcelable(EXTRA_CONVERSATION);
        } else {
            throw new IllegalArgumentException("The conversation must not be null");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentConversationBottomDialogBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mConversation.getType() == ConversationType.GROUP) {
            bindGroup();
        } else {
            bindNormal();
        }

        bindItem(mBinding.itemArchive, R.string.label_conversation_archive, R.drawable.ic_round_archive);
        bindItem(mBinding.itemDelete, R.string.label_conversation_delete, R.drawable.ic_round_delete);
        bindItem(mBinding.itemMuteNotifications, R.string.label_conversation_mute_notifications, R.drawable.ic_round_notifications_off);
        bindItem(mBinding.itemMarkUnread, R.string.label_conversation_mark_unread, R.drawable.ic_round_mark_email_unread);

        registerItemClickEvent();
    }

    @Override
    public int getTheme() {
        return R.style.UserSocialLinkListDialogFragment;
    }

    private void bindGroup() {
        bindItem(mBinding.itemIgnore, R.string.label_conversation_ignore_group, R.drawable.ic_round_disabled_visible);
        bindItem(mBinding.itemAddMembers, R.string.label_conversation_add_members, R.drawable.ic_round_group_add_member);
        bindItem(mBinding.itemLeaveGroup, R.string.label_conversation_leave_group, R.drawable.ic_round_logout);

        mBinding.itemLeaveGroup.getRoot().setVisibility(View.VISIBLE);
        mBinding.itemAddMembers.getRoot().setVisibility(View.VISIBLE);
        mBinding.itemCreateGroup.getRoot().setVisibility(View.GONE);
    }

    private void bindNormal() {
        bindItem(mBinding.itemIgnore, R.string.label_conversation_ignore_messages, R.drawable.ic_round_disabled_visible);
        bindItem(mBinding.itemCreateGroup, requireContext().getString(R.string.label_conversation_create_group, "Admin"), R.drawable.ic_round_groups);
        mBinding.itemLeaveGroup.getRoot().setVisibility(View.GONE);
        mBinding.itemAddMembers.getRoot().setVisibility(View.GONE);
        mBinding.itemCreateGroup.getRoot().setVisibility(View.VISIBLE);
    }

    private void bindItem(ItemPreferenceContentBinding item, String title, @DrawableRes int icon) {
        item.title.setText(title);
        item.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), icon));
        item.title.setTypeface(Typeface.DEFAULT_BOLD);
        item.summary.setVisibility(View.GONE);
    }

    private void bindItem(ItemPreferenceContentBinding item, @StringRes int title, @DrawableRes int icon) {
        item.title.setText(title);
        item.icon.setImageDrawable(ContextCompat.getDrawable(requireContext(), icon));
        item.title.setTypeface(Typeface.DEFAULT_BOLD);
        item.summary.setVisibility(View.GONE);
    }

    private void registerItemClickEvent() {
        mBinding.itemArchive.getRoot().setOnClickListener(v -> {
            mListener.onArchive(mConversation);
            dismiss();
        });
        mBinding.itemDelete.getRoot().setOnClickListener(v -> {
            mListener.onDelete(mConversation);
            dismiss();
        });
        mBinding.itemMuteNotifications.getRoot().setOnClickListener(v -> {
            mListener.onMuteNotification(mConversation);
            dismiss();
        });
        mBinding.itemCreateGroup.getRoot().setOnClickListener(v -> {
            mListener.onCreateGroup(mConversation);
            dismiss();
        });
        mBinding.itemLeaveGroup.getRoot().setOnClickListener(v -> {
            mListener.onLeaveGroup(mConversation);
            dismiss();
        });
        mBinding.itemAddMembers.getRoot().setOnClickListener(v -> {
            mListener.onAddMember(mConversation);
            dismiss();
        });
        mBinding.itemMarkUnread.getRoot().setOnClickListener(v -> {
            mListener.onMarkUnread(mConversation);
            dismiss();
        });
        mBinding.itemIgnore.getRoot().setOnClickListener(v -> {
            mListener.onIgnore(mConversation);
            dismiss();
        });
    }

    private void notifyItemClicked(String action) {
        Bundle data = new Bundle();
        data.putString("key_action", action);
        requireActivity().getSupportFragmentManager().setFragmentResult("key_option", data);
        dismiss();
    }
}
