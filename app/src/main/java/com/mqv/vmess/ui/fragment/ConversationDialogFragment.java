package com.mqv.vmess.ui.fragment;

import static com.mqv.vmess.network.model.type.ConversationStatusType.ARCHIVED;
import static com.mqv.vmess.network.model.type.ConversationStatusType.INBOX;

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
import com.mqv.vmess.R;
import com.mqv.vmess.databinding.FragmentConversationBottomDialogBinding;
import com.mqv.vmess.databinding.ItemPreferenceContentBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.util.MessageUtil;

public class ConversationDialogFragment extends BottomSheetDialogFragment {
    private FragmentConversationBottomDialogBinding mBinding;
    private Conversation mConversation;
    private User mUser;
    private boolean isTurnOffNotification;
    private boolean isUnread;

    private final ConversationOptionListener mListener;
    private static final String EXTRA_CONVERSATION = "extra_conversation";
    private static final String EXTRA_NOTIFICATION = "extra_notification";

    public interface ConversationOptionListener {
        default void onUnArchive(Conversation conversation) {
        }

        default void onArchive(Conversation conversation) {
        }

        void onDelete(Conversation conversation);

        void onMuteNotification(Conversation conversation);

        void onUnMuteNotification(Conversation conversation);

        void onCreateGroup(Conversation conversation, User whoCreateWith);

        void onLeaveGroup(Conversation conversation);

        void onAddMember(Conversation conversation);

        void onMarkUnread(Conversation conversation);

        void onMarkRead(Conversation conversation);

        void onIgnore(Conversation conversation);
    }

    public ConversationDialogFragment(ConversationOptionListener listener) {
        mListener = listener;
    }

    public static ConversationDialogFragment newInstance(ConversationOptionListener listener, Conversation conversation, boolean isTurnOffNotification) {
        ConversationDialogFragment dialog = new ConversationDialogFragment(listener);

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONVERSATION, conversation);
        args.putBoolean(EXTRA_NOTIFICATION, isTurnOffNotification);

        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args !=  null) {
            mConversation = args.getParcelable(EXTRA_CONVERSATION);
            isTurnOffNotification = args.getBoolean(EXTRA_NOTIFICATION, false);
        } else {
            throw new IllegalArgumentException("The conversation must not be null");
        }

        if (LoggedInUserManager.getInstance().getLoggedInUser() == null) {
            throw new IllegalArgumentException("The user must not be null");
        } else {
            mUser = LoggedInUserManager.getInstance().getLoggedInUser();
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

        if (mConversation.getStatus() == ARCHIVED) {
            bindItem(mBinding.itemArchive, R.string.label_conversation_unarchive, R.drawable.ic_round_unarchive);
        } else if (mConversation.getStatus() == INBOX) {
            bindItem(mBinding.itemArchive, R.string.label_conversation_archive, R.drawable.ic_round_archive);
        } else {
            mBinding.itemArchive.getRoot().setVisibility(View.GONE);
        }
        bindItem(mBinding.itemDelete, R.string.label_conversation_delete, R.drawable.ic_round_delete);
        if (isTurnOffNotification) {
            bindItem(mBinding.itemMuteNotifications, R.string.label_conversation_unmute_notifications, R.drawable.ic_round_notifications);
        } else {
            bindItem(mBinding.itemMuteNotifications, R.string.label_conversation_mute_notifications, R.drawable.ic_round_notifications_off);
        }
        Chat lastChat = mConversation.getLastChat();

        if (!MessageUtil.isDummyFirstMessagePair(lastChat) && !lastChat.getSenderId().equals(mUser.getUid())) {
            mBinding.itemMarkUnread.getRoot().setVisibility(View.VISIBLE);

            if (lastChat.getSeenBy().contains(mUser.getUid())) {
                isUnread = true;

                bindItem(mBinding.itemMarkUnread, R.string.label_conversation_mark_unread, R.drawable.ic_round_mark_email_unread);
            } else {
                isUnread = false;

                bindItem(mBinding.itemMarkUnread, R.string.label_conversation_mark_read, R.drawable.ic_round_mark_email_read);
            }
        } else {
            mBinding.itemMarkUnread.getRoot().setVisibility(View.GONE);
        }

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
        bindItem(mBinding.itemCreateGroup, requireContext().getString(R.string.label_conversation_create_group, getAnotherUser().getDisplayName()), R.drawable.ic_round_groups);
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
            if (mConversation.getStatus() == ARCHIVED) {
                mListener.onUnArchive(mConversation);
            } else {
                mListener.onArchive(mConversation);
            }
            dismiss();
        });
        mBinding.itemDelete.getRoot().setOnClickListener(v -> {
            mListener.onDelete(mConversation);
            dismiss();
        });
        mBinding.itemMuteNotifications.getRoot().setOnClickListener(v -> {
            if (isTurnOffNotification) {
                mListener.onUnMuteNotification(mConversation);
            } else {
                mListener.onMuteNotification(mConversation);
            }
            dismiss();
        });
        mBinding.itemCreateGroup.getRoot().setOnClickListener(v -> {
            mListener.onCreateGroup(mConversation, getAnotherUser());
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
            if (isUnread) {
                mListener.onMarkUnread(mConversation);
            } else {
                mListener.onMarkRead(mConversation);
            }
            dismiss();
        });
        mBinding.itemIgnore.getRoot().setOnClickListener(v -> {
            mListener.onIgnore(mConversation);
            dismiss();
        });
    }

    private User getAnotherUser() {
        return mConversation.getParticipants()
                            .stream()
                            .filter(u -> !u.getUid().equals(mUser.getUid()))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new);
        }
}
