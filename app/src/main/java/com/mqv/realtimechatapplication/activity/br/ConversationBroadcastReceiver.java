package com.mqv.realtimechatapplication.activity.br;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mqv.realtimechatapplication.MainApplication;
import com.mqv.realtimechatapplication.activity.ConversationActivity;
import com.mqv.realtimechatapplication.network.model.Conversation;

public class ConversationBroadcastReceiver extends BroadcastReceiver {
    private static final String KEY_UNFRIEND = "is_unfriend";
    private static final String KEY_NEW_MESSAGE = "new_message";
    private static final String EXTRA_UNFRIEND_USER_ID = "unfriend_user_id";
    private static final String EXTRA_CONVERSATION_DATA = "data";
    private static final String EXTRA_MESSAGE_ID = "message_id";
    private static final String ACTION_NEW_CONVERSATION = "com.mqv.tac.NEW_CONVERSATION";

    private OnConversationListener mListener;

    public interface OnConversationListener {
        void onNewConversationReceive(Conversation conversation);

        void onDeleteConversation(String unFriendUserId);

        void onConversationNewMessage(String newMessageId, Conversation conversation);
    }

    public ConversationBroadcastReceiver() {
    }

    public ConversationBroadcastReceiver(OnConversationListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(ACTION_NEW_CONVERSATION)) {
            boolean isUnFriend                  = intent.getBooleanExtra(KEY_UNFRIEND, false);
            boolean isConversationNewMessage    = intent.getBooleanExtra(KEY_NEW_MESSAGE, false);

            if (mListener != null) {
                if (isUnFriend) {
                    String unFriendUserId = intent.getStringExtra(EXTRA_UNFRIEND_USER_ID);
                    mListener.onDeleteConversation(unFriendUserId);
                } else if (isConversationNewMessage) {
                    fetchLiveMessageInConversationList(context, intent);
                } else {
                    Conversation conversation = intent.getParcelableExtra(EXTRA_CONVERSATION_DATA);
                    mListener.onNewConversationReceive(conversation);
                }
            }
        }
    }

    private void fetchLiveMessageInConversationList(Context context, Intent intent) {
        MainApplication application     = (MainApplication) context.getApplicationContext();
        Activity        activeActivity  = application.getActiveActivity();
        String          newMessageId    = intent.getStringExtra(EXTRA_MESSAGE_ID);
        Conversation    conversation    = intent.getParcelableExtra(EXTRA_CONVERSATION_DATA);

        if (activeActivity instanceof ConversationActivity) {
            ConversationActivity conversationActivity = (ConversationActivity) activeActivity;
            Conversation         currentConversation  = conversationActivity.currentConversation();

            if (!currentConversation.getId().equals(conversation.getId()))
                mListener.onConversationNewMessage(newMessageId, conversation);
        } else {
            mListener.onConversationNewMessage(newMessageId, conversation);
        }
    }
}
