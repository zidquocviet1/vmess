package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemChatBinding;
import com.mqv.realtimechatapplication.databinding.ItemChatProfileBinding;
import com.mqv.realtimechatapplication.databinding.ItemChatProfileGroupBinding;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.ui.data.ConversationMessageItem;
import com.mqv.realtimechatapplication.ui.data.ConversationMetadata;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Picture;

import java.util.List;
import java.util.Objects;

public class ChatListAdapter extends ListAdapter<Chat, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final List<Chat> mChatList;
    private final List<User> mParticipants;
    private final ColorStateList mChatColorStateList;
    private final User mCurrentUser;
    private final ConversationType mConversationType;

    private User mOtherUserDetail;
    private ConversationMetadata mConversationMetadata;
    private ConversationGroupOption mConversationCallback;

    private static final int VIEW_PROFILE = -1;
    private static final int VIEW_PROFILE_SELF = 0;
    private static final int VIEW_CHAT = 1;
    private static final int VIEW_LOAD_MORE = 2;
    private static final int VIEW_PROFILE_GROUP = 3;

    public static final String PROFILE_USER_PAYLOAD = "profile_user";
    public static final String TIMESTAMP_MESSAGE_PAYLOAD = "timestamp";
    public static final String MESSAGE_STATUS_PAYLOAD = "message_status";
    public static final String MESSAGE_UNSENT_PAYLOAD = "message_unsent";
    public static final String MESSAGE_SHAPE_PAYLOAD = "message_shape";

    public interface ConversationGroupOption {
        void addMember();

        void changeGroupName();

        void viewGroupMember();
    }

    public ChatListAdapter(Context context,
                           List<Chat> chatList,
                           List<User> participants,
                           ColorStateList chatColorStateList,
                           @NonNull User user,
                           ConversationType type) {
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
                        Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                        oldItem.getConversationId().equals(newItem.getConversationId()) &&
                        oldItem.isUnsent().equals(newItem.isUnsent());
            }
        });
        mContext = context;
        mChatColorStateList = chatColorStateList;
        mCurrentUser = user;
        mConversationType = type;
        mChatList = chatList;
        mParticipants = participants;
    }

    public void addChat(Chat chat) {
        mChatList.add(chat);

        if (mChatList.size() - 2 >= 0) {
            notifyItemChanged(mChatList.size() - 2, MESSAGE_SHAPE_PAYLOAD);
        }
        notifyItemInserted(mChatList.size() - 1);
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

    public void setConversationMetadata(ConversationMetadata metadata) {
        mConversationMetadata = metadata;
    }

    public void registerConversationOption(ConversationGroupOption callback) {
        mConversationCallback = callback;
    }

    @Override
    public int getItemViewType(int position) {
        var item = getItem(position);

        if (item == null)
            return VIEW_LOAD_MORE;

        if (item.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) {
            if (mConversationType == ConversationType.GROUP) {
                return VIEW_PROFILE_GROUP;
            } else if (mConversationType == ConversationType.SELF) {
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
        LayoutInflater inflater = LayoutInflater.from(mContext);

        switch (type) {
            case VIEW_PROFILE:
            case VIEW_PROFILE_SELF:
                return new ProfileViewHolder(ItemChatProfileBinding.bind(inflater.inflate(R.layout.item_chat_profile, parent, false)), mContext, type == VIEW_PROFILE_SELF);
            case VIEW_PROFILE_GROUP:
                return new ProfileGroupViewHolder(ItemChatProfileGroupBinding.bind(inflater.inflate(R.layout.item_chat_profile_group, parent, false)), mConversationCallback);
            default:
                return new ChatListViewHolder(ItemChatBinding.bind(inflater.inflate(R.layout.item_chat, parent, false)),
                        mCurrentUser,
                        mParticipants,
                        mChatColorStateList,
                        mChatList,
                        mConversationMetadata);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        if (holder instanceof ChatListViewHolder) {
            ChatListViewHolder chatHolder = (ChatListViewHolder) holder;
            ConversationMessageItem bindableItem = chatHolder.getItemBindable();

            if (!payloads.isEmpty()) {
                for (Object s : payloads) {
                    if (s.equals(MESSAGE_STATUS_PAYLOAD)) {
                        bindableItem.bindStatus(getItem(position));
                    } else if (s.equals(TIMESTAMP_MESSAGE_PAYLOAD)) {
                        bindableItem.showTimestamp(getItem(position - 1), getItem(position));
                    } else if (s.equals(MESSAGE_UNSENT_PAYLOAD)) {
                        bindableItem.bindUnsentMessage(getItem(position));
                    } else if (s.equals(MESSAGE_SHAPE_PAYLOAD)) {
                        bindableItem.bindMessageShape(getItem(position));
                    }
                }
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
        if (holder instanceof ChatListViewHolder) {
            var chatHolder = (ChatListViewHolder) holder;

            if (getItemViewType(position) == VIEW_LOAD_MORE) {
                chatHolder.getItemBindable().showLoading();
            } else {
                chatHolder.getItemBindable().bind(getItem(position));
            }
        } else if (holder instanceof ProfileViewHolder) {
            ProfileViewHolder profileHolder = (ProfileViewHolder) holder;

            if (getItemViewType(position) == VIEW_PROFILE_SELF) {
                profileHolder.bindTo(mCurrentUser);
            } else {
                profileHolder.bindTo(mOtherUserDetail);
            }
        } else if (holder instanceof ProfileGroupViewHolder) {
            ((ProfileGroupViewHolder) holder).bindGroup(mConversationMetadata);
        }
    }

    public static void initializePool(RecyclerView.RecycledViewPool pool) {
        pool.setMaxRecycledViews(VIEW_CHAT, 25);
        pool.setMaxRecycledViews(VIEW_LOAD_MORE, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE_SELF, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE, 1);
    }

    static class ChatListViewHolder extends RecyclerView.ViewHolder {
        final ItemChatBinding mBinding;
        final ConversationMessageItem mMessageItem;

        public ChatListViewHolder(@NonNull ItemChatBinding binding,
                                  @NonNull User user,
                                  @NonNull List<User> participants,
                                  @NonNull ColorStateList colorStateList,
                                  @NonNull List<Chat> listItem,
                                  @NonNull ConversationMetadata metadata) {
            super(binding.getRoot());

            mBinding = binding;
            mMessageItem = new ConversationMessageItem(binding, listItem, participants, user, metadata, colorStateList);
        }

        public ConversationMessageItem getItemBindable() {
            return mMessageItem;
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

    static class ProfileGroupViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatProfileGroupBinding mBinding;
        private final Context mContext;

        public ProfileGroupViewHolder(@NonNull ItemChatProfileGroupBinding binding,
                                      ConversationGroupOption conversationCallback) {
            super(binding.getRoot());

            mBinding = binding;
            mContext = mBinding.getRoot().getContext();

            if (conversationCallback != null) {
                mBinding.buttonAddMember.setOnClickListener(v -> conversationCallback.addMember());
                mBinding.buttonEditName.setOnClickListener(v -> conversationCallback.changeGroupName());
                mBinding.buttonViewGroupMember.setOnClickListener(v -> conversationCallback.viewGroupMember());
            }
        }

        public void bindGroup(ConversationMetadata metadata) {
            mBinding.textGroupName.setText(metadata.getConversationName());
            mBinding.textWhoCreated.setText(mContext.getString(R.string.label_who_create_this_group, metadata.getConversationCreatedBy()));

            List<String> thumbnails = metadata.getConversationThumbnail();

            Picture.loadUserAvatar(mContext, thumbnails.get(0)).into(mBinding.imageAvatar3);
            Picture.loadUserAvatar(mContext, thumbnails.get(1)).into(mBinding.imageAvatar2);

            if (thumbnails.size() == 2) {
                mBinding.layoutAvatar1.setVisibility(View.GONE);
            } else if (thumbnails.size() == 3) {
                Picture.loadUserAvatar(mContext, thumbnails.get(2)).into(mBinding.imageAvatar1);
                mBinding.textMoreNumber.setVisibility(View.GONE);
            } else {
                mBinding.imageAvatar1.setImageDrawable(Picture.getErrorAvatarLoaded(mContext));
                mBinding.textMoreNumber.setVisibility(View.VISIBLE);
                mBinding.textMoreNumber.setText(mContext.getString(R.string.label_text_more_number, thumbnails.size() - 2));
            }
        }
    }
}
