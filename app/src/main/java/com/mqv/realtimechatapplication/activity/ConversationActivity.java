package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.R.id.menu_about;
import static com.mqv.realtimechatapplication.R.id.menu_phone_call;
import static com.mqv.realtimechatapplication.R.id.menu_video_call;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.listener.OnNetworkChangedListener;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityConversationBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.ConversationGroup;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.network.model.type.MessageType;
import com.mqv.realtimechatapplication.ui.adapter.ChatListAdapter;
import com.mqv.realtimechatapplication.ui.fragment.ConversationListFragment;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.util.Picture;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConversationActivity extends BaseActivity<ConversationViewModel, ActivityConversationBinding>
        implements OnNetworkChangedListener,
                   View.OnLayoutChangeListener {
    public static final String EXTRA_CONVERSATION = "conversation";
    public static final String EXTRA_SEEN_CHAT = "is_seen_chat";
    public static final String EXTRA_NEW_CHAT_ADDED = "is_new_chat_added";
    private static final String FILE_MESSAGE_RINGTONE = "message_receive.mp3";
    private static final int NUM_ITEM_TO_SHOW_SCROLL_TO_BOTTOM = 10;
    private static final int NUM_ITEM_TO_SCROLL_FAST_THRESHOLD = 50;
    private static final int FAST_DURATION = 200;

    private List<Chat> mChatList;
    private List<User> mConversationParticipants;
    private Conversation mConversation;
    private ChatListAdapter mChatListAdapter;
    private LinearLayoutManager mLayoutManager;
    private User mCurrentUser;

    // Default color for the whole conversation
    private ColorStateList mDefaultColorStateList;
    private Animation slideUpAnimation;
    private Animation fadeAnimation;

    // Only NonNull when the conversation type NORMAL or SELF
    @Nullable
    private User mOtherUser;

    // Check whether the current conversation is updated or not
    private boolean isConversationUpdated = false;
    private boolean isNewChatAdded = false;
    private boolean isSeenChat = false;
    private boolean isLoadMore = false;

    private RecyclerView.OnScrollListener mScrollListener;
    private final RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mLayoutManager.findLastVisibleItemPosition() >= mChatListAdapter.getItemCount() - (itemCount + 1)) {
                mLayoutManager.smoothScrollToPosition(mBinding.recyclerChatList, null, mChatListAdapter.getItemCount());
            }
        }
    };

    @Override
    public void binding() {
        mBinding = ActivityConversationBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<ConversationViewModel> getViewModelClass() {
        return ConversationViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentUser = Objects.requireNonNull(LoggedInUserManager.getInstance().getLoggedInUser());
        mConversation = getIntent().getParcelableExtra(EXTRA_CONVERSATION);
        mChatList = mConversation.getChats();
        mConversationParticipants = mConversation.getParticipants();
        mDefaultColorStateList = ColorStateList.valueOf(getColor(R.color.purple_500));

        slideUpAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        fadeAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        slideUpAnimation.setDuration(FAST_DURATION);
        fadeAnimation.setDuration(FAST_DURATION);

        checkConversationType(mConversation);
        registerEventClick();
        registerNetworkEventCallback(this);
        setupColorUi();
        setupRecyclerView();
        seenChats();
    }

    @Override
    protected void onStart() {
        super.onStart();

        isUserActiveNow();
        showUserUi();
    }

    @Override
    public void finish() {
        if (isConversationUpdated) {
            Intent resultIntent = new Intent(this, ConversationListFragment.class);
            resultIntent.putExtra(EXTRA_CONVERSATION, mConversation);
            resultIntent.putExtra(EXTRA_SEEN_CHAT, isSeenChat);
            resultIntent.putExtra(EXTRA_NEW_CHAT_ADDED, isNewChatAdded);
            setResult(RESULT_OK, resultIntent);
        }

        super.finish();
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserDetail().observe(this, user -> {
            mChatListAdapter.setUserDetail(user);

            int firstItemPosition = mLayoutManager.findFirstVisibleItemPosition();
            if (firstItemPosition != -1) {
                Chat firstVisible = mChatList.get(firstItemPosition);
                if (firstVisible.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) {
                    mChatListAdapter.notifyItemChanged(0, ChatListAdapter.PROFILE_USER_PAYLOAD);
                }
            }
        });

        mViewModel.getMoreChatResult().observe(this, result -> {
            if (result == null)
                return;

            isLoadMore = result.getStatus() == NetworkStatus.LOADING;

            switch (result.getStatus()) {
                case ERROR:
                    mBinding.recyclerChatList.removeOnScrollListener(mScrollListener);
                    mChatList.remove(0);
                    mChatListAdapter.notifyItemRemoved(0);
                    mBinding.recyclerChatList.postDelayed(() -> mBinding.recyclerChatList.addOnScrollListener(mScrollListener), 500);

                    if (result.getError() != -1) Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    List<Chat> freshData = result.getSuccess();

                    mChatList.remove(0);
                    mChatList.addAll(0, freshData);
                    mChatListAdapter.notifyItemRangeInserted(0, freshData.size() - 1);
                    mChatListAdapter.notifyItemChanged(freshData.size(), ChatListAdapter.TIMESTAMP_MESSAGE_PAYLOAD);
                    break;
                case LOADING:
                    mChatList.add(0, null);
                    mChatListAdapter.notifyItemInserted(0);
                    mBinding.recyclerChatList.scrollToPosition(0);
                    break;
            }
        });

        mViewModel.getMessageObserver().observe(this, c -> {
            if (c == null) return;

            isConversationUpdated = true;
            isSeenChat = true;
            isNewChatAdded = true;

            if (mChatList.contains(c)) {
                int index = mChatList.indexOf(c);
                Chat oldItem = mChatList.get(index);
                mChatList.set(index, c);

                if (oldItem.isUnsent() != c.isUnsent()) {
                    mChatListAdapter.changeChatUnsentStatus(index);
                } else if (oldItem.getStatus() != c.getStatus()) {
                    mChatListAdapter.changeChatStatus(index);
                }
            } else {
                mChatListAdapter.addChat(c);
                postRequestSeenMessages(Collections.singletonList(c));
            }
        });

        mViewModel.getShowScrollButton().observe(this, shouldShow -> {
            if (shouldShow) {
                mBinding.buttonScrollToBottom.startAnimation(slideUpAnimation);
                mBinding.buttonScrollToBottom.setVisibility(View.VISIBLE);
            } else {
                mBinding.buttonScrollToBottom.startAnimation(fadeAnimation);
                mBinding.buttonScrollToBottom.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAvailable() {
        if (mBinding != null) {
            runOnUiThread(() -> mBinding.textNetworkError.setVisibility(View.GONE));
        }
    }

    @Override
    public void onLost() {
        if (mBinding != null) {
            runOnUiThread(() -> mBinding.textNetworkError.setVisibility(View.VISIBLE));
        }
    }

    private void checkConversationType(Conversation conversation) {
        if (conversation.getId().startsWith("NEW_CONVERSATION")) {
            // The new conversation when user request a new message but have not friend relationship
        } else {
            List<User> participants = conversation.getParticipants();

            if (conversation.getType() == ConversationType.SELF) {
                mOtherUser = participants.get(0);
            } else if (conversation.getType() == ConversationType.NORMAL) {
                participants.stream()
                        .filter(u -> !u.getUid().equals(mCurrentUser.getUid()))
                        .findFirst()
                        .ifPresent(u2 -> {
                            mOtherUser = u2;
                            mViewModel.loadUserDetail(u2.getUid());
                        });
            }
        }
    }

    private void registerEventClick() {
        mBinding.buttonBack.setOnClickListener(v -> onBackPressed());
        mBinding.layoutTitle.setOnClickListener(v -> Logging.show("Open User Detail fragment"));
        mBinding.toolbar.setOnMenuItemClickListener(item -> {
            var itemId = item.getItemId();

            if (itemId == menu_phone_call) {

                Logging.show("Start VoIP phone call");
                return true;
            } else if (itemId == menu_video_call) {
                Logging.show("Start WebRTC video call");

                return true;
            } else if (itemId == menu_about) {
                Logging.show("Open Conversation Details Activity");

                return true;
            }
            return false;
        });
        mBinding.buttonSendMessage.setOnClickListener(v -> {
            String senderId = mCurrentUser.getUid();
            String content = mBinding.editTextContent.getText().toString();
            String conversationId = mConversation.getId();

            mBinding.editTextContent.setText("");

            Chat chat = new Chat(UUID.randomUUID().toString(), senderId, content, conversationId, MessageType.GENERIC);
            addNewChatToAdapter(chat);
            mViewModel.sendMessage(this, getApplicationContext(), chat);
        });
        mBinding.buttonMore.setOnClickListener(v -> {
        });
        mBinding.buttonCamera.setOnClickListener(v -> {
        });
        mBinding.buttonGallery.setOnClickListener(v -> {
        });
        mBinding.buttonMic.setOnClickListener(v -> {
        });
        mBinding.editTextContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    mBinding.buttonMore.setVisibility(View.VISIBLE);
                    mBinding.buttonSendMessage.setVisibility(View.GONE);
                } else {
                    mBinding.buttonMore.setVisibility(View.GONE);
                    mBinding.buttonSendMessage.setVisibility(View.VISIBLE);
                }
            }
        });
        mBinding.buttonScrollToBottom.setOnClickListener(v -> {
            LinearLayoutManager llm = (LinearLayoutManager) mBinding.recyclerChatList.getLayoutManager();

            if (llm == null) return;

            int position = llm.findLastVisibleItemPosition();

            if (mChatList.size() - position >= NUM_ITEM_TO_SCROLL_FAST_THRESHOLD) {
                mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1);
            } else {
                mBinding.recyclerChatList.smoothScrollToPosition(mChatList.size() - 1);
            }
        });
    }

    private void addNewChatToAdapter(Chat chat) {
        mChatListAdapter.addChat(chat);

        if (mBinding != null)
            mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1);
    }

    private void setupColorUi() {
        var toolbarMenu = mBinding.toolbar.getMenu();
        var menuItemSize = toolbarMenu.size();

        for (int i = 0; i < menuItemSize; i++) {
            var menuItem = toolbarMenu.getItem(i);
            menuItem.setIconTintList(mDefaultColorStateList);
        }
        mBinding.buttonBack.setIconTint(mDefaultColorStateList);
        mBinding.buttonCamera.setIconTint(mDefaultColorStateList);
        mBinding.buttonGallery.setIconTint(mDefaultColorStateList);
        mBinding.buttonMic.setIconTint(mDefaultColorStateList);
        mBinding.buttonSendMessage.setIconTint(mDefaultColorStateList);
        mBinding.buttonMore.setIconTint(mDefaultColorStateList);
        mBinding.buttonScrollToBottom.setIconTint(mDefaultColorStateList);
    }

    private void setupRecyclerView() {
        mChatListAdapter = new ChatListAdapter(this,
                mChatList,
                mConversationParticipants,
                mDefaultColorStateList,
                mCurrentUser,
                mOtherUser);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        mBinding.recyclerChatList.setItemAnimator(null); // Remove DefaultItemAnimator
        mBinding.recyclerChatList.addItemDecoration(new CustomItemDecoration(this, mChatList, mCurrentUser));
        mBinding.recyclerChatList.setAdapter(mChatListAdapter);
        mBinding.recyclerChatList.setHasFixedSize(true);
        mBinding.recyclerChatList.setLayoutManager(mLayoutManager);
        mBinding.recyclerChatList.addOnLayoutChangeListener(this);
        ChatListAdapter.initializePool(mBinding.recyclerChatList.getRecycledViewPool());

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (mChatList.size() <= 2) {
                    return;
                }
                if (!recyclerView.canScrollVertically(-1)) {
                    onLoadMore();
                }

                int position = mLayoutManager.findLastVisibleItemPosition();
                mViewModel.setScrollButtonState(position < mChatList.size() - NUM_ITEM_TO_SHOW_SCROLL_TO_BOTTOM);
            }
        };
        mBinding.recyclerChatList.addOnScrollListener(mScrollListener);

        mChatListAdapter.submitList(mChatList);
        mChatListAdapter.registerAdapterDataObserver(mAdapterObserver);

        mBinding.recyclerChatList
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        /*
                        * Check the first dummy Profile chat view type
                        * If the layout can show the view completely then make the layout manager stackFromEnd = false
                        * Otherwise true
                        * */
                        int firstItem = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                        if (firstItem != -1) {
                            Chat chat = mChatList.get(firstItem);
                            mLayoutManager.setStackFromEnd(!chat.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX));
                        }
                        // At this point the layout is complete and the
                        // dimensions of recyclerView and any child views
                        // are known.
                        mBinding.recyclerChatList
                                .getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                    }
                });
    }

    private void onLoadMore() {
        int index = mLayoutManager.findFirstVisibleItemPosition();
        Chat headerChatItem = mChatList.get(index);

        if (headerChatItem != null && !headerChatItem.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX) && getNetworkStatus()) {
            if (!isLoadMore) {
                mViewModel.registerLoadMore(mConversation);
            }
        }
    }

    private void seenChats() {
        // TODO: add seen chat to temp chat [SEEN_CHAT] when user offline or server not response
        if (mChatList.size() == 2) {
            Chat welcomeChat = mChatList.get(1);

            if (!welcomeChat.getSeenBy().contains(mCurrentUser.getUid())) {
                welcomeChat.getSeenBy().add(mCurrentUser.getUid());

                mViewModel.seenWelcomeMessage(welcomeChat);

                isConversationUpdated = true;
                isSeenChat = true;
            }
        } else {
            postRequestSeenMessages(mChatList);
        }
    }

    private void isUserActiveNow() {
        mBinding.toolbarSubtitle.setVisibility(View.GONE);
    }

    private void showUserUi() {
        switch (mConversation.getType()) {
            case SELF:
                setToolbarTitle(getString(R.string.title_just_you));
                loadImage(mCurrentUser.getPhotoUrl());
                break;
            case GROUP:
                ConversationGroup group = mConversation.getGroup();

                setToolbarTitle(group.getName());
                loadImage(group.getThumbnail());
                break;
            case NORMAL:
                setToolbarTitle(Objects.requireNonNull(mOtherUser).getDisplayName());
                loadImage(mOtherUser.getPhotoUrl());
                break;
        }
    }

    private void loadImage(@Nullable String url) {
        Picture.loadUserAvatar(this, url).into(mBinding.imageAvatar);
    }

    private void setToolbarTitle(String title) {
        mBinding.toolbarTitle.setText(title);
    }

    private void postRequestSeenMessages(List<Chat> messages) {
        List<Chat> seenChatsRequest = messages.stream()
                                              .filter(c -> c.getSenderId() != null &&
                                                      !c.getSenderId().equals(mCurrentUser.getUid()) &&
                                                      !c.getSeenBy().contains(mCurrentUser.getUid()))
                                              .peek(c -> c.getSeenBy().add(mCurrentUser.getUid()))
                                              .collect(Collectors.toList());

        if (!seenChatsRequest.isEmpty()) {
            mViewModel.seenMessage(seenChatsRequest);
        }
    }

    public Conversation currentConversation() {
        return mConversation;
    }

    @Override
    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        int     lastItem        = mLayoutManager.findLastCompletelyVisibleItemPosition();
        boolean wasAtBottom     = lastItem >= mChatList.size() - 1;
        boolean isSoftInputShow = WindowInsetsCompat.toWindowInsetsCompat(mBinding.editTextContent.getRootWindowInsets())
                .isVisible(WindowInsetsCompat.Type.ime());

        if (!mChatList.isEmpty() && !isLoadMore && isSoftInputShow && !wasAtBottom) {
            mBinding.recyclerChatList.post(() -> mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1));
        }
    }

    static class CustomItemDecoration extends RecyclerView.ItemDecoration {
        private static final int BOUND_DURATION_TIME = 10;

        private final List<Chat> mChatList;
        private final User mCurrentUser;
        private final int mChatMargin;
        private final int mChatCornerRadius;
        private final int mChatCornerRadiusSmall;

        public CustomItemDecoration(Context context, List<Chat> chatList, User currentUser) {
            mChatList = chatList;
            mCurrentUser = currentUser;
            mChatCornerRadius = context.getResources().getDimensionPixelSize(R.dimen.chat_corner_radius);
            mChatCornerRadiusSmall = context.getResources().getDimensionPixelSize(R.dimen.chat_corner_radius_normal);
            mChatMargin = context.getResources().getDimensionPixelSize(R.dimen.chat_margin);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int prePosition = position - 1;
            int nextPosition = position + 1;

            /*
             * Get a chain of item include:
             * {preItem}: Nullable
             * {item}: NonNull
             * {nextItem}: Nullable
             * */
            Chat item = position >= 0 ? mChatList.get(position) : null;
            Chat preItem = prePosition >= 0 ? mChatList.get(prePosition) : null;
            Chat nextItem = nextPosition < mChatList.size() ? mChatList.get(nextPosition) : null;

            if (item == null)
                return;

            RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);
            /*
             * Check if the preItem is the dummy chat or not.
             * If the preItem is the dummy chat so the current item will show normally.
             * */
            if (preItem == null ||
                    preItem.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX) ||
                    preItem.getId().startsWith(Const.WELCOME_CHAT_PREFIX)) {
                if (viewHolder instanceof ChatListAdapter.ChatListViewHolder) {
                    String senderId = item.getSenderId();

                    if (senderId == null) return;

                    if (nextItem != null && !shouldShowTimestamp(item, nextItem) && senderId.equals(nextItem.getSenderId())) {
                        reformatCornerRadius((ChatListAdapter.ChatListViewHolder) viewHolder,
                                item,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadiusSmall);
                        if (shouldShowTimestamp(item, nextItem)) {
                            ((ChatListAdapter.ChatListViewHolder) viewHolder).showIconReceiver();
                        } else {
                            ((ChatListAdapter.ChatListViewHolder) viewHolder).hiddenIconReceiver();
                        }
                    } else {
                        reformatCornerRadius((ChatListAdapter.ChatListViewHolder) viewHolder,
                                item,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius);
                    }
                }
                return;
            }

            /*
             * Add margin between two different sender chat item.
             * */
            if (!shouldShowTimestamp(preItem, item)) {
                if (!item.getSenderId().equals(preItem.getSenderId())) {
                    outRect.top = mChatMargin;
                }
            }

            /*
             * Section to render bunch of sender chat item.
             * */
            if (viewHolder instanceof ChatListAdapter.ChatListViewHolder) {
                ChatListAdapter.ChatListViewHolder chatViewHolder = (ChatListAdapter.ChatListViewHolder) viewHolder;

                if (isReceiveMoreThanTwo(item, nextItem)) {
                    /*
                     * Don't check the nextItem is null or not. Because isReceiveMoreThanTwo method did that.
                     * */
                    if (shouldShowTimestamp(item, nextItem)) {
                        chatViewHolder.showIconReceiver();
                    } else {
                        chatViewHolder.hiddenIconReceiver();
                    }
                } else {
                    chatViewHolder.showIconReceiver();
                }

                renderBunchOfChats(chatViewHolder, preItem, item, nextItem);
                findLastSeenStatus(chatViewHolder, item);
            }
        }

        /*
         * Check the Current Chat vs Next Item is own by one user or not
         * */
        private boolean isReceiveMoreThanTwo(Chat item, Chat nextItem) {
            if (nextItem == null)
                return false;

            String itemSenderId = item.getSenderId();
            String nextItemSenderId = nextItem.getSenderId();
            String currentUserId = mCurrentUser.getUid();

            return itemSenderId.equals(nextItemSenderId) && !itemSenderId.equals(currentUserId);
        }

        /*
         * The method to check the duration time of two chat in a row larger than 10 minutes or not.
         * */
        private boolean shouldShowTimestamp(Chat item, Chat nextItem) {
            LocalDateTime from = item.getTimestamp();
            LocalDateTime to = nextItem.getTimestamp();

            long minuteDuration = ChronoUnit.MINUTES.between(from, to);

            return minuteDuration > BOUND_DURATION_TIME;
        }

        /*
         * Render the bunch of chats with the dynamic corner radius of background
         * */
        private void renderBunchOfChats(ChatListAdapter.ChatListViewHolder vh,
                                        Chat prev,
                                        Chat cur,
                                        Chat next) {
            String preSenderId = prev.getSenderId();
            String curSenderId = cur.getSenderId();

            if (!preSenderId.equals(curSenderId)) {
                if (next != null && curSenderId.equals(next.getSenderId()) && !shouldShowTimestamp(cur, next)) {
                    reformatCornerRadius(vh, cur,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadiusSmall);
                } else {
                    reformatCornerRadius(vh, cur,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius);
                }
            } else {
                if (!shouldShowTimestamp(prev, cur)) {
                    if (next != null) {
                        if (shouldShowTimestamp(cur, next) || !curSenderId.equals(next.getSenderId())) {
                            reformatCornerRadius(vh, cur,
                                    mChatCornerRadiusSmall,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadius);
                        } else {
                            reformatCornerRadius(vh, cur,
                                    mChatCornerRadiusSmall,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadiusSmall);
                        }
                    } else {
                        reformatCornerRadius(vh, cur,
                                mChatCornerRadiusSmall,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius);
                    }
                } else {
                    if (next != null) {
                        if (shouldShowTimestamp(prev, cur) && shouldShowTimestamp(cur, next)) {
                            reformatCornerRadius(vh, cur,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadius);
                            return;
                        }

                        if (!curSenderId.equals(next.getSenderId())) {
                            reformatCornerRadius(vh, cur,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadius,
                                    mChatCornerRadius);
                        } else {
                            if (shouldShowTimestamp(prev, cur)) {
                                reformatCornerRadius(vh, cur,
                                        mChatCornerRadius,
                                        mChatCornerRadius,
                                        mChatCornerRadius,
                                        mChatCornerRadiusSmall);
                            } else {
                                reformatCornerRadius(vh, cur,
                                        mChatCornerRadiusSmall,
                                        mChatCornerRadius,
                                        mChatCornerRadius,
                                        mChatCornerRadiusSmall);
                            }
                        }
                    } else {
                        reformatCornerRadius(vh, cur,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius);
                    }
                }
            }
        }

        /*
         * Set the corner radius of given drawable programmatically
         * */
        private void reformatCornerRadius(ChatListAdapter.ChatListViewHolder vh,
                                          Chat item,
                                          int topLeft,
                                          int topRight,
                                          int bottomRight,
                                          int bottomLeft) {
            if (item.getSenderId() == null)
                return;

            View v = vh.getBackground(item);

            GradientDrawable drawable = (GradientDrawable) v.getBackground();

            boolean isChatFromSender = item.getSenderId().equals(mCurrentUser.getUid());

            if (isChatFromSender) {
                topLeft = topLeft ^ topRight;
                topRight = topLeft ^ topRight;
                topLeft = topLeft ^ topRight;

                bottomLeft = bottomLeft ^ bottomRight;
                bottomRight = bottomLeft ^ bottomRight;
                bottomLeft = bottomLeft ^ bottomRight;
            }

            drawable.setCornerRadii(new float[]{
                    topLeft, topLeft,
                    topRight, topRight,
                    bottomRight, bottomRight,
                    bottomLeft, bottomLeft
            });

            v.setBackground(drawable);
        }

        private void findLastSeenStatus(ChatListAdapter.ChatListViewHolder vh, Chat item) {
            List<Chat> mSenderChatList = mChatList.stream()
                                                  .filter(c -> c != null &&
                                                          c.getSenderId() != null &&
                                                          isSelf(c) &&
                                                          hasSeen(c))
                                                  .collect(Collectors.toList());

            if (isSelf(item)) {
                if (mSenderChatList.contains(item)) {
                    vh.changeSeenStatusVisibility(mSenderChatList.indexOf(item) == mSenderChatList.size() - 1);
                } else {
                    if (hasSeen(item)) {
                        mSenderChatList.add(item);
                    }
                    vh.changeSeenStatusVisibility(true);
                }
            }
        }

        private boolean isSelf(Chat item) {
            return item.getSenderId().equals(mCurrentUser.getUid());
        }

        private boolean hasSeen(Chat item) {
            return !item.getSeenBy().isEmpty();
        }
    }
}