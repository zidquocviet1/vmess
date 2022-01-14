package com.mqv.realtimechatapplication.ui.fragment.preference;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
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

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.preferences.PreferenceManageAccountsActivity;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserPreferencesFragment extends PreferenceFragmentCompat {
    private static final String KEY_PREF_SIGNED_IN = "signedIn";
    private static final int    USER_AVATAR_HEIGHT = 88;
    private static final int    USER_AVATAR_WIDTH  = 88;

    private PreferenceGroup category;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        var context = requireContext();

        @SuppressLint("RestrictedApi")
        var screen = getPreferenceManager()
                .inflateFromResource(context, R.xml.pref_user_information_settings, null);
        category = Objects.requireNonNull((PreferenceGroup)
                screen.findPreference(getString(R.string.key_pref_category_accounts)));

        if (user != null) {
            var manageAccountsItem = new Preference(context);
            manageAccountsItem.setKey(getString(R.string.key_pref_manage_accounts));
            manageAccountsItem.setTitle(getString(R.string.title_preference_item_manage_accounts));
            manageAccountsItem.setIcon(R.drawable.ic_preferences_manage_accounts);
            manageAccountsItem.setIntent(new Intent(requireActivity(), PreferenceManageAccountsActivity.class));

            var signedInItem = new Preference(context);
            signedInItem.setKey(KEY_PREF_SIGNED_IN);
            signedInItem.setTitle(user.getDisplayName());
            signedInItem.setSummary(getString(R.string.title_preference_item_manage_accounts_summary));

            category.addPreference(signedInItem);
            category.addPreference(manageAccountsItem);
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

    @Deprecated
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

    public void setSignInIcon(Drawable resource) {
        Preference signedInPreference = category.findPreference(KEY_PREF_SIGNED_IN);
        if (signedInPreference != null) {
            if (resource != null) {
                Bitmap destBitmap;

                if (resource instanceof VectorDrawable) {
                    destBitmap = createBitmapFromDrawable(resource);
                } else if (resource instanceof ColorDrawable) {
                    destBitmap = createBitmapFromDrawable(resource);
                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(requireContext().getResources(), destBitmap);
                    rbd.setCornerRadius(Math.max(destBitmap.getHeight(), destBitmap.getWidth()) / 2.0f);
                    signedInPreference.setIcon(rbd);
                    return;
                } else {
                    Bitmap sourceBitmap = ((BitmapDrawable) resource).getBitmap();
                    destBitmap = Bitmap.createScaledBitmap(sourceBitmap, USER_AVATAR_WIDTH, USER_AVATAR_HEIGHT, true);
                }

                Drawable destDrawable = new BitmapDrawable(requireContext().getResources(), destBitmap);
                signedInPreference.setIcon(destDrawable);
            }
        }
    }

    private Bitmap createBitmapFromDrawable(Drawable d) {
        Bitmap destBitmap = Bitmap.createBitmap(USER_AVATAR_WIDTH, USER_AVATAR_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(destBitmap);

        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);

        return destBitmap;
    }
}
