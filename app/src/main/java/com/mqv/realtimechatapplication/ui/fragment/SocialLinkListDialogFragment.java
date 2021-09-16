package com.mqv.realtimechatapplication.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.FragmentSocialLinkListDialogBinding;
import com.mqv.realtimechatapplication.databinding.ItemFragmentSocialLinkListDialogBinding;

import java.util.ArrayList;
import java.util.Locale;

public class SocialLinkListDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_LIST_LINK = "item_count";
    private FragmentSocialLinkListDialogBinding binding;
    private OnSocialPlatformSelected mCallback;

    public static SocialLinkListDialogFragment newInstance(ArrayList<String> socialTypeString) {
        final SocialLinkListDialogFragment fragment = new SocialLinkListDialogFragment();
        final Bundle args = new Bundle();
        args.putStringArrayList(ARG_LIST_LINK, socialTypeString);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnSocialPlatformSelected {
        void onPlatformSelected(int position);

        void onDialogFinish();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSocialPlatformSelected){
            mCallback = (OnSocialPlatformSelected) context;
        }else{
            throw new RuntimeException(context + "must implement OnSocialPlatformSelected");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSocialLinkListDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.list.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.list.setAdapter(new UserSocialLinkAdapter(getArguments().getStringArrayList(ARG_LIST_LINK)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public int getTheme() {
        return R.style.UserSocialLinkListDialogFragment;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFragmentSocialLinkListDialogBinding binding;

        ViewHolder(ItemFragmentSocialLinkListDialogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindTo(String socialTypeName) {
            var capital = socialTypeName.substring(0, 1).toUpperCase(Locale.ROOT) + socialTypeName.substring(1);
            binding.textSocialName.setText(capital);
        }
    }

    private class UserSocialLinkAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final ArrayList<String> links;

        UserSocialLinkAdapter(ArrayList<String> links) {
            this.links = links;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var view = ItemFragmentSocialLinkListDialogBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bindTo(links.get(position));
            holder.itemView.setOnClickListener(v -> {
                if (mCallback != null) {
                    mCallback.onPlatformSelected(position);

                    dismiss();
                    mCallback.onDialogFinish();
                }
            });
        }

        @Override
        public int getItemCount() {
            return links.size();
        }
    }
}