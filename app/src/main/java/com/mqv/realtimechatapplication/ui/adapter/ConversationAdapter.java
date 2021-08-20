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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemConversationBinding;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.MessageStatus;

import java.util.List;
import java.util.Random;

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
            var randomIndex = new Random().nextInt(Const.DUMMIES_IMAGES_URL.length);

            if (item.getStatus() == MessageStatus.SEEN){
                Glide.with(context)
                        .load(Const.DUMMIES_IMAGES_URL[randomIndex])
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .signature(new ObjectKey(Const.DUMMIES_IMAGES_URL[randomIndex]))
                        .into(mBinding.imageState);
            }else if (item.getStatus() == MessageStatus.NOT_RECEIVED){
                mBinding.imageState.setBackground(context.getDrawable(R.drawable.ic_check_circle_outline));
                mBinding.imageState.setBackgroundTintList(context.getColorStateList(R.color.ic_background_tint));
            }else{
                mBinding.imageState.setBackground(context.getDrawable(R.drawable.ic_round_check_circle));
                mBinding.imageState.setBackgroundTintList(context.getColorStateList(R.color.ic_background_tint));
            }
            mBinding.textTitleConversation.setText(item.getTitle());
            mBinding.textContentConversation.setText(item.getLastMessage());

            Glide.with(context)
                    .load(Const.DUMMIES_IMAGES_URL[randomIndex])
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .signature(new ObjectKey(Const.DUMMIES_IMAGES_URL[randomIndex]))
                    .into(mBinding.imageConversation);
        }
    }
}
