package com.mqv.vmess.ui.adapter;

import static com.mqv.vmess.util.DateTimeHelper.getMessageDateTimeFormatted;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ItemChatBinding;
import com.mqv.vmess.databinding.ItemChatNotificationMessageBinding;
import com.mqv.vmess.databinding.ItemChatOutgoingMultiMediaBinding;
import com.mqv.vmess.databinding.ItemChatProfileBinding;
import com.mqv.vmess.databinding.ItemChatProfileGroupBinding;
import com.mqv.vmess.databinding.ItemChatReceivedMultiMediaBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.network.model.type.MessageStatus;
import com.mqv.vmess.ui.components.ImageClickListener;
import com.mqv.vmess.ui.components.ImageLongClickListener;
import com.mqv.vmess.ui.components.LinkPreviewView;
import com.mqv.vmess.ui.components.conversation.ConversationPhotoView;
import com.mqv.vmess.ui.components.conversation.ConversationVideoView;
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewListener;
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata;
import com.mqv.vmess.ui.data.ConversationMessageItem;
import com.mqv.vmess.ui.data.ConversationMetadata;
import com.mqv.vmess.util.MessageUtil;
import com.mqv.vmess.util.Picture;
import com.mqv.vmess.util.views.Stub;
import com.mqv.vmess.util.views.ViewUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChatListAdapter extends ListAdapter<Chat, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final List<Chat> mChatList;
    private final User mCurrentUser;
    private final ConversationType mConversationType;

    private List<User> mParticipants;
    private User mOtherUserDetail;
    private ConversationMetadata mConversationMetadata;
    private ConversationGroupOption mConversationCallback;
    private BaseAdapter.ItemEventHandler mItemEventHandler;
    private LinkPreviewListener mLinkPreviewListener;
    private BiConsumer<Chat, Chat.Video> mVideoListener;
    private Runnable mOpenDetailCallback;

    private static final int VIEW_PROFILE = -1;
    private static final int VIEW_PROFILE_SELF = 0;
    private static final int VIEW_CHAT = 1;
    private static final int VIEW_LOAD_MORE = 2;
    private static final int VIEW_PROFILE_GROUP = 3;
    private static final int VIEW_CHAT_NOTIFICATION = 4;
    private static final int VIEW_CHAT_RECEIVED_MULTI_MEDIA = 5;
    private static final int VIEW_CHAT_OUTGOING_MULTI_MEDIA = 7;
    private static final int VIEW_CHAT_UNSENT = 6;
    private static final int VIEW_CHAT_WELCOME = 8;

    public static final String PROFILE_USER_PAYLOAD = "profile_user";
    public static final String TIMESTAMP_MESSAGE_PAYLOAD = "timestamp";
    public static final String MESSAGE_STATUS_PAYLOAD = "message_status";
    public static final String MESSAGE_UNSENT_PAYLOAD = "message_unsent";
    public static final String MESSAGE_SHAPE_PAYLOAD = "message_shape";
    public static final String MESSAGE_SENDER = "message_sender";
    public static final String MESSAGE_COLOR = "message_color";

    public interface ConversationGroupOption {
        void addMember();

        void changeGroupName();

        void viewGroupMember();

        void changeGroupThumbnail();
    }

    public ChatListAdapter(Context context,
                           List<Chat> chatList,
                           List<User> participants,
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

    public void setUserLeftGroup(List<User> userLeftGroup) {
        ChatNotificationMessageViewHolder.setUserLeftGroup(userLeftGroup);
        ChatMultiMediaViewHolder.setUserLeftGroup(userLeftGroup);
    }

    public void setUserDetail(User user) {
        mOtherUserDetail = user;
    }

    public void setConversationMetadata(ConversationMetadata metadata) {
        mConversationMetadata = metadata;
    }

    public void setChatColor(ColorStateList color) {
        ConversationMessageItem.setChatColor(color);
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

    public void registerLinkPreviewListener(LinkPreviewListener callback) {
        mLinkPreviewListener = callback;
    }

    public void registerVideoListener(BiConsumer<Chat, Chat.Video> callback) {
        mVideoListener = callback;
    }

    public void registerOpenConversationDetail(Runnable callback) {
        mOpenDetailCallback = callback;
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
        } else if (item.isUnsent()) {
            return VIEW_CHAT_UNSENT;
        } else if (MessageUtil.isMultiMediaMessage(item)) {
            return item.getSenderId().equals(mCurrentUser.getUid()) ? VIEW_CHAT_OUTGOING_MULTI_MEDIA : VIEW_CHAT_RECEIVED_MULTI_MEDIA;
        } else if (MessageUtil.isWelcomeMessage(item)) {
            return VIEW_CHAT_WELCOME;
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
                return new ProfileViewHolder(ItemChatProfileBinding.bind(inflater.inflate(R.layout.item_chat_profile, parent, false)), mContext, type == VIEW_PROFILE_SELF, mOpenDetailCallback);
            case VIEW_PROFILE_GROUP:
                return new ProfileGroupViewHolder(ItemChatProfileGroupBinding.bind(inflater.inflate(R.layout.item_chat_profile_group, parent, false)), mConversationCallback);
            case VIEW_CHAT_NOTIFICATION:
                return new ChatNotificationMessageViewHolder(ItemChatNotificationMessageBinding.bind(inflater.inflate(R.layout.item_chat_notification_message, parent, false)), mParticipants);
            case VIEW_CHAT_RECEIVED_MULTI_MEDIA:
                return new ChatMultiMediaViewHolder(inflater.inflate(R.layout.item_chat_received_multi_media, parent, false), true, mCurrentUser,
                        mParticipants,
                        mChatList,
                        mLinkPreviewListener,
                        mVideoListener);
            case VIEW_CHAT_OUTGOING_MULTI_MEDIA:
                return new ChatMultiMediaViewHolder(inflater.inflate(R.layout.item_chat_outgoing_multi_media, parent, false), false, mCurrentUser,
                        mParticipants,
                        mChatList,
                        mLinkPreviewListener,
                        mVideoListener);
            default:
                return new ChatListViewHolder(ItemChatBinding.bind(inflater.inflate(R.layout.item_chat, parent, false)),
                        mCurrentUser,
                        mParticipants,
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
                    } else if (s.equals(MESSAGE_COLOR)) {
                        bindableItem.bindMessageColor(getItem(position));
                    }
                }
            }
        } else if (holder instanceof ProfileViewHolder) {
            if (!payloads.isEmpty() && payloads.get(0).equals(PROFILE_USER_PAYLOAD)) {
                var profileHolder = (ProfileViewHolder) holder;

                profileHolder.bindTo(mOtherUserDetail);
            }
        } else if (holder instanceof ChatMultiMediaViewHolder) {
            if (!payloads.isEmpty() && payloads.get(0).equals(MESSAGE_SENDER)) {
                ((ChatMultiMediaViewHolder) holder).bindNameAndAvatar(getItem(position));
            } else {
                ((ChatMultiMediaViewHolder) holder).bind(getItem(position));
            }
        } else if (holder instanceof ChatNotificationMessageViewHolder) {
            ((ChatNotificationMessageViewHolder) holder).bind(getItem(position));
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
        } else if (holder instanceof ChatMultiMediaViewHolder) {
            ((ChatMultiMediaViewHolder) holder).bind(getItem(position));
        }
    }

    public static void initializePool(RecyclerView.RecycledViewPool pool) {
        pool.setMaxRecycledViews(VIEW_CHAT, 25);
        pool.setMaxRecycledViews(VIEW_LOAD_MORE, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE_SELF, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE, 1);
        pool.setMaxRecycledViews(VIEW_PROFILE_GROUP, 1);
        pool.setMaxRecycledViews(VIEW_CHAT_UNSENT, 1);
        pool.setMaxRecycledViews(VIEW_CHAT_WELCOME, 1);
    }

    static class ChatListViewHolder extends RecyclerView.ViewHolder {
        final ItemChatBinding mBinding;
        final ConversationMessageItem mMessageItem;

        public ChatListViewHolder(@NonNull ItemChatBinding binding,
                                  @NonNull User user,
                                  @NonNull List<User> participants,
                                  @NonNull List<Chat> listItem,
                                  ConversationMetadata metadata,
                                  BaseAdapter.ItemEventHandler handler) {
            super(binding.getRoot());

            mBinding = binding;
            mMessageItem = new ConversationMessageItem(binding, listItem, participants, user, metadata);

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

        public ProfileViewHolder(@NonNull ItemChatProfileBinding binding, Context context, boolean isSelf, Runnable openDetailCallback) {
            super(binding.getRoot());

            mBinding = binding;
            mContext = context;
            mIsSelf  = isSelf;
            mBinding.buttonViewProfile.setOnClickListener(v -> {
                if (openDetailCallback != null) {
                    openDetailCallback.run();
                }
            });
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
                mBinding.conversationBanner.setOnThumbnailClickListener(conversationCallback::changeGroupThumbnail);
            }
        }

        public void bindGroup(ConversationMetadata metadata) {
            mBinding.conversationBanner.setMetadata(metadata);
            mBinding.conversationBanner.setSingleThumbnailSize(
                    ViewUtil.getLargeUserAvatarPixel(mContext.getResources()),
                    ViewUtil.getLargeUserAvatarPixel(mContext.getResources())
            );
            mBinding.textWhoCreated.setText(mContext.getString(R.string.label_who_create_this_group, metadata.getConversationCreatedBy()));
        }
    }

    static class ChatNotificationMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatNotificationMessageBinding mBinding;
        private final List<User> mParticipants;
        private final Context mContext;
        private static List<User> sUserLeftGroup = new ArrayList<>();

        public ChatNotificationMessageViewHolder(@NonNull ItemChatNotificationMessageBinding binding,
                                                 List<User> mParticipants) {
            super(binding.getRoot());

            this.mBinding = binding;
            this.mParticipants = mParticipants;
            this.mContext = binding.getRoot().getContext();
        }

        public static void setUserLeftGroup(List<User> userLeftGroup) {
            sUserLeftGroup = userLeftGroup;
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
            Predicate<User> idPredicate = user -> user.getUid().equals(senderId);

            return mParticipants.stream()
                                .filter(idPredicate)
                                .findFirst()
                                .orElse(sUserLeftGroup.stream()
                                                      .filter(idPredicate)
                                                      .findFirst()
                                                      .orElse(createNonUser()));
        }

        private User createNonUser() {
            return new User.Builder()
                           .setUid("123")
                           .setDisplayName(mContext.getString(R.string.dummy_user_name))
                           .create();
        }
    }

    static class ChatMultiMediaViewHolder extends RecyclerView.ViewHolder {
        ItemChatReceivedMultiMediaBinding mReceivedBinding;
        ItemChatOutgoingMultiMediaBinding mOutgoingBinding;
        Stub<ConversationPhotoView> mMediaThumbnailStub;
        Stub<LinkPreviewView> mLinkPreviewStub;
        Stub<ConversationVideoView> mVideoStub;
        User mUser;
        List<User> mParticipants;
        List<Chat> mListItem;
        Context mContext;
        LinkPreviewListener mLinkPreviewListener;
        BiConsumer<Chat, Chat.Video> mVideoListener;

        Drawable mReceivedIconDrawable;
        Drawable mNotReceivedIconDrawable;
        Drawable mSendingIconDrawable;

        boolean mIsReceived;

        private static List<User> sUserLeftGroup = new ArrayList<>();

        public ChatMultiMediaViewHolder(@NonNull View itemView, boolean isReceived,
                                        @NonNull User user,
                                        @NonNull List<User> participants,
                                        @NonNull List<Chat> listItem,
                                        @NonNull LinkPreviewListener linkPreviewListener,
                                        @NonNull BiConsumer<Chat, Chat.Video> videoListener) {
            super(itemView);

            if (isReceived) {
                mReceivedBinding = ItemChatReceivedMultiMediaBinding.bind(itemView);
                mMediaThumbnailStub = new Stub<>(mReceivedBinding.mediaThumbnailStub);
                mLinkPreviewStub = new Stub<>(mReceivedBinding.mediaLinkPreviewStub);
                mVideoStub = new Stub<>(mReceivedBinding.mediaVideoStub);
            } else {
                mOutgoingBinding = ItemChatOutgoingMultiMediaBinding.bind(itemView);
                mMediaThumbnailStub = new Stub<>(mOutgoingBinding.mediaThumbnailStub);
                mLinkPreviewStub = new Stub<>(mOutgoingBinding.mediaLinkPreviewStub);
                mVideoStub = new Stub<>(mOutgoingBinding.mediaVideoStub);
            }
            mContext = itemView.getContext();
            mIsReceived = isReceived;
            mParticipants = participants;
            mListItem = listItem;
            mLinkPreviewListener = linkPreviewListener;
            mVideoListener = videoListener;
            mUser = user;
            mSendingIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_outline_circle);
            mReceivedIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle);
            mNotReceivedIconDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle_outline);

            mReceivedIconDrawable.setTintList(ConversationMessageItem.getChatColor());
            mNotReceivedIconDrawable.setTintList(ConversationMessageItem.getChatColor());
            mSendingIconDrawable.setTintList(ConversationMessageItem.getChatColor());
        }

        public static void setUserLeftGroup(List<User> userLeftGroup) {
            sUserLeftGroup = userLeftGroup;
        }

        public void bindNameAndAvatar(Chat chat) {
            if (mIsReceived) {
                Picture.loadUserAvatar(mContext, getSenderFromChat(chat.getSenderId()).getPhotoUrl()).into(mReceivedBinding.imageReceiver);
            }
        }

        public void bind(Chat message) {
            bindByType(message);
            bindReceiverThumbnailAndMessageStatus(message);
            showTimestamp(message);
        }

        private void bindByType(Chat message) {
            if (MessageUtil.isPhotoMessage(message)) {
                bindPhotoMessage(message);
            } else if (MessageUtil.isVideoMessage(message)) {
                bindVideoMessage(message);
            } else if (MessageUtil.isShareMessage(message)) {
                bindShareMessage(message);
            } else if (MessageUtil.isFileMessage(message)) {
                bindFileMessage();
            } else if (MessageUtil.isCallMessage(message)) {
                bindCallMessage();
            }
        }

        private void bindReceiverThumbnailAndMessageStatus(Chat message) {
            if (mIsReceived) {
                Picture.loadUserAvatar(mContext, getSenderFromChat(message.getSenderId()).getPhotoUrl())
                        .into(mReceivedBinding.imageReceiver);
            } else {
                if (!message.getSeenBy().isEmpty()) {
                    Picture.loadUserAvatar(mContext, getSenderFromChat(message.getSeenBy().get(0)).getPhotoUrl())
                            .into(mOutgoingBinding.imageMessageStatus);
                } else {
                    MessageStatus status = message.getStatus();

                    if (status == MessageStatus.SENDING) {
                        mOutgoingBinding.imageMessageStatus.setImageDrawable(mSendingIconDrawable);
                    } else if (status == MessageStatus.NOT_RECEIVED) {
                        mOutgoingBinding.imageMessageStatus.setImageDrawable(mNotReceivedIconDrawable);
                    } else if (status == MessageStatus.RECEIVED) {
                        mOutgoingBinding.imageMessageStatus.setImageDrawable(mReceivedIconDrawable);
                    }
                }
                findLastSeenStatus(message);
            }
        }

        private void bindPhotoMessage(Chat message) {
            mLinkPreviewStub.get().setVisibility(View.GONE);
            mVideoStub.get().setVisibility(View.GONE);
            mMediaThumbnailStub.get().setVisibility(View.VISIBLE);

            mMediaThumbnailStub.get().setOnThumbnailClickListener((ImageClickListener) mContext);
            mMediaThumbnailStub.get().setOnThumbnailLongClickListener((ImageLongClickListener) mContext);
            mMediaThumbnailStub.get().setImageResource(mIsReceived, message);
        }

        private void bindVideoMessage(Chat message) {
            mVideoStub.get().setVisibility(View.VISIBLE);
            mMediaThumbnailStub.get().setVisibility(View.GONE);
            mLinkPreviewStub.get().setVisibility(View.GONE);

            mVideoStub.get().setOnThumbnailLongClickListener((ImageLongClickListener) mContext);
            mVideoStub.get().setVideoResource(mIsReceived, message);
            mVideoStub.get().setOnPlayListener(video -> mVideoListener.accept(message, video));
        }

        private void bindShareMessage(Chat message) {
            mMediaThumbnailStub.get().setVisibility(View.GONE);
            mVideoStub.get().setVisibility(View.GONE);
            mLinkPreviewStub.get().setVisibility(View.VISIBLE);
            mLinkPreviewStub.get().setOnClickListener(v -> mLinkPreviewListener.onOpenLink(message.getShare().getLink()));
            mLinkPreviewStub.get().setOnLongClickListener(v -> mLinkPreviewListener.onLinkPreviewLongClick(message));

            LinkPreviewMetadata metadata = mLinkPreviewListener.onBindLinkPreview(message.getId());

            if (metadata != null) {
                mLinkPreviewStub.get().setLinkPreview(mIsReceived, message, metadata);
            } else {
                mLinkPreviewListener.onLoadLinkPreview(message.getId(), message.getShare().getLink());
            }
        }

        private void bindFileMessage() {

        }

        private void bindCallMessage() {

        }

        private User getSenderFromChat(String uid) {
            Predicate<User> idPredicate = user -> user.getUid().equals(uid);

            return mParticipants.stream()
                                .filter(idPredicate)
                                .findFirst()
                                .orElse(sUserLeftGroup.stream()
                                                      .filter(idPredicate)
                                                      .findFirst()
                                                      .orElse(createNonUser()));
        }

        private User createNonUser() {
            return new User.Builder()
                           .setUid("123")
                           .setDisplayName(mContext.getString(R.string.dummy_user_name))
                           .create();
        }

        private void findLastSeenStatus(Chat item) {
            if (isSelf(item) && !MessageUtil.isDummyMessage(item)) {
                List<Chat> mSenderChatList = mListItem.stream()
                                                        .filter(c -> c != null &&
                                                                    c.getSenderId() != null &&
                                                                    isSelf(c) &&
                                                                    !MessageUtil.isDummyMessage(c) &&
                                                                    !c.getSeenBy().isEmpty())
                                                        .collect(Collectors.toList());

                if (mSenderChatList.contains(item)) {
                    changeSeenStatusVisibility(mSenderChatList.indexOf(item) == mSenderChatList.size() - 1);
                } else {
                    Optional<Chat> nextSeenChat = mSenderChatList.stream()
                            .filter ( c -> c.getTimestamp().compareTo(item.getTimestamp()) > 0 )
                            .findFirst();
                    changeSeenStatusVisibility(!nextSeenChat.isPresent());
                }
            }
        }

        private boolean isSelf(Chat item) {
            return mUser.getUid().equals(item.getSenderId());
        }

        private void changeSeenStatusVisibility(boolean isShow) {
            if (!mIsReceived) {
                mOutgoingBinding.imageMessageStatus.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
            }
        }

        private void showTimestamp(Chat item) {
            int      currentIndex = mListItem.indexOf(item);
            Chat     preItem      = (currentIndex - 1) < 0 ? null : mListItem.get(currentIndex - 1);
            TextView timestamp    = mIsReceived ? mReceivedBinding.textTimestamp : mOutgoingBinding.textTimestamp;

            if (preItem == null || MessageUtil.isDummyMessage(preItem)) {
                return;
            }
            if (MessageUtil.isWelcomeMessage(preItem)) {
                timestamp.setVisibility(View.VISIBLE);
                timestamp.setText(getMessageDateTimeFormatted(mContext, item.getTimestamp()));
                return;
            }
            if (shouldShowTimestamp(preItem, item)) {
                timestamp.setVisibility(View.VISIBLE);
                timestamp.setText(getMessageDateTimeFormatted(mContext, item.getTimestamp()));
            } else {
                timestamp.setVisibility(View.GONE);
            }
        }

        private boolean shouldShowTimestamp(Chat item, Chat nextItem) {
            if (MessageUtil.isDummyMessage(item) || MessageUtil.isDummyMessage(nextItem)) {
                return true;
            }

            LocalDateTime from = item.getTimestamp();
            LocalDateTime to = nextItem.getTimestamp();
            long minuteDuration = ChronoUnit.MINUTES.between(from, to);
            return minuteDuration > 10;
        }
    }
}
