package com.mqv.realtimechatapplication.activity.br;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mqv.realtimechatapplication.network.model.Conversation;

public class ConversationBroadcastReceiver extends BroadcastReceiver {
    private OnConversationListener mListener;

    public interface OnConversationListener {
        void onNewConversationReceive(Conversation conversation);

        void onDeleteConversation(String unFriendUserId);
    }

    public ConversationBroadcastReceiver() {
    }

    public ConversationBroadcastReceiver(OnConversationListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals("com.mqv.tac.NEW_CONVERSATION")) {
            boolean isUnFriend = intent.getBooleanExtra("is_unfriend", false);

            if (isUnFriend) {
                String unFriendUserId = intent.getStringExtra("unfriend_user_id");

                if (mListener != null)
                    mListener.onDeleteConversation(unFriendUserId);
            } else {
                Conversation conversation = intent.getParcelableExtra("data");

                if (mListener != null)
                    mListener.onNewConversationReceive(conversation);
            }
        }
    }
}
