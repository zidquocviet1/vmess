package com.mqv.realtimechatapplication.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.preferences.PreferenceManageAccountsActivity;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Objects;

public class UserPreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        var context = requireContext();

        @SuppressLint("RestrictedApi")
        var screen = getPreferenceManager()
                .inflateFromResource(context, R.xml.user_information_settings, null);
        var category = Objects.requireNonNull((PreferenceGroup)
                screen.findPreference(getString(R.string.key_pref_category_accounts)));

        if (user != null) {
            var manageAccountsItem = new Preference(context);
            manageAccountsItem.setKey(getString(R.string.key_pref_manage_accounts));
            manageAccountsItem.setTitle(getString(R.string.title_preference_item_manage_accounts));
            manageAccountsItem.setIcon(R.drawable.ic_preferences_manage_accounts);
            manageAccountsItem.setIntent(new Intent(requireActivity(), PreferenceManageAccountsActivity.class));

            var signedInItem = new Preference(context);
            signedInItem.setTitle(user.getDisplayName());
            signedInItem.setSummary(getString(R.string.title_preference_item_manage_accounts_summary));

            var uri = user.getPhotoUrl();
            if (uri != null) {
                //TODO: reformat the url in the develop mode
                var url = uri.toString().replace("localhost", Const.BASE_IP);

                Glide.with(context)
                        .load(url)
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                signedInItem.setIcon(R.drawable.ic_preferences_image_user_not_found);
                                category.addPreference(signedInItem);
                                category.addPreference(manageAccountsItem);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                var srcBmp = ((BitmapDrawable) resource.mutate()).getBitmap();
                                var dstBmp = createCenterCropBitmap(srcBmp);
                                var scaledBmp = Bitmap.createScaledBitmap(dstBmp, 88, 88, false);

                                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), scaledBmp);
                                rbd.setCornerRadius(Math.max(scaledBmp.getHeight(), scaledBmp.getWidth()) / 2.0f);

                                signedInItem.setIcon(rbd);
                                category.addPreference(signedInItem);
                                category.addPreference(manageAccountsItem);
                                return false;
                            }
                        })
                        .centerCrop()
                        .signature(new ObjectKey(url))
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            }
        }

        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var listView = getListView();
        listView.setNestedScrollingEnabled(false);
        listView.setHasFixedSize(false);
        listView.setVerticalScrollBarEnabled(false);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        var key = preference.getKey();
        // Accounts pref category
        if (TextUtils.equals(key, getString(R.string.key_pref_manage_accounts))) {
            Logging.show(preference.getTitle().toString());
        }
        // Profile pref category
        else if (TextUtils.equals(key, getString(R.string.key_pref_dark_mode))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_messages_requests))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_active_status))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_username))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_qr_code))) {
            Logging.show(preference.getTitle().toString());
        }
        // Preferences pref category
        else if (TextUtils.equals(key, getString(R.string.key_pref_notifications_and_sounds))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_archived_chats))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_privacy))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_story))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_phone_contacts))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_photos_and_media))) {
            Logging.show(preference.getTitle().toString());
        }
        // System pref category
        else if (TextUtils.equals(key, getString(R.string.key_pref_account_settings))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_report_problem))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_help))) {
            Logging.show(preference.getTitle().toString());
        } else if (TextUtils.equals(key, getString(R.string.key_pref_legal_and_policies))) {
            Logging.show(preference.getTitle().toString());
        }
        return super.onPreferenceTreeClick(preference);
    }

    private Bitmap createCenterCropBitmap(Bitmap srcBmp) {
        Bitmap dstBmp;
        var height = srcBmp.getHeight();
        var width = srcBmp.getWidth();

        if (width >= height) {
            dstBmp = Bitmap.createBitmap(srcBmp, width / 2 - height / 2, 0, height, height);
        } else {
            dstBmp = Bitmap.createBitmap(srcBmp, 0, height / 2 - width / 2, width, width);
        }

        return dstBmp;
    }
}
