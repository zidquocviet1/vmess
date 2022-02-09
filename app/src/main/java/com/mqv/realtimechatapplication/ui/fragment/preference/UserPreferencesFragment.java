package com.mqv.realtimechatapplication.ui.fragment.preference;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.preferences.PreferenceManageAccountsActivity;
import com.mqv.realtimechatapplication.util.Picture;

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
        category = Objects.requireNonNull(screen.findPreference(getString(R.string.key_pref_category_accounts)));

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
                    destBitmap = Picture.createBitmapFromDrawable(resource, Picture.DEFAULT_IMAGE_WIDTH, Picture.DEFAULT_IMAGE_HEIGHT);
                } else if (resource instanceof ColorDrawable) {
                    signedInPreference.setIcon(Picture.createRoundedDrawable(
                            requireContext(),
                            resource,
                            Picture.DEFAULT_IMAGE_WIDTH,
                            Picture.DEFAULT_IMAGE_HEIGHT
                    ));
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
}
