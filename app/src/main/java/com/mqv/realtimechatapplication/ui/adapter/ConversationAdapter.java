package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.util.MessageStatus;

import java.util.List;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder> {
    private final List<Conversation> data;
    private final Context context;

    public ConversationAdapter(List<Conversation> data, Context context){
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.data = data;
        this.context = context;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        var item = getItem(position);
        holder.bind(item, context);
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding mBinding;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ItemConversationBinding.bind(itemView);
        }

        public void bind(Conversation item, Context context){
            Drawable imageState;
            if (item.getStatus() == MessageStatus.SEEN){
                imageState = context.getDrawable(R.drawable.ic_facebook);
            }else if (item.getStatus() == MessageStatus.NOT_RECEIVED){
                imageState = context.getDrawable(R.drawable.ic_check_circle_outline);
                mBinding.imageState.setBackgroundTintList(context.getColorStateList(R.color.ic_background_tint));
            }else{
                imageState = context.getDrawable(R.drawable.ic_round_check_circle);
                mBinding.imageState.setBackgroundTintList(context.getColorStateList(R.color.ic_background_tint));
            }
            mBinding.textTitleConversation.setText(item.getTitle());
            mBinding.textContentConversation.setText(item.getLastMessage());
            mBinding.imageState.setBackground(imageState);
        }
    }
}
