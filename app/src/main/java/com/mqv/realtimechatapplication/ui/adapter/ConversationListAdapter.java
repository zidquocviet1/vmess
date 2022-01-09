package com.mqv.realtimechatapplication.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemConversationBinding;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.ConversationGroup;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Picture;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ConversationListAdapter extends ListAdapter<Conversation, ConversationListAdapter.ConversationViewHolder> {
    private final List<Conversation> mConversations;
    private final Context mContext;
    private final FirebaseUser mCurrentUser;
    private BiConsumer<Integer, Boolean> conversationConsumer;

    public static final String LAST_CHAT_PAYLOAD = "last_chat";
    public static final String LAST_CHAT_STATUS_PAYLOAD = "last_chat_status";
    public static final String LAST_CHAT_UNSENT_PAYLOAD = "last_chat_unsent";
    public static final String NAME_PAYLOAD = "name";
    public static final String PRESENCE_PAYLOAD = "presence";
    public static final String THUMBNAIL_PAYLOAD = "avatar";

    public ConversationListAdapter(List<Conversation> data, Context context) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                Chat oldLastChat = oldItem.getLastChat();
                Chat newLastChat = newItem.getLastChat();

                boolean isLastChatEquals = oldLastChat.getId().equals(newLastChat.getId()) &&
                        oldLastChat.isUnsent().equals(newLastChat.isUnsent()) &&
                        oldLastChat.getSeenBy().equals(newLastChat.getSeenBy()) &&
                        oldLastChat.getStatus() == newLastChat.getStatus() &&
                        oldLastChat.getType() == newLastChat.getType();

                return oldItem.getId().equals(newItem.getId()) &&
                       oldItem.getParticipants().equals(newItem.getParticipants()) &&
                       oldItem.getType() == newItem.getType() &&
                       ((oldItem.getGroup() == null && newItem.getGroup() == null) ||
                               (oldItem.getGroup() != null && newItem.getGroup() != null &&
                                       oldItem.getGroup().equals(newItem.getGroup()))) &&
                       oldItem.getStatus() == newItem.getStatus() &&
                       oldItem.getCreationTime().equals(newItem.getCreationTime()) &&
                       isLastChatEquals;
            }

            @Override
            public Object getChangePayload(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                Bundle bundle = new Bundle();

                if (oldItem.getType() == ConversationType.GROUP) {
                    ConversationGroup oldGroup = oldItem.getGroup();
                    ConversationGroup newGroup = newItem.getGroup();

                    String oldGroupName = oldGroup.getName();
                    String newGroupName = newGroup.getName();

                    String oldGroupThumbnail = oldGroup.getThumbnail();
                    String newGroupThumbnail = newGroup.getThumbnail();

                    bundle.putBoolean(NAME_PAYLOAD, !oldGroupName.equals(newGroupName));
                    bundle.putBoolean(THUMBNAIL_PAYLOAD, !Objects.equals(oldGroupThumbnail, newGroupThumbnail));
                }

                Chat oldRecentChat = oldItem.getLastChat();
                Chat newRecentChat = newItem.getLastChat();

                bundle.putBoolean(LAST_CHAT_PAYLOAD, !Objects.equals(oldRecentChat, newRecentChat));
                bundle.putBoolean(LAST_CHAT_STATUS_PAYLOAD, !Objects.equals(oldRecentChat.getStatus(), newRecentChat.getStatus()));
                bundle.putBoolean(LAST_CHAT_UNSENT_PAYLOAD, oldRecentChat.isUnsent() != newRecentChat.isUnsent());

                return bundle;
            }
        });
        mConversations = data;
        mContext = context;
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void registerOnConversationClick(BiConsumer<Integer, Boolean> conversationConsumer) {
        this.conversationConsumer = conversationConsumer;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_conversation, parent, false);

        return new ConversationViewHolder(view, mContext, mCurrentUser, conversationConsumer);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        if (!payloads.isEmpty() && payloads.get(0).equals(LAST_CHAT_PAYLOAD)) {
            holder.bindRecentChat(mConversations.get(position));
        } else if (!payloads.isEmpty() && payloads.get(0) instanceof Bundle) {
            Bundle bundle = (Bundle) payloads.get(0);

            if (bundle.getBoolean(NAME_PAYLOAD, false)) {
                holder.bindConversationName(getItem(position));
            } else if (bundle.getBoolean(THUMBNAIL_PAYLOAD, false)) {
                holder.bindConversationThumbnail(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_PAYLOAD, false)) {
                holder.bindRecentChat(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_STATUS_PAYLOAD, false)) {
                holder.bindRecentChatStatus(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_UNSENT_PAYLOAD, false)) {
                holder.bindUnsentMessage(getItem(position));
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding mBinding;
        private final Context mContext;
        private final FirebaseUser mCurrentUser;
        private final ColorStateList mIconColor;
        private final ColorStateList mIconErrorColor;
        private final int mTextUnreadColor;
        private final int mDefaultTextViewColor;

        private static final String DAY_PATTERN = "hh:mm a";
        private static final String WEEK_PATTERN = "EEE";
        private static final String MONTH_PATTERN = "MMM dd";
        private static final String YEAR_PATTERN = "MMM dd, yyyy";

        private static final int MONTH_INTERVAL = 6;
        private static final int WEEK_INTERVAL = 1;
        private static final int DAY_INTERVAL = 1;

        public ConversationViewHolder(@NonNull View itemView,
                                      Context context,
                                      FirebaseUser user,
                                      BiConsumer<Integer, Boolean> conversationConsumer) {
            super(itemView);
            mBinding = ItemConversationBinding.bind(itemView);
            mContext = context;
            mCurrentUser = user;
            mIconColor = ContextCompat.getColorStateList(mContext, R.color.ic_background_tint);
            mIconErrorColor = ContextCompat.getColorStateList(mContext, android.R.color.holo_red_light);
            mTextUnreadColor = ContextCompat.getColor(mContext, R.color.black);
            mDefaultTextViewColor = mBinding.textTitleConversation.getCurrentTextColor();

            itemView.setOnClickListener(v -> {
                if (conversationConsumer != null)
                    conversationConsumer.accept(getBindingAdapterPosition(), false);
            });
            itemView.setOnLongClickListener(v -> {
                if (conversationConsumer != null) {
                    conversationConsumer.accept(getBindingAdapterPosition(), true);
                    return true;
                }
                return false;
            });
        }

        public void bindConversationName(Conversation item) {
            mBinding.textTitleConversation.setText(item.getGroup().getName());
        }

        public void bindConversationThumbnail(Conversation item) {
            loadImage(item.getGroup().getThumbnail(), mBinding.imageState);
        }

        public void bindUnsentMessage(Conversation item) {
            Chat recentChat = item.getLastChat();
            String textContent;

            if (recentChat.getSenderId().equals(mCurrentUser.getUid())) {
                textContent = mContext.getString(R.string.title_sender_chat_unsent);
            } else {
                User sender = item.getParticipants()
                                  .stream()
                                  .filter(u -> u.getUid().equals(recentChat.getSenderId()))
                                  .findFirst()
                                  .orElseThrow(IllegalStateException::new);
                textContent = mContext.getString(R.string.title_receiver_chat_unsent, sender.getDisplayName());
            }
            mBinding.textContentConversation.setText(textContent);
        }

        public void bindRecentChatStatus(Conversation item) {
            bindConversationStatus(item.getLastChat(), item.getParticipants(), item.getType());
        }

        public void bindRecentChat(Conversation item) {
            Chat recentChat = item.getLastChat();

            if (recentChat.getId().startsWith(Const.WELCOME_CHAT_PREFIX)) {
                bindWelcomeChat(recentChat);
            } else {
                bindConversationStatus(recentChat, item.getParticipants(), item.getType());
            }
            showRecentChat(recentChat);
        }

        public void bind(Conversation item) {
            Chat recentChat = item.getLastChat();

            bindConversationStatus(recentChat, item.getParticipants(), item.getType());

            switch (item.getType()) {
                case GROUP:
                    bindGroup();
                    break;
                case NORMAL:
                    bindNormal(item, recentChat);
                    break;
                case SELF:
                    bindSelf();
                    break;
            }
        }

        private void bindSelf() {

        }

        private void bindNormal(Conversation item, Chat recentChat) {
            item.getParticipants()
                    .stream()
                    .filter(u -> !u.getUid().equals(mCurrentUser.getUid()))
                    .findFirst()
                    .ifPresent(user -> {
                        loadImage(user.getPhotoUrl(), mBinding.imageConversation);

                        /*
                         * Check whether the conversation has any chat or not.
                         * */
                        showRecentChat(recentChat);

                        mBinding.textTitleConversation.setText(user.getDisplayName());
                        // Not complete
                        String timestamp = mContext.getString(R.string.title_conversation_timestamp, getReadableTime(recentChat.getTimestamp()));
                        mBinding.textCreatedAt.setText(timestamp);
                    });
        }

        private void bindGroup() {

        }

        private void showRecentChat(Chat recentChat) {
            StringBuilder textContent = new StringBuilder();

            if (recentChat.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) 
                return;

            if (recentChat.getId().startsWith(Const.WELCOME_CHAT_PREFIX)) {
                textContent.append(mContext.getString(R.string.dummy_first_chat));
            } else {
                if (recentChat.getSenderId().equals(mCurrentUser.getUid())) {
                    textContent.append(mContext.getString(R.string.msg_owner_chat));
                }
                textContent.append(recentChat.getContent());
            }
            mBinding.textContentConversation.setText(textContent);
        }

        private void bindWelcomeChat(Chat welcomeChat) {
            markAsUnread(!welcomeChat.getSeenBy().contains(mCurrentUser.getUid()));
            mBinding.imageState.setVisibility(View.GONE);
        }

        private void bindConversationStatus(Chat recentChat, List<User> participants, ConversationType type) {
            String userId = mCurrentUser.getUid();
            String chatSenderId = recentChat.getSenderId();

            switch (type) {
                case NORMAL:
                    /*
                     * If the recently chat belong to another user. Then make the status icon is invisible.
                     * Chat's sender id == null. That's mean the chat is dummy first chat.
                     * */
                    List<String> seenUserId = recentChat.getSeenBy();

                    if (chatSenderId == null || !chatSenderId.equals(userId)) {
                        mBinding.imageState.setVisibility(View.GONE);

                        markAsUnread(seenUserId.isEmpty() || !seenUserId.contains(userId));
                    } else {
                        if (!seenUserId.isEmpty()) {
                            seenUserId.stream()
                                    .findFirst()
                                    .flatMap(id -> participants.stream()
                                            .filter(u -> u.getUid().equals(id))
                                            .findFirst())
                                    .ifPresent(user -> loadImage(user.getPhotoUrl(), mBinding.imageState));
                            return;
                        }
                        switch (recentChat.getStatus()) {
                            case ERROR:
                                setIconStatus(R.drawable.ic_round_error, mIconErrorColor);
                                break;
                            case SENDING:
                                setIconStatus(R.drawable.ic_outline_circle, mIconColor);
                                break;
                            case RECEIVED:
                                setIconStatus(R.drawable.ic_round_check_circle, mIconColor);
                                break;
                            case NOT_RECEIVED:
                                setIconStatus(R.drawable.ic_round_check_circle_outline, mIconColor);
                                break;
                        }
                    }
                    break;
                case GROUP:
                    break;
                case SELF:
                    mBinding.imageState.setVisibility(View.GONE);
                    break;
            }
        }

        private void setIconStatus(@DrawableRes int iconId, ColorStateList iconTint) {
            Drawable drawable = ContextCompat.getDrawable(mContext, iconId);

            mBinding.imageState.setBackground(drawable);
            mBinding.imageState.setBackgroundTintList(iconTint);
        }

        private void markAsUnread(boolean isUnread) {
            if (isUnread) {
                mBinding.textTitleConversation.setTypeface(Typeface.DEFAULT_BOLD);
                mBinding.textContentConversation.setTypeface(Typeface.DEFAULT_BOLD);
                mBinding.textCreatedAt.setTypeface(Typeface.DEFAULT_BOLD);

                mBinding.textTitleConversation.setTextColor(mTextUnreadColor);
                mBinding.textContentConversation.setTextColor(mTextUnreadColor);
                mBinding.textCreatedAt.setTextColor(mTextUnreadColor);
            } else {
                mBinding.textTitleConversation.setTypeface(Typeface.DEFAULT);
                mBinding.textContentConversation.setTypeface(Typeface.DEFAULT);
                mBinding.textCreatedAt.setTypeface(Typeface.DEFAULT);

                mBinding.textTitleConversation.setTextColor(mDefaultTextViewColor);
                mBinding.textContentConversation.setTextColor(mDefaultTextViewColor);
                mBinding.textCreatedAt.setTextColor(mDefaultTextViewColor);
            }
        }

        private void loadImage(@Nullable String url, ImageView container) {
            Picture.loadUserAvatar(mContext, url).into(container);
        }

        private String getReadableTime(LocalDateTime from) {
            var now = LocalDateTime.now();

            var day = ChronoUnit.DAYS.between(from, now);
            var week = ChronoUnit.WEEKS.between(from, now);
            var month = ChronoUnit.MONTHS.between(from, now);
            var defaultLocale = Locale.getDefault();

            if (month >= MONTH_INTERVAL) return from.format(DateTimeFormatter.ofPattern(YEAR_PATTERN, defaultLocale));
            if (week >= WEEK_INTERVAL) return from.format(DateTimeFormatter.ofPattern(MONTH_PATTERN, defaultLocale));
            if (day < DAY_INTERVAL) return from.format(DateTimeFormatter.ofPattern(DAY_PATTERN, defaultLocale));
            return from.format(DateTimeFormatter.ofPattern(WEEK_PATTERN, defaultLocale));
        }
    }
}
