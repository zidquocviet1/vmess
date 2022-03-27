package com.mqv.vmess.ui.adapter;

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

import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ItemChatBinding;
import com.mqv.vmess.databinding.ItemChatNotificationMessageBinding;
import com.mqv.vmess.databinding.ItemChatProfileBinding;
import com.mqv.vmess.databinding.ItemChatProfileGroupBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.ui.data.ConversationMessageItem;
import com.mqv.vmess.ui.data.ConversationMetadata;
import com.mqv.vmess.util.MessageUtil;
import com.mqv.vmess.util.Picture;

import java.util.List;
import java.util.Objects;

public class ChatListAdapter extends ListAdapter<Chat, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final List<Chat> mChatList;
    private final ColorStateList mChatColorStateList;
    private final User mCurrentUser;
    private final ConversationType mConversationType;

    private List<User> mParticipants;
    private User mOtherUserDetail;
    private ConversationMetadata mConversationMetadata;
    private ConversationGroupOption mConversationCallback;
    private BaseAdapter.ItemEventHandler mItemEventHandler;

    private static final int VIEW_PROFILE = -1;
    private static final int VIEW_PROFILE_SELF = 0;
    private static final int VIEW_CHAT = 1;
    private static final int VIEW_LOAD_MORE = 2;
    private static final int VIEW_PROFILE_GROUP = 3;
    private static final int VIEW_CHAT_NOTIFICATION = 4;
    private static final int VIEW_CHAT_MULTI_MEDIA = 5;
    private static final int VIEW_CHAT_UNSENT = 6;

    public static final String PROFILE_USER_PAYLOAD = "profile_user";
    public static final String TIMESTAMP_MESSAGE_PAYLOAD = "timestamp";
    public static final String MESSAGE_STATUS_PAYLOAD = "message_status";
    public static final String MESSAGE_UNSENT_PAYLOAD = "message_unsent";
    public static final String MESSAGE_SHAPE_PAYLOAD = "message_shape";

    public interface ConversationGroupOption {
        void addMember();

        void changeGroupName();

        void viewGroupMember();

        void changeGroupThumbnail();
    }

    public ChatListAdapter(Context context,
                           List<Chat> chatList,
                           List<User> participants,
                           ColorStateList chatColorStateList,
                           @NonNull FirebaseUser user,
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
        mCurrentUser = LoggedInUserManager.getInstance().parseFirebaseUser(user);
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

    // Update the participants after new member was added
    public void setParticipants(@NonNull List<User> participants) {
        mParticipants = participants;
    }

    public void registerConversationOption(ConversationGroupOption callback) {
        mConversationCallback = callback;
    }

    public void registerItemEventListener(BaseAdapter.ItemEventHandler callback) {
        mItemEventHandler = callback;
    }

    @Override
    public int getItemViewType(int position) {
        var item = getItem(position);

        if (item == null)
            return VIEW_LOAD_MORE;

        if (MessageUtil.isDummyProfileMessage(item)) {
            if (mConversationType == ConversationType.GROUP) {
                return VIEW_PROFILE_GROUP;
            } else if (mConversationType == ConversationType.SELF) {
                return VIEW_PROFILE_SELF;
            }
            return VIEW_PROFILE;
        } else if (MessageUtil.isNotificationMessage(item)) {
            return VIEW_CHAT_NOTIFICATION;
        } else if (MessageUtil.isMultiMediaMessage(item)) {
            return VIEW_CHAT_MULTI_MEDIA;
        } else {
            return item.isUnsent() ? VIEW_CHAT_UNSENT : VIEW_CHAT;
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
            case VIEW_CHAT_NOTIFICATION:
                return new ChatNotificationMessageViewHolder(ItemChatNotificationMessageBinding.bind(inflater.inflate(R.layout.item_chat_notification_message, parent, false)), mParticipants);
            default:
                return new ChatListViewHolder(ItemChatBinding.bind(inflater.inflate(R.layout.item_chat, parent, false)),
                        mCurrentUser,
                        mParticipants,
                        mChatColorStateList,
                        mChatList,
                        mConversationMetadata,
                        mItemEventHandler);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        if (holder instanceof ChatListViewHolder) {
            ChatListViewHolder chatHolder = (ChatListViewHolder) holder;
            ConversationMessageItem bindableItem = chatHolder.getItemBindable();

            if (!payloads.isEmpty() && getItem(position) != null) {
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
        } else if (holder instanceof ChatNotificationMessageViewHolder) {
            ((ChatNotificationMessageViewHolder) holder).bind(getItem(position));
        }
    }

    public static void initializePool(RecyclerView.RecycledViewPool pool) {
        pool.setMaxRecycledViews(VIEW_CHAT, 25);
        pool.setMaxRecycledViews(VIEW_LOAD_MORE, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE_SELF, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE, 1);
        pool.setMaxRecycledViews(VIEW_CHAT_UNSENT, 1);
    }

    static class ChatListViewHolder extends RecyclerView.ViewHolder {
        final ItemChatBinding mBinding;
        final ConversationMessageItem mMessageItem;

        public ChatListViewHolder(@NonNull ItemChatBinding binding,
                                  @NonNull User user,
                                  @NonNull List<User> participants,
                                  @NonNull ColorStateList colorStateList,
                                  @NonNull List<Chat> listItem,
                                  ConversationMetadata metadata,
                                  BaseAdapter.ItemEventHandler handler) {
            super(binding.getRoot());

            mBinding = binding;
            mMessageItem = new ConversationMessageItem(binding, listItem, participants, user, metadata, colorStateList);

            mBinding.senderChatBackground.setOnClickListener(v -> {
                if (handler != null) {
                    handler.onItemClick(getLayoutPosition());
                }
            });
            mBinding.receiverChatBackground.setOnClickListener(v -> {
                if (handler != null) {
                    handler.onItemClick(getLayoutPosition());
                }
            });
            mBinding.senderChatBackground.setOnLongClickListener(v -> {
                if (handler != null) {
                    handler.onItemLongClick(getLayoutPosition());
                    return true;
                } else {
                    return false;
                }
            });
            mBinding.receiverChatBackground.setOnLongClickListener(v -> {
                if (handler != null) {
                    handler.onItemLongClick(getLayoutPosition());
                    return true;
                } else {
                    return false;
                }
            });
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

            if (user == null) {
                photoUrl    = null;
                displayName = mContext.getString(R.string.dummy_user_name);
                bio         = "";
                username    = "";
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
                mBinding.layoutGroupThumbnail.setOnClickListener(v -> conversationCallback.changeGroupThumbnail());
            }
        }

        public void bindGroup(ConversationMetadata metadata) {
            mBinding.textGroupName.setText(metadata.getConversationName());
            mBinding.textWhoCreated.setText(mContext.getString(R.string.label_who_create_this_group, metadata.getConversationCreatedBy()));

            List<String> thumbnails = metadata.getConversationThumbnail();
            List<User> participants = metadata.getConversationParticipants();

            if (thumbnails.isEmpty()) {
                Picture.loadUserAvatar(mContext, null).into(mBinding.imageAvatar3);

                mBinding.layoutAvatar2.setVisibility(View.GONE);
                mBinding.layoutAvatar1.setVisibility(View.GONE);
                mBinding.textMoreNumber.setVisibility(View.GONE);
            } else if (thumbnails.size() == 1) {
                Picture.loadUserAvatar(mContext, thumbnails.get(0)).into(mBinding.imageAvatar3);

                mBinding.layoutAvatar2.setVisibility(View.GONE);
                mBinding.layoutAvatar1.setVisibility(View.GONE);
                mBinding.textMoreNumber.setVisibility(View.GONE);
            } else {
                Picture.loadUserAvatar(mContext, thumbnails.get(0)).into(mBinding.imageAvatar3);
                Picture.loadUserAvatar(mContext, thumbnails.get(1)).into(mBinding.imageAvatar2);

                mBinding.layoutAvatar2.setVisibility(View.VISIBLE);

                if (participants.size() == 3) {
                    mBinding.layoutAvatar1.setVisibility(View.GONE);
                } else if (participants.size() == 4) {
                    Picture.loadUserAvatar(mContext, thumbnails.get(2)).into(mBinding.imageAvatar1);
                    mBinding.layoutAvatar1.setVisibility(View.VISIBLE);
                    mBinding.textMoreNumber.setVisibility(View.GONE);
                } else {
                    mBinding.layoutAvatar1.setVisibility(View.VISIBLE);
                    mBinding.imageAvatar1.setVisibility(View.VISIBLE);
                    mBinding.imageAvatar1.setImageDrawable(Picture.getErrorAvatarLoaded(mContext));
                    mBinding.textMoreNumber.setVisibility(View.VISIBLE);
                    mBinding.textMoreNumber.setText(mContext.getString(R.string.label_text_more_number, participants.size() - 3));
                }
            }
        }
    }

    static class ChatNotificationMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatNotificationMessageBinding mBinding;
        private final List<User> mParticipants;
        private final Context mContext;

        public ChatNotificationMessageViewHolder(@NonNull ItemChatNotificationMessageBinding binding, List<User> mParticipants) {
            super(binding.getRoot());

            this.mBinding = binding;
            this.mParticipants = mParticipants;
            this.mContext = binding.getRoot().getContext();
        }

        public void bind(Chat item) {
            if (MessageUtil.isChangeGroupNameMessage(item)) {
                bindChangeGroupName(item);
            } else if (MessageUtil.isAddedMemberMessage(item)) {
                bindAddedMember(item);
            } else if (MessageUtil.isChangeThumbnailMessage(item)) {
                bindChangeThumbnail(item);
            } else if (MessageUtil.isMemberLeaveGroupMessage(item)) {
                bindLeaveGroup(item);
            } else if (MessageUtil.isRemoveMemberMessage(item)) {
                bindRemoveMember(item);
            }
        }

        private void bindChangeGroupName(Chat item) {
            String content  = item.getContent(); // This is the new name of the group
            String senderId = item.getSenderId(); // That user who in the group changed the name
            User   sender   = findUserIfNotExists(senderId);

            mBinding.textMessage.setText(mContext.getString(R.string.msg_who_changed_the_group_name, sender.getDisplayName(), content));
        }

        private void bindAddedMember(Chat item) {
            String content  = item.getContent(); // This is the id of the user has been added
            String senderId = item.getSenderId(); // That user who in the group added that member

            User   member   = findUserIfNotExists(content);
            User   sender   = findUserIfNotExists(senderId);

            mBinding.textMessage.setText(mContext.getString(R.string.msg_who_added_another_to_the_group, sender.getDisplayName(), member.getDisplayName()));
        }

        private void bindRemoveMember(Chat item) {
            String content  = item.getContent(); // That member who was removed in the group
            String senderId = item.getSenderId();

            User   sender   = findUserIfNotExists(senderId);

            mBinding.textMessage.setText(mContext.getString(R.string.msg_who_remove_another_member, sender.getDisplayName(), content));
        }

        private void bindLeaveGroup(Chat item) {
            String senderId = item.getSenderId();
            User   sender   = findUserIfNotExists(senderId);

            mBinding.textMessage.setText(mContext.getString(R.string.msg_who_leave_group, sender.getDisplayName()));
        }

        private void bindChangeThumbnail(Chat item) {
            String senderId = item.getSenderId();
            User   sender   = findUserIfNotExists(senderId);

            mBinding.textMessage.setText(mContext.getString(R.string.msg_who_change_group_thumbnail, sender.getDisplayName()));
        }

        private User findUserIfNotExists(String senderId) {
            return mParticipants.stream()
                                .filter(u -> u.getUid()
                                              .equals(senderId))
                                .findFirst()
                                .orElse(new User.Builder()
                                                .setDisplayName(mContext.getString(R.string.dummy_user_name))
                                                .create());
        }
    }
}
