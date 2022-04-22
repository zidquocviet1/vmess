package com.mqv.vmess.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ItemConversationBinding;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.ConversationGroup;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.ui.adapter.payload.ConversationNotificationPayload;
import com.mqv.vmess.ui.adapter.payload.ConversationNotificationType;
import com.mqv.vmess.ui.adapter.payload.ConversationPresencePayload;
import com.mqv.vmess.ui.adapter.payload.ConversationPresenceType;
import com.mqv.vmess.ui.data.ConversationListItem;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConversationListAdapter extends ListAdapter<Conversation, ConversationListAdapter.ConversationViewHolder> {
    private final Context mContext;
    private final User mCurrentUser;
    private BiConsumer<Integer, Boolean> conversationConsumer;
    private Consumer<Boolean> onDataSizeChanged;

    public static final String LAST_CHAT_PAYLOAD = "last_chat";
    public static final String LAST_CHAT_STATUS_PAYLOAD = "last_chat_status";
    public static final String LAST_CHAT_UNSENT_PAYLOAD = "last_chat_unsent";
    public static final String NAME_PAYLOAD = "name";
    public static final String PRESENCE_OFFLINE_PAYLOAD = "presence_offline";
    public static final String THUMBNAIL_PAYLOAD = "avatar";

    private static final int VIEW_LOAD_MORE = 0;

    public ConversationListAdapter(Context context) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                Chat oldLastChat = oldItem.getLastChat();
                Chat newLastChat = newItem.getLastChat();

                boolean isLastChatEquals = oldLastChat.getId().equals(newLastChat.getId()) &&
                        oldLastChat.isUnsent().equals(newLastChat.isUnsent()) &&
                        oldLastChat.getSeenBy().equals(newLastChat.getSeenBy()) &&
                        Objects.equals(oldLastChat.getStatus(), newLastChat.getStatus()) &&
                        Objects.equals(oldLastChat.getType(), newLastChat.getType());

                return oldItem.getId().equals(newItem.getId()) &&
                       oldItem.getParticipants().equals(newItem.getParticipants()) &&
                       oldItem.getType().equals(newItem.getType()) &&
                       ((oldItem.getGroup() == null && newItem.getGroup() == null) ||
                               (oldItem.getGroup() != null && newItem.getGroup() != null &&
                                       oldItem.getGroup().equals(newItem.getGroup()))) &&
                       oldItem.getStatus().equals(newItem.getStatus()) &&
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

                    bundle.putBoolean(NAME_PAYLOAD, !Objects.equals(oldGroupName, newGroupName));
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
        mContext = context;

        FirebaseUser user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

        mCurrentUser = new User.Builder()
                               .setUid(user.getUid())
                               .setDisplayName(user.getDisplayName())
                               .setPhotoUrl(user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString())
                               .create();
    }

    public void registerOnConversationClick(BiConsumer<Integer, Boolean> conversationConsumer) {
        this.conversationConsumer = conversationConsumer;
    }

    public void registerOnDataSizeChanged(Consumer<Boolean> listener) {
        this.onDataSizeChanged = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getId().equals("-1") ? VIEW_LOAD_MORE : 1;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(ItemConversationBinding.bind(LayoutInflater.from(mContext).inflate(R.layout.item_conversation, parent, false)),
                mCurrentUser,
                conversationConsumer);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        ConversationListItem bindableItem = holder.getBindableItem();

        if (!payloads.isEmpty() && payloads.get(0).equals(LAST_CHAT_PAYLOAD)) {
            bindableItem.bindRecentMessage(getItem(position));
        } else if (!payloads.isEmpty() && payloads.get(0) instanceof Bundle) {
            Bundle bundle = (Bundle) payloads.get(0);

            if (bundle.getBoolean(NAME_PAYLOAD, false)) {
                bindableItem.bindConversationName(getItem(position));
            } else if (bundle.getBoolean(THUMBNAIL_PAYLOAD, false)) {
                bindableItem.bindConversationThumbnail(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_PAYLOAD, false)) {
                bindableItem.bindRecentMessage(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_STATUS_PAYLOAD, false)) {
                bindableItem.bindConversationStatus(getItem(position));
            } else if (bundle.getBoolean(LAST_CHAT_UNSENT_PAYLOAD, false)) {
                bindableItem.bindUnsentMessage(getItem(position));
            }
        }

        payloads.stream()
                .filter(payload -> payload instanceof ConversationNotificationPayload)
                .map(object -> (ConversationNotificationPayload)object)
                .max((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()))
                .ifPresent(cnp -> bindableItem.bindNotificationStatus(cnp.getType() != ConversationNotificationType.ON));

        payloads.stream()
                .filter(payload -> payload instanceof ConversationPresencePayload)
                .map(object -> (ConversationPresencePayload)object)
                .max((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()))
                .ifPresent(cpp -> bindableItem.bindPresence(cpp.getType() == ConversationPresenceType.ONLINE));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_LOAD_MORE) {
            holder.getBindableItem().showLoading();
        } else {
            holder.getBindableItem().bind(getItem(position));
        }
    }

    @Override
    public void onCurrentListChanged(@NonNull List<Conversation> previousList, @NonNull List<Conversation> currentList) {
        if (onDataSizeChanged != null) {
            onDataSizeChanged.accept(currentList.isEmpty());
        }
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ConversationListItem mConversationListItem;

        public ConversationViewHolder(@NonNull ItemConversationBinding binding,
                                      User user,
                                      BiConsumer<Integer, Boolean> conversationConsumer) {
            super(binding.getRoot());

            mConversationListItem = new ConversationListItem(binding, user);

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

        public ConversationListItem getBindableItem() {
            return mConversationListItem;
        }
    }
}
