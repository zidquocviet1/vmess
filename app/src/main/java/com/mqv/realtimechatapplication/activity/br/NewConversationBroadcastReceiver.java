package com.mqv.realtimechatapplication.activity.br;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mqv.realtimechatapplication.network.model.Conversation;

public class NewConversationBroadcastReceiver extends BroadcastReceiver {
    private OnNewConversationListener mListener;

    public interface OnNewConversationListener {
        void onNewConversationReceive(Conversation conversation);
    }

    public NewConversationBroadcastReceiver() {
    }

    public NewConversationBroadcastReceiver(OnNewConversationListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals("com.mqv.tac.NEW_CONVERSATION")) {
            Conversation conversation = intent.getParcelableExtra("data");

            if (mListener != null)
                mListener.onNewConversationReceive(conversation);
        }
    }
}
