package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.EditProfileLinkActivity;
import com.mqv.realtimechatapplication.databinding.ItemIndividualSocialLinkBinding;
import com.mqv.realtimechatapplication.databinding.ItemPreferenceContentBinding;
import com.mqv.realtimechatapplication.network.model.SocialType;
import com.mqv.realtimechatapplication.network.model.UserSocialLink;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class UserLinkAdapter extends ListAdapter<UserSocialLink, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final int mLayout;
    private final ACTION mAction;
    public static final SparseIntArray ICON_SOCIAL = new SparseIntArray();
    private Consumer<UserSocialLink> onSocialLinkViewClick;
    private final List<UserSocialLink> mMutableList;

    // Edit selected item section
    private OnEditSocialLinkItemListener callback;
    private SocialType mEditSocialType;

    public enum ACTION {
        EDIT,
        VIEW
    }

    static {
        ICON_SOCIAL.put(SocialType.INSTAGRAM.getKey(), R.drawable.ic_social_instagram);
        ICON_SOCIAL.put(SocialType.FACEBOOK.getKey(), R.drawable.ic_social_facebook);
        ICON_SOCIAL.put(SocialType.GITHUB.getKey(), R.drawable.ic_social_github);
        ICON_SOCIAL.put(SocialType.LINKEDIN.getKey(), R.drawable.ic_social_linkedin);
        ICON_SOCIAL.put(SocialType.PINTEREST.getKey(), R.drawable.ic_social_pinterest);
        ICON_SOCIAL.put(SocialType.SOUNDCLOUD.getKey(), R.drawable.ic_social_soundcloud);
        ICON_SOCIAL.put(SocialType.TIKTOK.getKey(), R.drawable.ic_social_tiktok);
        ICON_SOCIAL.put(SocialType.TUMBLR.getKey(), R.drawable.ic_social_tumblr);
        ICON_SOCIAL.put(SocialType.TWITTER.getKey(), R.drawable.ic_social_twitter);
        ICON_SOCIAL.put(SocialType.VINE.getKey(), R.drawable.ic_social_vine);
        ICON_SOCIAL.put(SocialType.VK.getKey(), R.drawable.ic_social_vk);
    }

    public UserLinkAdapter(Context context, List<UserSocialLink> mutableList, int layout, ACTION action) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull UserSocialLink oldItem, @NonNull UserSocialLink newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull UserSocialLink oldItem, @NonNull UserSocialLink newItem) {
                return oldItem.equals(newItem);
            }
        });
        mContext = context;
        mLayout = layout;
        mAction = action;
        mMutableList = mutableList;
    }

    public void setOnSocialLinkViewClickListener(Consumer<UserSocialLink> onSocialLinkViewClick) {
        this.onSocialLinkViewClick = onSocialLinkViewClick;
    }

    public void addItem(UserSocialLink item) {
        mMutableList.add(item);
        submitList(mMutableList);
        notifyItemInserted(mMutableList.indexOf(item));
    }

    public void removeItem(int position) {
        mMutableList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mMutableList.size());
    }

    public void editItem(int position, SocialType newType, String newAccountName) {
        var editItem = mMutableList.get(position);
        editItem.setId(System.currentTimeMillis());
        editItem.setAccountName(newAccountName);
        editItem.setType(newType);

        notifyItemChanged(position);
    }

    public interface OnEditSocialLinkItemListener {
        void onOpenPlatformSelection(Button button, UserSocialLink item);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(mLayout, parent, false);
        if (mAction == ACTION.VIEW)
            return new UserLinkViewHolder(view, mContext);
        else
            return new UserLinkEditViewHolder(view, mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        var item = getItem(position);

        if (mAction == ACTION.VIEW) {
            var viewHolder = (UserLinkViewHolder) holder;

            viewHolder.bind(item);
            viewHolder.itemView.setOnClickListener(v -> {
                if (onSocialLinkViewClick != null)
                    onSocialLinkViewClick.accept(item);
            });
        } else if (mAction == ACTION.EDIT) {
            /*
            * Cast the current context to listener.
            * The activity use this class must be implement the OnEditSocialLinkItemListener
            * */
            if (mContext instanceof OnEditSocialLinkItemListener) {
                callback = (OnEditSocialLinkItemListener) mContext;
            } else {
                throw new RuntimeException(mContext + " must implement the OnEditSocialLinkItemListener");
            }

            var editHolder = (UserLinkEditViewHolder) holder;
            editHolder.bind(item);

            /*
             * Set event to change mode to Edit Selected Item
             * */
            editHolder.mBinding.layoutItem.setOnClickListener(v -> editHolder.changeUi(true));

            /*
             * Event listener to remove the selected item
             * */
            editHolder.mBinding.buttonRemove.setOnClickListener(v -> removeItem(position));

            /*
            * Change to the new Social Type
            * */
            editHolder.mBinding.buttonSelectService.setOnClickListener(v ->
                    callback.onOpenPlatformSelection((Button) v, item));

            /*
            * Register editor action listener on the EditText Account Name
            * And confirm the new account name is changed
            * */
            var mEditAccount = Objects.requireNonNull(editHolder.mBinding.textLayoutAccountName.getEditText());
            mEditAccount.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    var imm = mContext.getSystemService(InputMethodManager.class);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    editHolder.changeUi(false);

                    var accountName = v.getText().toString().trim();

                    if (!TextUtils.isEmpty(accountName) && mEditSocialType != null) {
                        editItem(position, mEditSocialType, accountName);
                    }
                    return true;
                }
                return false;
            });

            /*
            * Receive the new selected Social Type and then change the name of button
            * */
            ((EditProfileLinkActivity) mContext).setOnSocialSelectedFromAdapterListener((button, type) -> {
                if (type != null && button != null) {
                    mEditSocialType = type;
                    button.setText(reformatSocialBrandName(type));
                }
            });
        }
    }

    private String reformatSocialBrandName(@NonNull SocialType type) {
        var socialName = type.getValue();
        return socialName.substring(0, 1).toUpperCase(Locale.ROOT) + socialName.substring(1);
    }

    public static class UserLinkViewHolder extends RecyclerView.ViewHolder {
        ItemPreferenceContentBinding mBinding;
        Context mContext;

        public UserLinkViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            mBinding = ItemPreferenceContentBinding.bind(itemView);
            mContext = context;
        }

        public void bind(UserSocialLink item) {
            var icon = ContextCompat.getDrawable(mContext, ICON_SOCIAL.get(item.getType().getKey()));

            mBinding.icon.setImageDrawable(icon);
            mBinding.title.setText(item.getAccountName());
            mBinding.summary.setVisibility(View.GONE);
        }
    }

    public static class UserLinkEditViewHolder extends RecyclerView.ViewHolder {
        ItemIndividualSocialLinkBinding mBinding;
        EditText editAccount;
        UserSocialLink currentItem;
        Context mContext;

        public UserLinkEditViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            mBinding = ItemIndividualSocialLinkBinding.bind(itemView);
            mContext = context;
            editAccount = Objects.requireNonNull(mBinding.textLayoutAccountName.getEditText());
        }

        public void bind(UserSocialLink item) {
            var icon = ContextCompat.getDrawable(mContext, ICON_SOCIAL.get(item.getType().getKey()));

            mBinding.summary.setText(reformatSocialBrandName(item.getType()));
            mBinding.icon.setBackground(icon);
            mBinding.title.setText(item.getAccountName());
            currentItem = item;
        }

        public void changeUi(boolean isEditing) {
            mBinding.layoutSocialView.setVisibility(isEditing ? View.GONE : View.VISIBLE);
            mBinding.layoutSocialEdit.setVisibility(isEditing ? View.VISIBLE : View.GONE);
            if (isEditing) {
                mBinding.buttonSelectService.setText(reformatSocialBrandName(currentItem.getType()));
                editAccount.setText(currentItem.getAccountName());
            }
        }

        private String reformatSocialBrandName(@NonNull SocialType type) {
            var socialName = type.getValue();
            return socialName.substring(0, 1).toUpperCase(Locale.ROOT) + socialName.substring(1);
        }
    }
}
