package com.mqv.vmess.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.vmess.R;
import com.mqv.vmess.databinding.ItemRankConversationBinding;
import com.mqv.vmess.network.model.RemoteUser;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Picture;

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

            var randomIndex = getAdapterPosition() >= Const.DUMMIES_IMAGES_URL.length ?
                    Const.DUMMIES_IMAGES_URL.length - 1 : getAdapterPosition();
            Picture.loadUserAvatar(context, Const.DUMMIES_IMAGES_URL[randomIndex]).into(mRankBinding.imageAvatar);
        }
    }
}
