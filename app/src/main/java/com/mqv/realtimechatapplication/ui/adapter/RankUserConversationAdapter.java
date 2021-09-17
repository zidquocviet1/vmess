package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemRankConversationBinding;
import com.mqv.realtimechatapplication.network.model.RemoteUser;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Random;

public class RankUserConversationAdapter extends ListAdapter<RemoteUser, RankUserConversationAdapter.RankUserViewHolder> {
    private final Context context;

    public RankUserConversationAdapter(Context context) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull RemoteUser oldItem, @NonNull RemoteUser newItem) {
                return oldItem.getUserId().equals(newItem.getUserId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull RemoteUser oldItem, @NonNull RemoteUser newItem) {
                return (oldItem.getUserId().equals(newItem.getUserId()) &&
                        oldItem.getFirstName().equals(newItem.getFirstName()) &&
                        oldItem.getLastName().equals(newItem.getLastName()));
            }
        });
        this.context = context;
    }

    @NonNull
    @Override
    public RankUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(context).inflate(R.layout.item_rank_conversation, parent, false);
        return new RankUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankUserViewHolder holder, int position) {
        var item = getItem(position);
        holder.bind(item, context);
    }

    public static class RankUserViewHolder extends RecyclerView.ViewHolder {
        private final ItemRankConversationBinding mRankBinding;

        public RankUserViewHolder(@NonNull View itemView) {
            super(itemView);
            mRankBinding = ItemRankConversationBinding.bind(itemView);
        }

        public void bind(RemoteUser item, Context context){
            mRankBinding.textFirstName.setText(item.getFirstName());
            mRankBinding.textLastName.setText(item.getLastName());

            var randomIndex = new Random().nextInt(Const.DUMMIES_IMAGES_URL.length);

            var placeHolder = new CircularProgressDrawable(context);
            placeHolder.setStrokeWidth(5f);
            placeHolder.setCenterRadius(30f);
            placeHolder.start();


            Glide.with(context)
                    .load(Const.DUMMIES_IMAGES_URL[randomIndex])
                    .centerCrop()
                    .placeholder(placeHolder)
                    .error(ContextCompat.getDrawable(context, R.drawable.ic_round_account))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .signature(new ObjectKey(Const.DUMMIES_IMAGES_URL[randomIndex]))
                    .into(mRankBinding.imageAvatar);
        }
    }
}
