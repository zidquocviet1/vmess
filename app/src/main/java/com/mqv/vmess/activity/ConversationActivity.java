package com.mqv.vmess.activity;

import static com.mqv.vmess.R.id.menu_about;
import static com.mqv.vmess.R.id.menu_block;
import static com.mqv.vmess.R.id.menu_phone_call;
import static com.mqv.vmess.R.id.menu_send_message;
import static com.mqv.vmess.R.id.menu_video_call;
import static com.mqv.vmess.R.id.menu_view_profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.br.MarkNotificationReadReceiver;
import com.mqv.vmess.activity.listener.OnNetworkChangedListener;
import com.mqv.vmess.activity.viewmodel.ConversationViewModel;
import com.mqv.vmess.data.model.LocalPlaintextContentModel;
import com.mqv.vmess.databinding.ActivityConversationBinding;
import com.mqv.vmess.databinding.DialogEnterOtpCodeBinding;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationType;
import com.mqv.vmess.network.model.type.MessageMediaUploadType;
import com.mqv.vmess.network.model.type.MessageStatus;
import com.mqv.vmess.ui.ConversationOptionHandler;
import com.mqv.vmess.ui.adapter.BaseAdapter;
import com.mqv.vmess.ui.adapter.ChatListAdapter;
import com.mqv.vmess.ui.components.ImageClickListener;
import com.mqv.vmess.ui.components.ImageLongClickListener;
import com.mqv.vmess.ui.components.conversation.ConversationMultiMediaFooter;
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewListener;
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata;
import com.mqv.vmess.ui.data.ImageThumbnail;
import com.mqv.vmess.ui.data.Media;
import com.mqv.vmess.ui.data.UserSelection;
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment;
import com.mqv.vmess.ui.permissions.Permission;
import com.mqv.vmess.util.AlertDialogUtil;
import com.mqv.vmess.util.FileProviderUtil;
import com.mqv.vmess.util.MediaUtil;
import com.mqv.vmess.util.MessageUtil;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Picture;
import com.mqv.vmess.util.ServiceUtil;
import com.mqv.vmess.util.views.Stub;
import com.mqv.vmess.work.DownloadMediaWorkWrapper;
import com.mqv.vmess.work.WorkDependency;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConversationActivity
       extends BaseActivity<ConversationViewModel, ActivityConversationBinding>
       implements OnNetworkChangedListener,
                  ChatListAdapter.ConversationGroupOption,
                  BaseAdapter.ItemEventHandler,
                  ImageClickListener,
                  ImageLongClickListener,
                  LinkPreviewListener,
                  ConversationMultiMediaFooter.Callback {
    public static final String ACTION_SEARCH_MESSAGE = "search_message";
    public static final String ACTION_ADD_MEMBER = "add_member";
    public static final String ACTION_REMOVE_GROUP_MEMBER = "remove_group_member";
    public static final String ACTION_LEAVE_GROUP = "leave_group";
    public static final String EXTRA_REMOVE_MEMBER_ID = "remove_member_id";
    public static final String EXTRA_MEMBER_TO_ADD = "member_to_add";
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_PARTICIPANT_ID = "participant_id";

    private static final int NUM_ITEM_TO_SHOW_SCROLL_TO_BOTTOM = 10;
    private static final int NUM_ITEM_TO_SCROLL_FAST_THRESHOLD = 50;
    private static final int FAST_DURATION = 200;

    private final ConversationOptionHandler conversationOptionHandler = new ConversationOptionHandler(this);
    private List<Chat> mChatList;
    private Conversation mConversation;
    private ChatListAdapter mChatListAdapter;
    private CustomLinearLayoutManager mLayoutManager;
    private Stub<ConversationMultiMediaFooter> mMediaFooterStub;
    private FirebaseUser mCurrentUser;

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
        AppDependencies.getIncomingMessageObserver();

        super.onCreate(savedInstanceState);

        mCurrentUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());
        mMediaFooterStub = new Stub<>(mBinding.stubMediaFooter);

        slideUpAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        fadeAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        slideUpAnimation.setDuration(FAST_DURATION);
        fadeAnimation.setDuration(FAST_DURATION);

        registerEventClick();
        registerNetworkEventCallback(this);
        triggerIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register window insets listener to handle keyboard show/hidden
        WindowCompat.setDecorFitsSystemWindows(getWindow(), Build.VERSION.SDK_INT < Build.VERSION_CODES.R);
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.footer.getRootView(), (v, insets) -> {
            checkForScrollToBottomWhenOpenKeyboard(insets.isVisible(WindowInsetsCompat.Type.ime()));
            return insets;
        });
    }

    @Override
    public void onBackPressed() {
        if (mMediaFooterStub.get().isShowing()) {
            mMediaFooterStub.get().hide();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setupObserver() {
        mViewModel.getUserDetail().observe(this, user -> {
            mChatListAdapter.setUserDetail(user);

            int firstItemPosition = mLayoutManager.findFirstVisibleItemPosition();
            if (firstItemPosition != -1) {
                Chat firstVisible = mChatList.get(firstItemPosition);
                if (MessageUtil.isDummyProfileMessage(firstVisible)) {
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
                mViewModel.setNewMessageState(c.getSenderId() != null && !c.getSenderId().equals(mCurrentUser.getUid()));
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

        mViewModel.getNewMessageState().observe(this, shouldShow -> {
            if (mLayoutManager != null) {
                int position = mLayoutManager.findLastCompletelyVisibleItemPosition();
                boolean finallyShow = shouldShow && (position < mChatListAdapter.getCurrentList().size() - 2);

                if (finallyShow) {
                    mBinding.textNewMessage.startAnimation(slideUpAnimation);
                    mBinding.textNewMessage.setVisibility(View.VISIBLE);
                } else {
                    mBinding.textNewMessage.startAnimation(fadeAnimation);
                    mBinding.textNewMessage.setVisibility(View.GONE);
                }
            }
        });

        mViewModel.getConversationActiveStatus().observe(this, isOnline -> {
            isActive = isOnline;
            mBinding.conversationThumbnail.setActiveStatus((isOnline && shouldShowHeader()));
            mBinding.toolbarSubtitle.setVisibility((isOnline && shouldShowHeader()) ? View.VISIBLE : View.GONE);
        });

        mViewModel.getConversationMetadata().observe(this, metadata -> {
            mChatListAdapter.setConversationMetadata(metadata);
            mChatListAdapter.setParticipants(metadata.getConversationParticipants());
            mChatListAdapter.notifyItemChanged(0);
            mConversation.setParticipants(metadata.getConversationParticipants());

            setToolbarTitle(metadata.getConversationName());

            List<String> conversationThumbnail = metadata.getConversationThumbnail();

            mBinding.conversationThumbnail.setThumbnail(conversationThumbnail);

            if (metadata.getType() == ConversationType.NORMAL) {
                mViewModel.loadUserDetail(metadata.getOtherUid());
            }
        });

        mViewModel.getConversationObserver().observe(this, result -> {
            if (result.getStatus() == NetworkStatus.SUCCESS) {
                mConversation = result.getSuccess();

                setupRecyclerView(mConversation.getParticipants(), mConversation.getType());

                mChatList.addAll(mConversation.getChats());
                mChatListAdapter.notifyItemRangeInserted(0, mConversation.getChats().size());
                mChatListAdapter.registerAdapterDataObserver(mAdapterObserver);

                seenWelcomeChat();
            } else if (result.getStatus() == NetworkStatus.ERROR) {
                Toast.makeText(this, result.getError(), Toast.LENGTH_SHORT).show();
                finish();
            }

            mBinding.textLoading.setVisibility(result.getStatus() == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);
            mBinding.recyclerChatList.setVisibility(result.getStatus() == NetworkStatus.LOADING ? View.GONE : View.VISIBLE);
        });

        mViewModel.getEventToast().observe(this, event -> {
            Integer value = event.getContentIfNotHandled();

            if (value != null) {
                Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
            }
        });

        mViewModel.getSingleRequestCall().observe(this, result -> {
            if (result != null) {
                if (result.getStatus() == NetworkStatus.LOADING) {
                    AlertDialogUtil.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
                } else if (result.getStatus() == NetworkStatus.SUCCESS || result.getStatus() == NetworkStatus.ERROR) {
                    AlertDialogUtil.finishLoadingDialog();
                } else if (result.getStatus() == NetworkStatus.TERMINATE) {
                    finish();
                }
            }
        });

        mViewModel.getLinkPreviewMapper().observe(this, mapper ->
                mapper.entrySet()
                      .stream()
                      .filter(entry -> !entry.getValue().isLoadComplete() || !entry.getValue().isNoPreview())
                      .mapToInt(e -> mChatListAdapter.getCurrentList()
                                                     .stream()
                                                     .filter(c ->c != null && c.getId().equals(e.getKey()))
                                                     .findFirst()
                                                     .map(c -> mChatListAdapter.getCurrentList().indexOf(c))
                                                     .orElse(-1))
                      .filter(i -> i >= 0)
                      .forEach(index -> mChatListAdapter.notifyItemChanged(index)));

        mViewModel.getUserLeftGroup().observe(this, user -> {
            if (mChatListAdapter != null) {
                mChatListAdapter.setUserLeftGroup(user);
                mChatListAdapter.notifyItemRangeChanged(0, mChatListAdapter.getCurrentList().size(), ChatListAdapter.MESSAGE_SENDER);
            }
        });

        mViewModel.getConversationColor().observe(this, color -> {
            if (color != null) {
                setupColorUi(color.getChatColor(), color.getWallpaperColor());
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

    private void triggerIntent() {
        Intent intent = getIntent().getParcelableExtra(MarkNotificationReadReceiver.EXTRA_INTENT);

        if (intent != null) {
            sendBroadcast(intent);
        }
    }

    public String getExtraConversationId() {
        return mConversation.getId();
    }

    private void registerEventClick() {
        mBinding.buttonBack.setOnClickListener(v -> onBackPressed());
        mBinding.layoutTitle.setOnClickListener(v -> openConversationDetail());
        mBinding.conversationThumbnail.setOnClickListener(v -> openConversationDetail());
        mBinding.toolbar.setOnMenuItemClickListener(item -> {
            var itemId = item.getItemId();

            if (itemId == menu_phone_call) {
                openCallActivity(false);

                return true;
            } else if (itemId == menu_video_call) {
                openCallActivity(true);

                return true;
            } else if (itemId == menu_about) {
                openConversationDetail();

                return true;
            }
            return false;
        });
        mBinding.buttonSendMessage.setOnClickListener(v -> {
            Map<MessageMediaUploadType, List<String>> selectedMedia = mMediaFooterStub.get().getSelectedListMediaAsPath();

            if (selectedMedia.isEmpty()) {
                String plainText = mBinding.editTextContent.getText().toString();
                mBinding.editTextContent.getText().clear();

                mViewModel.sendMessage(this, plainText);
            } else {
                mMediaFooterStub.get().resetAllSelectMedia();
                mViewModel.sendMultiMessageDifferentMediaType(this, selectedMedia);
            }
        });
        mBinding.buttonMore.setOnClickListener(v -> {
            mViewModel.getMediaAttachment().removeObservers(this);

            if (mMediaFooterStub.get().isShowing()) {
                mMediaFooterStub.get().hide();

                mBinding.editTextContent.requestFocus();
                ServiceUtil.getInputMethodManager(this).showSoftInput(mBinding.editTextContent, 0);
            } else {
                mViewModel.getMediaAttachment().observe(this, media -> mMediaFooterStub.get().setMediaList(media));

                mMediaFooterStub.get().show();
                mMediaFooterStub.get().setCallback(this);

                ServiceUtil.getInputMethodManager(this).hideSoftInputFromWindow(mBinding.editTextContent.getWindowToken(), 0);

                mViewModel.onMediaKeyboardOpen();
            }
        });
        mBinding.buttonCamera.setOnClickListener(v -> Permission.with(this, mPermissionsLauncher)
                .request(Manifest.permission.CAMERA)
                .ifNecessary()
                .onAllGranted(() -> {
                    Uri uri = FileProviderUtil.createTempFilePicture(getContentResolver());

                    takePictureLauncher.launch(uri, isSuccess -> {
                        if (isSuccess) {
                            String path = FileProviderUtil.getImagePathFromUri(getContentResolver(), uri);

                            if (path != null) {
                                mViewModel.sendMediaMessage(this, path, MessageMediaUploadType.PHOTO);
                            }
                        } else {
                            getContentResolver().delete(uri, null, null);
                        }
                    });
                })
                .withRationaleDialog(getString(R.string.msg_permission_camera_rational), R.drawable.ic_camera)
                .withPermanentDenialDialog(getString(R.string.msg_permission_allow_app_use_camera_title), getString(R.string.msg_permission_camera_message), getString(R.string.msg_permission_settings_construction, getString(R.string.label_camera)))
                .execute());
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
                    hideSendMessageButton();
                } else {
                    showSendMessageButton();
                }
            }
        });
        mBinding.editTextContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && mMediaFooterStub.get().isShowing()) {
                mMediaFooterStub.get().hide();
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
        mBinding.textNewMessage.setOnClickListener(v -> {
            int position = mLayoutManager.findLastVisibleItemPosition();

            if (mChatList.size() - position >= NUM_ITEM_TO_SCROLL_FAST_THRESHOLD) {
                mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1);
            } else {
                mLayoutManager.smoothScrollToPosition(mBinding.recyclerChatList, false, mChatList.size() - 1);
            }
        });
        mBinding.includedFooterOption.layoutUnsent.setOnClickListener(this::handleUnsentMessage);
        mBinding.includedFooterOption.layoutReply.setOnClickListener(this::handleReplyMessage);
        mBinding.includedFooterOption.layoutForward.setOnClickListener(this::handleForwardMessage);
        mBinding.includedFooterOption.layoutCopy.setOnClickListener(this::handleCopyMessageContent);
        mBinding.includedFooterOption.layoutSave.setOnClickListener(this::handleSaveMessageMedia);
    }

    private void openConversationDetail() {
        Intent intent = new Intent(this, ConversationDetailActivity.class);
        intent.putExtra(ConversationDetailActivity.EXTRA_CONVERSATION_ID, mConversation.getId());

        activityResultLauncher.launch(intent, result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    String action = data.getAction();
                    if (action != null) {
                        switch (action) {
                            case ACTION_ADD_MEMBER:
                                ArrayList<UserSelection> members = data.getParcelableArrayListExtra(EXTRA_MEMBER_TO_ADD);

                                mViewModel.addGroupMember(members.stream()
                                                                 .map(UserSelection::getUid)
                                                                 .collect(Collectors.toList()));
                                break;
                            case ACTION_SEARCH_MESSAGE:
                                break;
                            case ACTION_REMOVE_GROUP_MEMBER:
                                mViewModel.removeGroupMember(data.getStringExtra(EXTRA_REMOVE_MEMBER_ID));
                                break;
                            case ACTION_LEAVE_GROUP:
                                mViewModel.leaveGroup();
                                break;
                        }
                    }
                }
            }
        });
    }

    private void openReceiverPopupMenu(int position, View view) {
        int menuRes = mConversation.getGroup() != null ?
                R.menu.menu_conversation_receiver_group :
                R.menu.menu_conversation_receiver_personal;

        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(menuRes);
        menu.setOnMenuItemClickListener(item -> handleReceiverPopupMenu(item, position));
        menu.show();
    }

    private void showSendMessageButton() {
        mBinding.buttonMore.setVisibility(View.GONE);
        mBinding.buttonSendMessage.setVisibility(View.VISIBLE);
    }

    private void hideSendMessageButton() {
        mBinding.buttonMore.setVisibility(View.VISIBLE);
        mBinding.buttonSendMessage.setVisibility(View.GONE);
    }

    private void setupColorUi(String chatColor, String wallpaperColor) {
        var toolbarMenu = mBinding.toolbar.getMenu();
        var menuItemSize = toolbarMenu.size();
        var wallpaperColorStateList = ColorStateList.valueOf(Color.parseColor(chatColor));

        for (int i = 0; i < menuItemSize; i++) {
            var menuItem = toolbarMenu.getItem(i);
            menuItem.setIconTintList(wallpaperColorStateList);
        }
        mBinding.buttonBack.setIconTint(wallpaperColorStateList);
        mBinding.buttonCamera.setIconTint(wallpaperColorStateList);
        mBinding.buttonMic.setIconTint(wallpaperColorStateList);
        mBinding.buttonSendMessage.setIconTint(wallpaperColorStateList);
        mBinding.buttonMore.setIconTint(wallpaperColorStateList);
        mBinding.buttonScrollToBottom.setIconTint(wallpaperColorStateList);
        mBinding.recyclerChatList.setBackgroundColor(Color.parseColor(wallpaperColor));

        mChatListAdapter.setChatColor(wallpaperColorStateList);
        mChatListAdapter.notifyItemRangeChanged(0, mChatListAdapter.getCurrentList().size(), ChatListAdapter.MESSAGE_COLOR);
    }

    private void setupRecyclerView(List<User> mConversationParticipants, ConversationType type) {
        mChatList        = new ArrayList<>();
        mChatListAdapter = new ChatListAdapter(this,
                                                mChatList,
                                                mConversationParticipants,
                                                mCurrentUser,
                                                type,
                                                mConversation.getEncrypted(),
                                                onLoadOutgoingEncryptedMessage());

        mLayoutManager = new CustomLinearLayoutManager(this);
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);

        mBinding.recyclerChatList.setItemAnimator(null);
        mBinding.recyclerChatList.addItemDecoration(new CustomItemDecoration(this, mChatList));
        mBinding.recyclerChatList.setAdapter(mChatListAdapter);
        mBinding.recyclerChatList.setHasFixedSize(true);
        mBinding.recyclerChatList.setLayoutManager(mLayoutManager);
        ChatListAdapter.initializePool(mBinding.recyclerChatList.getRecycledViewPool());

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                checkForShowHeader();
                checkForLoadMore(recyclerView);
                checkForShowScrollButton();
                checkForSeenMessage();
            }
        };
        mBinding.recyclerChatList.addOnScrollListener(mScrollListener);

        mChatListAdapter.submitList(mChatList);
        mChatListAdapter.registerConversationOption(this);
        mChatListAdapter.registerItemEventListener(this);
        mChatListAdapter.registerLinkPreviewListener(this);
        mChatListAdapter.registerVideoListener(this::handlePlayVideo);
        mChatListAdapter.registerOpenConversationDetail(this::openConversationDetail);
        mChatListAdapter.registerOpenReceiverMenu(this::openReceiverPopupMenu);
    }

    private ChatListAdapter.LocalPlaintextInterface onLoadOutgoingEncryptedMessage() {
        return (conversationId, messageId) -> {
            List<LocalPlaintextContentModel> models = mViewModel.getPlaintextContentModel();

            if (models == null) return getString(R.string.dummy_encrypted_message);

            return models.stream()
                         .filter(model -> model.getMessageId().equals(messageId))
                         .map(LocalPlaintextContentModel::getContent)
                         .findFirst()
                         .orElse(getString(R.string.dummy_encrypted_message));
        };
    }

    private void checkForShowHeader() {
        boolean shouldShowHeader = shouldShowHeader();

        mBinding.conversationThumbnail.showHeader(shouldShowHeader, isActive);
        mBinding.toolbarSubtitle.setVisibility(shouldShowHeader && isActive ? View.VISIBLE : View.GONE);
        mBinding.toolbarTitle.setVisibility(shouldShowHeader ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean shouldShowHeader() {
        int firstItemPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (firstItemPosition != -1) {
            Chat firstVisible = mChatList.get(firstItemPosition);
            return !(firstVisible != null && MessageUtil.isDummyProfileMessage(firstVisible));
        }
        return false;
    }

    private void checkForLoadMore(RecyclerView recyclerView) {
        if (mChatList.size() <= 2) {
            return;
        }
        if (!recyclerView.canScrollVertically(-1)) {
            onLoadMore();
        }
    }

    private void onLoadMore() {
        int index = mLayoutManager.findFirstVisibleItemPosition();
        Chat headerChatItem = mChatList.get(index);

        if (headerChatItem != null && !MessageUtil.isDummyProfileMessage(headerChatItem) && getNetworkStatus()) {
            if (!isLoadMore) {
                mViewModel.registerLoadMore(mConversation);
            }
        }
    }

    private void checkForShowScrollButton() {
        int position = mLayoutManager.findLastVisibleItemPosition();
        mViewModel.setScrollButtonState(position < mChatList.size() - NUM_ITEM_TO_SHOW_SCROLL_TO_BOTTOM);
    }

    private void checkForScrollToBottomWhenOpenKeyboard(boolean isSoftKeyShow) {
        if (mLayoutManager != null) {
            int lastItem = mLayoutManager.findLastCompletelyVisibleItemPosition();
            boolean wasAtBottom = lastItem >= mChatList.size() - 1;

            if (!mChatList.isEmpty() && !isLoadMore && isSoftKeyShow && !wasAtBottom) {
                mBinding.recyclerChatList.post(() -> mBinding.recyclerChatList.scrollToPosition(mChatList.size() - 1));
            }
        }
    }

    private void checkForSeenMessage() {
        if (mLayoutManager != null) {
            int position = mLayoutManager.findLastCompletelyVisibleItemPosition();
            if (position < 0) return;

            Chat item = mChatListAdapter.getCurrentList().get(position);

            if (item != null) {
                mViewModel.seenUnreadMessageByTimestamp(mConversation.getId(), item.getTimestamp());

                if (position == mChatListAdapter.getCurrentList().size() - 1) {
                    mViewModel.setNewMessageState(false);
                }
            }
        }
    }

    private void seenWelcomeChat() {
        List<Chat> dummyMessages = mChatList.stream()
                                            .filter(c -> c != null &&
                                                         (MessageUtil.isNotificationMessage(c) ||
                                                         MessageUtil.isWelcomeMessage(c)) &&
                                                         !c.getSeenBy().contains(mCurrentUser.getUid()))
                                            .peek(c -> c.getSeenBy().add(mCurrentUser.getUid()))
                                            .collect(Collectors.toList());
        mViewModel.seenDummyMessage(dummyMessages);
    }

    private void setToolbarTitle(String title) {
        mBinding.toolbarTitle.setText(title);
    }

    @Override
    public void addMember() {
        conversationOptionHandler.addMember(activityResultLauncher, mConversation, memberIds -> {
            mViewModel.addGroupMember(memberIds);
            return null;
        });
    }

    @Override
    public void changeGroupName() {
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_enter_otp_code, null, false);
        DialogEnterOtpCodeBinding binding = DialogEnterOtpCodeBinding.bind(view);

        binding.textTitle.setText(R.string.title_change_group_name);
        binding.textSubtitle.setText(R.string.prompt_enter_your_group_name);
        binding.buttonDone.setEnabled(false);
        binding.getRoot().setPadding(30, 30, 30, 30);
        binding.editTextOtp.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        binding.editTextOtp.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(50)});
        binding.editTextOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                binding.buttonDone.setEnabled(editable.length() > 0);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                                .setView(view)
                                .show();

        binding.buttonCancel.setOnClickListener(v -> dialog.dismiss());
        binding.buttonDone.setOnClickListener(v -> {
            String oldName = mConversation.getGroup().getName();
            String newName = binding.editTextOtp.getText().toString().trim();

            if (!Objects.equals(oldName, newName)) {
                mViewModel.changeGroupName(newName);
            }

            dialog.dismiss();
        });
    }

    @Override
    public void viewGroupMember() {
        Intent intent = new Intent(this, GroupMemberActivity.class);
        intent.putExtra("conversation", mConversation);

        activityResultLauncher.launch(intent, result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();

                if (data != null) {
                    int type = data.getIntExtra(GroupMemberActivity.EXTRA_TYPE, -1);

                    switch (type) {
                        case GroupMemberActivity.TYPE_REMOVE:
                            mViewModel.removeGroupMember(data.getStringExtra(GroupMemberActivity.EXTRA_MEMBER_ID));
                            break;
                        case GroupMemberActivity.TYPE_LEAVE:
                            mViewModel.leaveGroup();
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void changeGroupThumbnail() {
        AlertDialogUtil.showPhotoSelectionDialog(this, (dialog, which) -> {
            if (which == 0) {
                Permission.with(this, mPermissionsLauncher)
                          .request(Manifest.permission.CAMERA)
                          .ifNecessary()
                          .onAllGranted(this::handleTakePicture)
                          .withRationaleDialog(getString(R.string.msg_permission_camera_rational), R.drawable.ic_camera)
                          .withPermanentDenialDialog(getString(R.string.msg_permission_allow_app_use_camera_title), getString(R.string.msg_permission_camera_message), getString(R.string.msg_permission_settings_construction, getString(R.string.label_camera)))
                          .execute();
            } else {
                requestStoragePermission(() -> getContentLauncher.launch("image/*", uri -> {
                    if (uri != null) {
                        String path = FileProviderUtil.getPath(this, uri);
                        if (!path.equals("")) {
                            File file = new File(path);

                            mViewModel.changeGroupThumbnail(this, file);
                        } else {
                            Toast.makeText(this, "Can't open the file, check later", Toast.LENGTH_SHORT).show();
                        }
                    }
                }));
            }
        });
    }

    private void handleTakePicture() {
        Uri uri = FileProviderUtil.createTempFilePicture(getContentResolver());

        takePictureLauncher.launch(uri, isSuccess -> {
            if (isSuccess) {
                ImageThumbnail imageThumbnail = FileProviderUtil.getImageThumbnailFromUri(getContentResolver(), uri);

                Intent intent = new Intent(this, PreviewEditPhotoActivity.class);
                intent.putExtra(PreviewEditPhotoActivity.EXTRA_CHANGE_PHOTO, PreviewEditPhotoActivity.EXTRA_GROUP_THUMBNAIL);
                intent.putExtra(PreviewEditPhotoActivity.EXTRA_IMAGE_THUMBNAIL, imageThumbnail);

                activityResultLauncher.launch(intent, result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();

                        if (data != null) {
                            String filePath = data.getStringExtra(PreviewEditPhotoActivity.EXTRA_FILE_PATH_RESULT);
                            File file = new File(filePath);

                            mViewModel.changeGroupThumbnail(this, file);
                        }
                    }
                });
            } else {
                getContentResolver().delete(uri, null, null);
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        // Show the timestamp and list of users have seen messages.
    }

    @Override
    public void onItemLongClick(int position) {
        setupForShowFooterOption(mChatListAdapter.getCurrentList().get(position));
    }

    private void setupForShowFooterOption(Chat message) {
        String senderId = message.getSenderId();
        String userId   = mCurrentUser.getUid();

        mBinding.includedFooterOption.layoutUnsent.setVisibility(!senderId.equals(userId) ? View.GONE : View.VISIBLE);
        mBinding.includedFooterOption.layoutSave.setVisibility(MessageUtil.isMultiMediaMessage(message) && !MessageUtil.isShareMessage(message) ? View.VISIBLE : View.INVISIBLE);
        mBinding.includedFooterOption.layoutCopy.setVisibility(MessageUtil.isMultiMediaMessage(message) && !MessageUtil.isShareMessage(message) ? View.INVISIBLE : View.VISIBLE);
        mBinding.includedFooterOption.layoutSave.setTag(message);
        mBinding.includedFooterOption.layoutUnsent.setTag(message);
        mBinding.includedFooterOption.layoutCopy.setTag(message);
        mBinding.includedFooterOption.layoutReply.setTag(message);
        mBinding.includedFooterOption.layoutForward.setTag(message);

        if (!message.isUnsent() && message.getStatus() != MessageStatus.SENDING) {
            shouldShowFooterOption(true);
        }
    }

    @Override
    public void onListItemSizeChanged(int size) {
        // Default implementation
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        Rect footerRect = new Rect();
        mBinding.includedFooterOption.getRoot().getHitRect(footerRect);

        if (event.getAction() == MotionEvent.ACTION_DOWN &&
                !footerRect.contains(x, y) &&
                mBinding.includedFooterOption.getRoot().getVisibility() == View.VISIBLE) {
            shouldShowFooterOption(false);
        }
        return super.dispatchTouchEvent(event);
    }

    private void shouldShowFooterOption(boolean isShow) {
        mBinding.includedFooterOption.getRoot().startAnimation(isShow ? slideUpAnimation : AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down));
        mBinding.includedFooterOption.getRoot().setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void handleCopyMessageContent(View view) {
        Chat             message = (Chat) view.getTag();
        ClipData         cd      = ClipData.newPlainText("Copied Message Content", MessageUtil.isShareMessage(message) ? message.getShare().getLink() : message.getContent());

        ServiceUtil.getClipboardManager(this).setPrimaryClip(cd);

        shouldShowFooterOption(false);

        Toast.makeText(this, R.string.msg_copy_message_successfully, Toast.LENGTH_SHORT).show();
    }

    private void handleReplyMessage(View view) {
        shouldShowFooterOption(false);

        Chat message = (Chat) view.getTag();
    }

    private void handleForwardMessage(View view) {
        shouldShowFooterOption(false);

        Chat message = (Chat) view.getTag();

        Intent intent = new Intent(this, ForwardMessageActivity.class);

        // Put boolean extra to open single send list conversation in ForwardMessageActivity
        // These key extra must correct with constant val in SuggestionFriendListFragment
        intent.putExtra(SuggestionFriendListFragment.ARG_INCLUDE_GROUP, true);
        intent.putExtra(SuggestionFriendListFragment.ARG_SINGLE_SEND, true);
        intent.putExtra("message", message);

        startActivity(intent);
    }

    private void handleUnsentMessage(View view) {
        Chat message = (Chat) view.getTag();

        shouldShowFooterOption(false);

        mViewModel.unsentMessage(message);
    }

    private void handleSaveMessageMedia(View view) {
        Chat.Media media = (Chat.Media) view.getTag();

        Permission.with(this, mPermissionsLauncher)
                  .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                  .ifNecessary(!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q))
                  .onAllGranted(() -> WorkDependency.enqueue(new DownloadMediaWorkWrapper(this, media.getUri(), media instanceof Chat.Video)))
                  .withRationaleDialog(getString(R.string.msg_permission_external_storage_rational), R.drawable.ic_round_storage_24)
                  .withPermanentDenialDialog(getString(R.string.msg_permission_allow_app_use_external_storage_title), getString(R.string.msg_permission_external_storage_message), getString(R.string.msg_permission_settings_construction, getString(R.string.label_storage)))
                  .execute();

        shouldShowFooterOption(false);
    }

    private void handlePlayVideo(Chat chat, Chat.Video video) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_START_INDEX, chat.getVideos().indexOf(video));
        intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_URI, chat.getVideos()
                                                                          .stream()
                                                                          .map(Chat.Media::getUri)
                                                                          .collect(Collectors.toCollection(ArrayList::new)));
        startActivity(intent);
    }

    @Override
    public void onClick(@NonNull View v, @NonNull Chat.Photo photo) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar_TranslucentDecor);
        dialog.setContentView(R.layout.dialog_image_preview);

        ImageView img         = dialog.findViewById(R.id.imgAvatar);
        View      imgBack     = dialog.findViewById(R.id.imgBack);
        View      imgMoreVert = dialog.findViewById(R.id.imgMoreVert);

        imgBack.setOnClickListener(view -> dialog.dismiss());

        Picture.loadSecureResource(getAppPreference().getUserAuthToken().orElse(""), this, photo.getUri())
               .apply(RequestOptions.centerCropTransform())
               .into(img);

        dialog.show();
    }

    @Override
    public boolean onLongClick(@NonNull View v, int indexOfMedia) {
        if (indexOfMedia != -1) {
            Chat chat = (Chat) v.getTag();

            setupForShowFooterOption(chat);

            Chat.Media media = null;

            if (MessageUtil.isVideoMessage(chat)) {
                media = chat.getVideos().get(indexOfMedia);
            } else if (MessageUtil.isPhotoMessage(chat)) {
                media = chat.getPhotos().get(indexOfMedia);
            }

            mBinding.includedFooterOption.layoutSave.setTag(media);
        }
        return true;
    }

    @Nullable
    @Override
    public LinkPreviewMetadata onBindLinkPreview(@NonNull String messageId) {
        return Objects.requireNonNull(mViewModel.getLinkPreviewMapper().getValue()).get(messageId);
    }

    @Override
    public void onLoadLinkPreview(@NonNull String messageId, @NonNull String url) {
        mViewModel.loadLinkPreview(messageId, url);
    }

    @Override
    public void onOpenLink(@NonNull String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LinkPreviewMetadata.resolveHttpUrl(url))));
    }

    @Override
    public boolean onLinkPreviewLongClick(@NonNull Chat message) {
        setupForShowFooterOption(message);
        return true;
    }

    @Override
    public void onGalleryClick() {
        requestStoragePermission(() -> mGetMultipleContentLauncher.launch("image/*|video/*", uriList ->
                mViewModel.sendMultiMessageDifferentMediaType(this, getMessageMediaUploadType(uriList))));
    }

    @Override
    public void onFileClick() {
        requestStoragePermission(() -> mGetMultipleDocument.launch(new String[]{"*/*"}, uriList ->
                mViewModel.sendMultiMessageDifferentMediaType(this, getMessageMediaUploadType(uriList))));
    }

    @Override
    public void onMediaClick(int totalSelectedItem) {
        if (totalSelectedItem != 0) {
            showSendMessageButton();
        } else {
            hideSendMessageButton();
        }
    }

    @Override
    public void onRequestPermissionStorage() {
        requestStoragePermission(() -> mViewModel.onMediaKeyboardOpen());
    }

    private void requestStoragePermission(Runnable onAllGranted) {
        Permission.with(this, mPermissionsLauncher)
                  .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                  .ifNecessary()
                  .onAllGranted(onAllGranted)
                  .withRationaleDialog(getString(R.string.msg_permission_external_storage_rational), R.drawable.ic_round_storage_24)
                  .withPermanentDenialDialog(getString(R.string.msg_permission_allow_app_use_external_storage_title), getString(R.string.msg_permission_external_storage_message), getString(R.string.msg_permission_settings_construction, getString(R.string.label_storage)))
                  .execute();
    }

    private Map<MessageMediaUploadType, List<String>> getMessageMediaUploadType(List<Uri> uriList) {
        Map<String, List<Media>> dataForMediaMessage = uriList.stream()
                                                              .map(uri -> FileProviderUtil.getMediaFromUriSpecificId(this, uri))
                                                              .filter(Objects::nonNull)
                                                              .collect(Collectors.groupingBy(Media::getMimeType));

        Map<MessageMediaUploadType, List<String>> data = new HashMap<>();

        dataForMediaMessage.forEach((mimeType, mediaList) -> {
            List<String> paths = mediaList.stream()
                                          .map(media -> {
                                              if (media.getPath().equals("")) {
                                                  return FileProviderUtil.getPath(this, media.getUri());
                                              } else return media.getPath();
                                          })
                                          .collect(Collectors.toList());
            data.put(MediaUtil.mapTypeToMessageUploadType(mimeType), paths);
        });

        return data;
    }

    private boolean handleReceiverPopupMenu(MenuItem item, int position) {
        Chat chat = mChatListAdapter.getCurrentList().get(position);
        int  id   = item.getItemId();

        if (id == menu_view_profile) {
            Intent intent = new Intent(this, ConnectPeopleActivity.class);
            intent.setAction(ConnectPeopleActivity.ACTION_FIND_USER);
            intent.putExtra(ConnectPeopleActivity.EXTRA_USER_ID, chat.getSenderId());

            startActivity(intent);
            return true;
        } else if (id == menu_phone_call) {
            openCallActivity(false);
            return true;
        } else if (id == menu_video_call) {
            openCallActivity(true);
            return true;
        } else if (id == menu_send_message) {
            return true;
        } else if (id == menu_block) {
            return true;
        } else {
            return false;
        }
    }

    private void openCallActivity(boolean isVideoEnabled) {
        if (mConversation.getType() == ConversationType.GROUP) {
            Toast.makeText(this, R.string.msg_webrtc_call_is_not_available_for_group, Toast.LENGTH_SHORT).show();
        } else {
            Permission.with(this, mPermissionsLauncher)
                      .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                      .ifNecessary()
                      .onAllGranted(() -> {
                          //noinspection OptionalGetWithoutIsPresent
                          String participantId = mConversation.getParticipants()
                                                              .stream()
                                                              .filter(u -> !u.getUid().equals(mCurrentUser.getUid()))
                                                              .findFirst()
                                                              .get()
                                                              .getUid();

                          startActivity(isVideoEnabled ?
                                        WebRtcCallActivity.createVideoCallIntent(this, participantId) :
                                        WebRtcCallActivity.createAudioCallIntent(this, participantId));
                      })
                      .onAnyDenied(() -> Toast.makeText(this, R.string.msg_webrtc_permission_on_any_denied, Toast.LENGTH_SHORT).show())
                      .onSomePermanentlyDenied(granted -> Toast.makeText(this, R.string.msg_webrtc_some_permission_permanently_denied, Toast.LENGTH_SHORT).show())
                      .withRationaleDialog(getString(R.string.msg_permission_camera_rational), R.drawable.ic_round_videocam, R.drawable.ic_round_mic)
                      .withPermanentDenialDialog(getString(R.string.msg_permission_allow_app_use_camera_title), getString(R.string.msg_permission_camera_message), getString(R.string.msg_permission_settings_construction, getString(R.string.label_camera)))
                      .execute();
        }
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
                MessageUtil.isDummyProfileMessage(preItem) ||
                MessageUtil.isWelcomeMessage(preItem)) {
                return;
            }

            /*
             * Add margin between two different sender chat item.
             * */
            if (!shouldShowTimestamp(preItem, item)) {
                if (!item.getSenderId().equals(preItem.getSenderId()) && !MessageUtil.isDummyMessage(item)) {
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