package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.R.id.menu_about;
import static com.mqv.realtimechatapplication.R.id.menu_phone_call;
import static com.mqv.realtimechatapplication.R.id.menu_video_call;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.listener.OnNetworkChangedListener;
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityConversationBinding;
import com.mqv.realtimechatapplication.databinding.ItemImageGroupBinding;
import com.mqv.realtimechatapplication.databinding.ItemUserAvatarBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;
import com.mqv.realtimechatapplication.network.model.type.MessageType;
import com.mqv.realtimechatapplication.ui.adapter.ChatListAdapter;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.util.Picture;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConversationActivity
       extends BaseActivity<ConversationViewModel, ActivityConversationBinding>
       implements OnNetworkChangedListener,
                  View.OnLayoutChangeListener,
                  ViewStub.OnInflateListener,
                  ChatListAdapter.ConversationGroupOption {
    public static final String EXTRA_CONVERSATION = "conversation";
    private static final int NUM_ITEM_TO_SHOW_SCROLL_TO_BOTTOM = 10;
    private static final int NUM_ITEM_TO_SCROLL_FAST_THRESHOLD = 50;
    private static final int FAST_DURATION = 200;

    private List<Chat> mChatList;
    private List<User> mConversationParticipants;
    private Conversation mConversation;
    private ChatListAdapter mChatListAdapter;
    private CustomLinearLayoutManager mLayoutManager;
    private User mCurrentUser;

    // Default color for the whole conversation
    private ColorStateList mDefaultColorStateList;
    private Animation slideUpAnimation;
    private Animation fadeAnimation;

    private boolean isLoadMore = false;
    private boolean isActive = false;

    private RecyclerView.OnScrollListener mScrollListener;
    private final RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mLayoutManager.findLastVisibleItemPosition() >= mChatListAdapter.getItemCount() - (itemCount + 1)) {
                mBinding.recyclerChatList.smoothScrollToPosition(mChatList.size() - 1);
            }
        }
    };

    private ItemUserAvatarBinding avatarNormalStubBinding;
    private ItemImageGroupBinding avatarGroupStubBinding;

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
        mConversationParticipants = mConversation.getParticipants();
        mDefaultColorStateList = ColorStateList.valueOf(getColor(R.color.purple_500));

        slideUpAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        fadeAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        slideUpAnimation.setDuration(FAST_DURATION);
        fadeAnimation.setDuration(FAST_DURATION);

        setupConversation(mConversation);
        registerEventClick();
        registerNetworkEventCallback(this);
        setupColorUi();
        setupRecyclerView();
        postRequestSeenMessages();
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

                    if (result.getError() != -1)
                        Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
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

            if (mChatList.contains(c)) {
                int index = mChatList.indexOf(c);
                Chat oldItem = mChatList.get(index);
                mChatList.set(index, c);

                if (oldItem.isUnsent() != c.isUnsent()) {
                    mChatListAdapter.changeChatUnsentStatus(index);
                } else if (oldItem.getStatus() != c.getStatus()) {
                    mChatListAdapter.changeChatStatus(index);
                    mChatListAdapter.notifyItemRangeChanged(0, mChatList.subList(0, index).size(), ChatListAdapter.MESSAGE_STATUS_PAYLOAD);
                }
            } else {
                mChatListAdapter.addChat(c);
                postRequestSeenMessages();
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

        mViewModel.getConversationActiveStatus().observe(this, isOnline -> {
            isActive = isOnline;
            mBinding.toolbarSubtitle.setVisibility(isOnline ? View.VISIBLE : View.GONE);
            if (avatarNormalStubBinding != null) {
                avatarNormalStubBinding.imageActive.setVisibility(isOnline ? View.VISIBLE : View.GONE);
            }
            if (avatarGroupStubBinding != null) {
                avatarGroupStubBinding.imageActive.setVisibility(isOnline ? View.VISIBLE : View.GONE);
            }
        });

        // First load conversation cache messages if not load the messages from server
        mViewModel.getCacheChats().observe(this, list -> {
            if (!list.isEmpty()) {
                mChatList.addAll(list);
                mChatListAdapter.notifyItemRangeInserted(0, list.size());

                onFirstLoadComplete();
            }
        });

        mViewModel.getConversationMetadata().observe(this, metadata -> {
            mChatListAdapter.setConversationMetadata(metadata);

            setToolbarTitle(metadata.getConversationName());

            List<String> conversationThumbnail = metadata.getConversationThumbnail();

            if (conversationThumbnail.size() > 1) {
                loadImage(conversationThumbnail.get(0), avatarGroupStubBinding.avatarUser1);
                loadImage(conversationThumbnail.get(1), avatarGroupStubBinding.avatarUser2);
            } else {
                loadImage(conversationThumbnail.get(0), avatarNormalStubBinding.imageAvatar);
            }

            if (metadata.getType() == ConversationType.NORMAL) {
                mViewModel.loadUserDetail(metadata.getOtherUid());
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

    private void setupConversation(Conversation conversation) {
        mBinding.viewStubImageGroup.setOnInflateListener(this);
        mBinding.viewStubImageAvatar.setOnInflateListener(this);

        if (conversation.getId().startsWith("-1")) {
            // The new conversation when user request a new message but have not friend relationship
        } else {
            if (conversation.getType() == ConversationType.SELF || conversation.getType() == ConversationType.NORMAL) {
                mBinding.viewStubImageAvatar.inflate();
            } else {
                mBinding.viewStubImageGroup.inflate();
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

            mBinding.editTextContent.getText().clear();

            Chat chat = new Chat(UUID.randomUUID().toString(), senderId, content, conversationId, MessageType.GENERIC);
            mViewModel.sendMessage(this, chat);
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
                if (s.length() == 0 || TextUtils.isEmpty(s.toString().replace(" ", ""))) {
                    mBinding.buttonMore.setVisibility(View.VISIBLE);
                    mBinding.buttonSendMessage.setVisibility(View.GONE);
                } else {
                    mBinding.buttonMore.setVisibility(View.GONE);
                    mBinding.buttonSendMessage.setVisibility(View.VISIBLE);
                }
            }
        });
        mBinding.buttonScrollToBottom.setOnClickListener(v -> {
            int position = mLayoutManager.findLastVisibleItemPosition();

            if (mChatList.size() - position >= NUM_ITEM_TO_SCROLL_FAST_THRESHOLD) {
                mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1);
            } else {
                mLayoutManager.smoothScrollToPosition(mBinding.recyclerChatList, false, mChatList.size() - 1);
            }
        });
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
        mChatList        = new ArrayList<>();
        mChatListAdapter = new ChatListAdapter(this,
                mChatList,
                mConversationParticipants,
                mDefaultColorStateList,
                mCurrentUser,
                mConversation.getType());

        mLayoutManager = new CustomLinearLayoutManager(this);
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        mBinding.recyclerChatList.setItemAnimator(null);
        mBinding.recyclerChatList.addItemDecoration(new CustomItemDecoration(this, mChatList));
        mBinding.recyclerChatList.setAdapter(mChatListAdapter);
        mBinding.recyclerChatList.setHasFixedSize(true);
        mBinding.recyclerChatList.setLayoutManager(mLayoutManager);
        mBinding.recyclerChatList.addOnLayoutChangeListener(this);
        ChatListAdapter.initializePool(mBinding.recyclerChatList.getRecycledViewPool());

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                shouldShowHeader();

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
        mChatListAdapter.registerConversationOption(this);
    }

    private void onFirstLoadComplete() {
        seenWelcomeChat();
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

    private void seenWelcomeChat() {
        if (mChatList.size() == 2) {
            Chat welcomeChat = mChatList.get(1);

            if (!welcomeChat.getSeenBy().contains(mCurrentUser.getUid())) {
                welcomeChat.getSeenBy().add(mCurrentUser.getUid());

                mViewModel.seenWelcomeMessage(welcomeChat);
            }
        }
    }

    private void loadImage(@Nullable String url, ImageView container) {
        Picture.loadUserAvatar(this, url).into(container);
    }

    private void setToolbarTitle(String title) {
        mBinding.toolbarTitle.setText(title);
    }

    private void postRequestSeenMessages() {
        mViewModel.postSeenMessageConversation(this, mConversation.getId(), mCurrentUser.getUid());
    }

    private void shouldShowHeader() {
        int firstItemPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (firstItemPosition != -1) {
            Chat firstVisible = mChatList.get(firstItemPosition);
            boolean shouldShowHeader = !(firstVisible != null && firstVisible.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX));
            boolean isGroup = mConversation.getGroup() != null;

            if (isGroup) {
                mBinding.viewStubImageGroup.setVisibility(shouldShowHeader ? View.VISIBLE : View.INVISIBLE);
            } else {
                mBinding.viewStubImageAvatar.setVisibility(shouldShowHeader ? View.VISIBLE : View.INVISIBLE);
            }
            mBinding.toolbarSubtitle.setVisibility(shouldShowHeader && isActive ? View.VISIBLE : View.GONE);
            mBinding.toolbarTitle.setVisibility(shouldShowHeader ? View.VISIBLE : View.INVISIBLE);
        }
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

    @Override
    public void onInflate(ViewStub viewStub, View view) {
        int id = viewStub.getInflatedId();

        if (id == R.id.inflated_image_normal) {
            avatarNormalStubBinding = ItemUserAvatarBinding.bind(view);
        } else if (id == R.id.inflated_image_group) {
            avatarGroupStubBinding = ItemImageGroupBinding.bind(view);
        }
    }

    @Override
    public void addMember() {
        Logging.show("Add new member to this group");
    }

    @Override
    public void changeGroupName() {
        Logging.show("Change the name of this group");
    }

    @Override
    public void viewGroupMember() {
        Logging.show("View all group members");
    }

    private static class CustomLinearLayoutManager extends LinearLayoutManager {
        private static final float CUSTOM_SCROLL_DURATION = 300f;
        private static final float DEFAULT_SCROLL_DURATION = 25f;

        public CustomLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
            smoothScrollToPosition(recyclerView, true, position);
        }

        // This method is used by scroll to bottom button
        public void smoothScrollToPosition(RecyclerView recyclerView, boolean isInserted, int position) {
            final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return (isInserted ? CUSTOM_SCROLL_DURATION : DEFAULT_SCROLL_DURATION) / displayMetrics.densityDpi;
                }
            };

            linearSmoothScroller.setTargetPosition(position);
            startSmoothScroll(linearSmoothScroller);
        }
    }

    private static class CustomItemDecoration extends RecyclerView.ItemDecoration {
        private static final int BOUND_DURATION_TIME = 10;

        private final List<Chat> mChatList;
        private final int mChatMargin;

        public CustomItemDecoration(Context context, List<Chat> chatList) {
            mChatList = chatList;
            mChatMargin = context.getResources().getDimensionPixelSize(R.dimen.chat_margin);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int prePosition = position - 1;

            /*
             * Get a chain of item include:
             * {preItem}: Nullable
             * {item}: NonNull
             * {nextItem}: Nullable
             * */
            Chat item = position >= 0 ? mChatList.get(position) : null;
            Chat preItem = prePosition >= 0 ? mChatList.get(prePosition) : null;

            if (item == null)
                return;

            /*
             * Check if the preItem is the dummy chat or not.
             * If the preItem is the dummy chat so the current item will show normally.
             * */
            if (preItem == null ||
                preItem.getId().startsWith(Const.DUMMY_FIRST_CHAT_PREFIX) ||
                preItem.getId().startsWith(Const.WELCOME_CHAT_PREFIX)) {
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
    }
}