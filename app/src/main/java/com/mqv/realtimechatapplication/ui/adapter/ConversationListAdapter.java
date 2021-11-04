package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemConversationBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.util.Const;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConversationListAdapter extends ListAdapter<Conversation, ConversationListAdapter.ConversationViewHolder> {
    private final List<Conversation> mConversations;
    private final Context mContext;
    private final FirebaseUser mCurrentUser;
    private Consumer<Integer> conversationConsumer;

    public ConversationListAdapter(List<Conversation> data, Context context) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                // TODO: complete later
                return false;
            }
        });
        mConversations = data;
        mContext = context;
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void registerOnConversationClick(Consumer<Integer> conversationConsumer) {
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

        if (!payloads.isEmpty() && payloads.get(0).equals("last_chat")) {
            holder.bindRecentChat(getItem(position));
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

        private static final String TOO_LONG_TEXT_SUFFIX = "...";
        private static final String WEEK_PATTERN = "EEE hh:mm a";
        private static final String MONTH_PATTERN = "MMM dd hh:mm a";

        private static final int MAX_CONTENT_LENGTH = 30;
        private static final int MAX_TITLE_LENGTH = 30;

        public ConversationViewHolder(@NonNull View itemView,
                                      Context context,
                                      FirebaseUser user,
                                      Consumer<Integer> conversationConsumer) {
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
                    conversationConsumer.accept(getAdapterPosition());
            });
        }

        public void bindRecentChat(Conversation item) {
            showRecentChat(item.getLastChat());
            bindConversationStatus(item.getLastChat(), item.getParticipants(), item.getType());
        }

        public void bind(Conversation item) {
            List<Chat> sortedChat = item.getChats()
                    .stream()
                    .sorted(Comparator.comparing(Chat::getTimestamp))
                    .collect(Collectors.toList());

            Chat recentChat = sortedChat.get(sortedChat.size() - 1);

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

            if (textContent.length() >= MAX_CONTENT_LENGTH) {
                int suffixSize = TOO_LONG_TEXT_SUFFIX.length();

                textContent = new StringBuilder(textContent
                        .substring(0, MAX_CONTENT_LENGTH - (suffixSize + 1))
                        .concat(TOO_LONG_TEXT_SUFFIX));
            }

            mBinding.textContentConversation.setText(textContent);
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
            String formattedUrl = url == null ? null : url.replace("localhost", Const.BASE_IP);

            GlideApp.with(mContext)
                    .load(formattedUrl)
                    .error(ContextCompat.getDrawable(mContext, R.drawable.ic_account_undefined))
                    .fallback(ContextCompat.getDrawable(mContext, R.drawable.ic_round_account))
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(container);
        }

        private String getReadableTime(LocalDateTime from) {
            var now = LocalDateTime.now();

            var day = ChronoUnit.DAYS.between(from, now);

            if (day < 1) {
                var arr = from.format(DateTimeFormatter.ofPattern(WEEK_PATTERN)).split(" ");

                return arr[1] + " " + arr[2];
            }

            if (day == 1) {
                var arr = from.format(DateTimeFormatter.ofPattern(WEEK_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_yesterday, arr[1] + " " + arr[2]);
            }

            if (day <= 7) {
                var arr = from.format(DateTimeFormatter.ofPattern(WEEK_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_week, arr[0], arr[1] + " " + arr[2]);
            } else {
                var arr = from.format(DateTimeFormatter.ofPattern(MONTH_PATTERN)).split(" ");

                return mContext.getString(R.string.msg_notification_month, arr[0], arr[1], arr[2] + " " + arr[3]);
            }
        }
    }
}
