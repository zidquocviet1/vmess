package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemChatBinding;
import com.mqv.realtimechatapplication.databinding.ItemChatProfileBinding;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Picture;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatListAdapter extends ListAdapter<Chat, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final List<Chat> mChatList;
    private final List<User> mParticipants;
    private final ColorStateList mChatColorStateList;
    private final User mCurrentUser;
    @Nullable
    private final User mOtherUser;
    private User mOtherUserDetail;

    private static final int VIEW_PROFILE = -1;
    private static final int VIEW_PROFILE_SELF = 0;
    private static final int VIEW_CHAT = 1;
    private static final int VIEW_LOAD_MORE = 2;
    public static final String MESSAGE_STATUS_PAYLOAD = "message_status";
    public static final String PROFILE_USER_PAYLOAD = "profile_user";
    public static final String ICON_RECEIVER_PAYLOAD = "icon_receiver";
    public static final String TIMESTAMP_MESSAGE_PAYLOAD = "timestamp";
    public static final String MESSAGE_UNSENT_PAYLOAD = "message_unsent";

    public ChatListAdapter(Context context,
                           List<Chat> chatList,
                           List<User> participants,
                           ColorStateList chatColorStateList,
                           @NonNull User user,
                           @Nullable User otherUser) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
                return oldItem.getId().equals(newItem.getId()) &&
                        Objects.equals(oldItem.getSenderId(), newItem.getSenderId()) &&
                        oldItem.getTimestamp().equals(newItem.getTimestamp()) &&
                        oldItem.getSeenBy().equals(newItem.getSeenBy()) &&
                        Objects.equals(oldItem.getStatus(), newItem.getStatus())&&
                        oldItem.getConversationId().equals(newItem.getConversationId()) &&
                        oldItem.isUnsent().equals(newItem.isUnsent());
            }
        });
        mContext = context;
        mChatColorStateList = chatColorStateList;
        mCurrentUser = user;
        mOtherUser = otherUser;
        mChatList = chatList;
        mParticipants = participants;
    }

    public void addChat(Chat chat) {
        mChatList.add(chat);

        notifyItemInserted(mChatList.size() - 1);

        if (mChatList.size() - 2 >= 0)
            notifyItemChanged(mChatList.size() - 2);
    }

    public void changeChatStatus(Chat chat) {
        notifyItemChanged(mChatList.indexOf(chat), MESSAGE_STATUS_PAYLOAD);
    }

    public void changeChatStatus(int position) {
        notifyItemChanged(position, MESSAGE_STATUS_PAYLOAD);
    }

    public void changeChatUnsentStatus(int position) {
        notifyItemChanged(position, MESSAGE_UNSENT_PAYLOAD);
    }

    public void setUserDetail(User user) {
        mOtherUserDetail = user;
    }

    @Override
    public int getItemViewType(int position) {
        var item = getItem(position);

        if (item == null)
            return VIEW_LOAD_MORE;

        if (item.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) {
            if (mCurrentUser.getUid().equals(mOtherUser.getUid())) {
                return VIEW_PROFILE_SELF;
            }
            return VIEW_PROFILE;
        } else {
            return VIEW_CHAT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(mContext);

        switch (type) {
            case VIEW_PROFILE:
            case VIEW_PROFILE_SELF:
                view = inflater.inflate(R.layout.item_chat_profile, parent, false);
                return new ProfileViewHolder(ItemChatProfileBinding.bind(view), mContext, type == VIEW_PROFILE_SELF);
            default:
                view = inflater.inflate(R.layout.item_chat, parent, false);
                return new ChatListViewHolder(ItemChatBinding.bind(view),
                        mContext,
                        mCurrentUser,
                        mOtherUser,
                        mParticipants,
                        mChatColorStateList,
                        mChatList);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        if (holder instanceof ChatListViewHolder) {
            var chatHolder = (ChatListViewHolder) holder;

            if (!payloads.isEmpty() && payloads.get(0).equals(MESSAGE_STATUS_PAYLOAD)) {
                chatHolder.bindSenderMessageStatus(getItem(position));
            } else if (!payloads.isEmpty() && payloads.get(0).equals(ICON_RECEIVER_PAYLOAD)) {
                chatHolder.hiddenIconReceiver();
            } else if (!payloads.isEmpty() && payloads.get(0).equals(TIMESTAMP_MESSAGE_PAYLOAD)) {
                chatHolder.shouldShowTimestamp(getItem(position - 1), getItem(position));
            } else if (!payloads.isEmpty() && payloads.get(0).equals(MESSAGE_UNSENT_PAYLOAD)) {
                chatHolder.bindUnsentStatus(getItem(position));
            }
        } else if (holder instanceof ProfileViewHolder) {
            if (!payloads.isEmpty() && payloads.get(0).equals(PROFILE_USER_PAYLOAD)) {
                var profileHolder = (ProfileViewHolder) holder;

                profileHolder.bindTo(mOtherUserDetail);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        var preItem = position - 1 >= 0 ? getItem(position - 1) : null;
        var nextItem = position + 1 < mChatList.size() ? getItem(position + 1) : null;

        if (holder instanceof ChatListViewHolder) {
            var chatHolder = (ChatListViewHolder) holder;

            if (getItemViewType(position) == VIEW_LOAD_MORE) {
                chatHolder.showLoading();
            } else {
                chatHolder.bindTo(preItem, getItem(position), nextItem);
            }
        } else if (holder instanceof ProfileViewHolder) {
            ProfileViewHolder profileHolder = (ProfileViewHolder) holder;

            if (getItemViewType(position) == VIEW_PROFILE_SELF){
                profileHolder.bindTo(mCurrentUser);
            } else {
                profileHolder.bindTo(mOtherUserDetail);
            }
        }
    }

    public static void initializePool(RecyclerView.RecycledViewPool pool) {
        pool.setMaxRecycledViews(VIEW_CHAT, 25);
        pool.setMaxRecycledViews(VIEW_LOAD_MORE, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE_SELF, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE, 1);
    }

    public static class ChatListViewHolder extends RecyclerView.ViewHolder {
        final ItemChatBinding mBinding;
        final User mUser;
        @Nullable
        final User mOtherUser;
        final List<User> mParticipants;
        final ColorStateList mBackgroundChatColor;
        final ColorStateList mErrorChatColor;
        final Context mContext;
        final List<Chat> mListItem;
        Drawable mReceivedIconDrawable;
        Drawable mNotReceivedIconDrawable;
        Drawable mSendingIconDrawable;
        Drawable mErrorIconDrawable;

        private static final String WEEK_PATTERN = "EEE hh:mm a";
        private static final String MONTH_PATTERN = "MMM dd hh:mm a";

        public ChatListViewHolder(@NonNull ItemChatBinding binding,
                                  @NonNull Context context,
                                  @NonNull User user,
                                  @Nullable User otherUser,
                                  @NonNull List<User> participants,
                                  @NonNull ColorStateList colorStateList,
                                  List<Chat> listItem) {
            super(binding.getRoot());

            mBinding = binding;
            mContext = context;
            mUser = user;
            mOtherUser = otherUser;
            mParticipants = participants;
            mBackgroundChatColor = colorStateList;
            mErrorChatColor = ColorStateList.valueOf(ContextCompat.getColor(mContext, android.R.color.holo_red_light));
            mListItem = listItem;

            mReceivedIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle);
            mNotReceivedIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle_outline);
            mSendingIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_outline_circle);
            mErrorIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_round_error);

            mReceivedIconDrawable.setTintList(mBackgroundChatColor);
            mNotReceivedIconDrawable.setTintList(mBackgroundChatColor);
            mSendingIconDrawable.setTintList(mBackgroundChatColor);
            mErrorIconDrawable.setTintList(mErrorChatColor);
        }

        /*
        * Used by Item Decorator
        * */
        public void hiddenIconReceiver() {
            mBinding.imageReceiver.setVisibility(View.INVISIBLE);
        }

        /*
         * Used by Item Decorator
         * */
        public void showIconReceiver() {
            mBinding.imageReceiver.setVisibility(View.VISIBLE);
        }

        public void showLoading() {
            mBinding.progressBarLoading.setVisibility(View.VISIBLE);
            mBinding.layoutSender.setVisibility(View.GONE);
            mBinding.layoutReceiver.setVisibility(View.GONE);
        }

        public View getBackground(Chat item) {
            if (item.getSenderId().equals(mUser.getUid())) {
                return mBinding.senderChatBackground;
            } else {
                return mBinding.receiverChatBackground;
            }
        }

        public void changeSeenStatusVisibility(boolean shouldShow) {
            mBinding.imageMessageStatus.setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
        }

        private void findLastSeenStatus(Chat item) {
            List<Chat> mSenderChatList = mListItem.stream()
                                                    .filter(c -> c != null &&
                                                            c.getSenderId() != null &&
                                                            isSelf(c) &&
                                                            hasSeen(c))
                                                    .collect(Collectors.toList());

            if (isSelf(item)) {
                if (mSenderChatList.contains(item)) {
                    changeSeenStatusVisibility(mSenderChatList.indexOf(item) == mSenderChatList.size() - 1);
                } else {
                    if (hasSeen(item)) {
                        mSenderChatList.add(item);
                    }
                    changeSeenStatusVisibility(true);
                }
            }
        }

        private boolean hasSeen(Chat item) {
            return !item.getSeenBy().isEmpty();
        }
        /*
         * Used to bind specific chat status.
         * */
        public void bindSenderMessageStatus(Chat item) {
            if (item.getSeenBy() != null && item.getSeenBy().size() > 0) {
                findLastSeenStatus(item);

                item.getSeenBy()
                        .stream()
                        .findFirst()
                        .flatMap(id -> mParticipants.stream()
                                .filter(u -> u.getUid().equals(id))
                                .findFirst())
                        .ifPresent(u2 -> renderImage(u2.getPhotoUrl(), mBinding.imageMessageStatus));
                return;
            }

            switch (item.getStatus()) {
                case RECEIVED:
                    mBinding.imageMessageStatus.setImageDrawable(mReceivedIconDrawable);
                    break;
                case NOT_RECEIVED:
                    mBinding.imageMessageStatus.setImageDrawable(mNotReceivedIconDrawable);
                    break;
                case SENDING:
                    mBinding.imageMessageStatus.setImageDrawable(mSendingIconDrawable);
                    break;
                case ERROR:
                    mBinding.imageMessageStatus.setImageDrawable(mErrorIconDrawable);
                    break;
            }
        }

        public void bindUnsentStatus(Chat item) {
            View messageBackground;
            TextView messageText;
            String message;

            if (isSelf(item)) {
                message = mContext.getString(R.string.title_sender_chat_unsent);
                messageBackground = mBinding.senderChatBackground;
                messageText = mBinding.textSenderContent;
            } else {
                String[] otherNameArr = mOtherUser.getDisplayName().split(" ");
                String otherName = otherNameArr[otherNameArr.length - 1];

                message = mContext.getString(R.string.title_receiver_chat_unsent, otherName);
                messageBackground = mBinding.receiverChatBackground;
                messageText = mBinding.textReceiverContent;
            }

            if (item.isUnsent()) {
                renderUnsentMessage(messageBackground, messageText, message);
            } else {
                messageText.setText(message);
            }
        }

        public void bindTo(Chat preItem, Chat item, Chat nextItem) {
            String senderId = item.getSenderId();

            if (senderId == null) {
                mBinding.layoutWelcome.setVisibility(nextItem != null ? View.GONE : View.VISIBLE);
                mBinding.layoutReceiver.setVisibility(View.GONE);
                mBinding.layoutSender.setVisibility(View.GONE);
            } else {
                if (isSelf(item)) {
                    bindSenderMessage(preItem, item);
                } else {
                    bindReceiverMessage(preItem, item);
                }
            }
        }

        private void bindSenderMessage(Chat preItem, Chat item) {
            mBinding.layoutReceiver.setVisibility(View.GONE);
            mBinding.layoutSender.setVisibility(View.VISIBLE);
            mBinding.layoutWelcome.setVisibility(View.GONE);
            mBinding.textSenderContent.setText(item.getContent());
            mBinding.senderChatBackground.setBackgroundTintList(mBackgroundChatColor);

            if (item.isUnsent()) {
                var unsentContent = mContext.getString(R.string.title_sender_chat_unsent);

                renderUnsentMessage(mBinding.senderChatBackground, mBinding.textSenderContent, unsentContent);
            }

            bindSenderMessageStatus(item);
            shouldShowTimestamp(preItem, item);
        }

        private void bindReceiverMessage(Chat preItem, Chat item) {
            mBinding.layoutSender.setVisibility(View.GONE);
            mBinding.layoutReceiver.setVisibility(View.VISIBLE);
            mBinding.layoutWelcome.setVisibility(View.GONE);
            mBinding.textReceiverContent.setText(item.getContent());
            mBinding.imageReceiver.setVisibility(View.VISIBLE);

            // Render the sender profile image. Not otherUser because we have a group type
            renderImage(mOtherUser.getPhotoUrl(), mBinding.imageReceiver);

            if (item.isUnsent()) {
                String[] otherNameArr = mOtherUser.getDisplayName().split(" ");
                String otherName = otherNameArr[otherNameArr.length - 1];
                String unsentContent = mContext.getString(R.string.title_receiver_chat_unsent, otherName);

                renderUnsentMessage(mBinding.receiverChatBackground, mBinding.textReceiverContent, unsentContent);
            }

            shouldShowTimestamp(preItem, item);
        }

        /*
         * The method to check the duration time of two chat in a row larger than 10 minutes or not.
         * */
        private void shouldShowTimestamp(Chat preItem, Chat item) {
            if (preItem == null) {
                if (!item.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) {
                    mBinding.textTimestamp.setVisibility(View.VISIBLE);
                    mBinding.textTimestamp.setText(getReadableTime(item.getTimestamp()));
                }
                return;
            }

            if (preItem.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) {
                mBinding.textTimestamp.setVisibility(View.VISIBLE);
                mBinding.textTimestamp.setText(getReadableTime(item.getTimestamp()));
                return;
            }

            var from = preItem.getTimestamp();
            var to = item.getTimestamp();
            var minutesDuration = ChronoUnit.MINUTES.between(from, to);
            var boundDurationTime = 10;

            if (minutesDuration > boundDurationTime) {
                mBinding.textTimestamp.setVisibility(View.VISIBLE);
                mBinding.textTimestamp.setText(getReadableTime(item.getTimestamp()));
            } else {
                mBinding.textTimestamp.setVisibility(View.GONE);
            }
        }

        private void renderUnsentMessage(View background, TextView contentView, String content) {
            var backgroundTintColor = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.hint_text_color_with_icon));
            var backgroundDrawable = ContextCompat.getDrawable(mContext, R.drawable.background_rounded_chat_unsent);

            background.setBackgroundTintList(backgroundTintColor);
            background.setBackground(backgroundDrawable);

            contentView.setText(content);
            contentView.setTypeface(mBinding.textSenderContent.getTypeface(), Typeface.ITALIC);
            contentView.setTextColor(ContextCompat.getColor(mContext, R.color.hint_text_color_with_icon));
        }

        private void renderImage(String url, ImageView container) {
            Picture.loadUserAvatar(mContext, url).into(container);
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

        private boolean isSelf(Chat item) {
            return item.getSenderId().equals(mUser.getUid());
        }
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ItemChatProfileBinding mBinding;
        Context mContext;
        boolean mIsSelf;

        public ProfileViewHolder(@NonNull ItemChatProfileBinding binding, Context context, boolean isSelf) {
            super(binding.getRoot());

            mBinding = binding;
            mContext = context;
            mIsSelf  = isSelf;
        }

        public void bindTo(User user) {
            String photoUrl;
            String displayName;
            String bio;
            String username;

            // TODO: load remote user
            if (user == null) {
                photoUrl    = null;
                displayName = "Unknown";
                bio         = "Unknown";
                username    = "Unknown";
            } else {
                photoUrl    = user.getPhotoUrl();
                bio         = user.getBiographic() == null ? "" : user.getBiographic();
                username    = user.getUsername() == null ? "" : user.getUsername();
                displayName = mIsSelf ? mContext.getString(R.string.title_just_you) : user.getDisplayName();
            }
            Picture.loadUserAvatar(mContext, photoUrl).into(mBinding.imageAvatar);

            mBinding.textDisplayName.setText(displayName);
            mBinding.textBio.setText(bio);
            mBinding.textUserName.setText(username);

            mBinding.textBio.setVisibility(TextUtils.isEmpty(bio) ? View.GONE : View.VISIBLE);
            mBinding.textUserName.setVisibility(TextUtils.isEmpty(username) ? View.GONE : View.VISIBLE);
            mBinding.buttonViewProfile.setVisibility(mIsSelf ? View.GONE : View.VISIBLE);
        }
    }
}
