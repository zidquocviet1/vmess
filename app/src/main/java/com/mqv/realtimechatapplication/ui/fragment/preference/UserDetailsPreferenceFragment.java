package com.mqv.realtimechatapplication.ui.fragment.preference;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.EditDetailsActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.EditProfileViewModel;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.firebase.FirebaseUserManager;
import com.mqv.realtimechatapplication.network.model.Gender;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserDetailsPreferenceFragment extends PreferenceFragmentCompat {
    EditProfileViewModel mViewModel;
    PreferenceScreen preferenceScreen;
    private static final DateTimeFormatter NORMAL_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final SparseIntArray GENDER_ARRAY = new SparseIntArray();

    static {
        GENDER_ARRAY.put(Gender.MALE.getKey(), R.drawable.ic_gender_male);
        GENDER_ARRAY.put(Gender.FEMALE.getKey(), R.drawable.ic_gender_female);
        GENDER_ARRAY.put(Gender.NON_BINARY.getKey(), R.drawable.ic_transgender);
        GENDER_ARRAY.put(Gender.TRANSGENDER.getKey(), R.drawable.ic_transgender);
        GENDER_ARRAY.put(Gender.INTERSEX.getKey(), R.drawable.ic_transgender);
        GENDER_ARRAY.put(Gender.PREFER_NOT_TO_SAY.getKey(), R.drawable.ic_not_interested);
    }

    private final LoggedInUserManager.LoggedInUserListener listener = user -> refreshPreferenceScreen();

    private final FirebaseUserManager.Listener firebaseUserChanged = this::refreshPreferenceScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(requireActivity()).get(EditProfileViewModel.class);

        super.onCreate(savedInstanceState);

        LoggedInUserManager.getInstance().addListener(listener);
        FirebaseUserManager.getInstance().addListener(firebaseUserChanged);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        preferenceScreen = getPreferenceManager().inflateFromResource(requireActivity(),
                R.xml.pref_user_edit_profile_details, null);

        inflatePreferences(preferenceScreen);
        setPreferenceScreen(preferenceScreen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var recyclerView = getListView();
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);
        recyclerView.setVerticalScrollBarEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LoggedInUserManager.getInstance().removeListener(listener);
        FirebaseUserManager.getInstance().removeListener(firebaseUserChanged);
    }

    private void inflatePreferences(PreferenceScreen preferenceScreen) {
        var firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            var prefName = createPreference(firebaseUser.getDisplayName(), R.drawable.ic_display_name);
            preferenceScreen.addPreference(prefName);

            var metadata = firebaseUser.getMetadata();
            if (metadata != null) {
                var joinedAtInstant = new Date(metadata.getCreationTimestamp()).toInstant();
                var joinedAtTitle = LocalDateTime.ofInstant(joinedAtInstant, ZoneId.systemDefault()).format(MONTH_YEAR_FORMATTER);
                var prefCreatedAt = createPreference("Joined " + joinedAtTitle, R.drawable.ic_watch_later);
                preferenceScreen.addPreference(prefCreatedAt);
            }
        }

        var user = LoggedInUserManager.getInstance().getLoggedInUser();
        if (user != null) {
            var genderType = user.getGender();
            if (genderType != null) {
                var genderIcon = GENDER_ARRAY.get(genderType.getKey());
                var prefGender = createPreference(genderType.getValue(), genderIcon);
                preferenceScreen.addPreference(prefGender);
            }

            var birthday = user.getBirthday();
            if (birthday != null) {
                var birthdayTitle = birthday.format(NORMAL_FORMATTER);
                var prefBirthday = createPreference(birthdayTitle, R.drawable.ic_birthday);
                preferenceScreen.addPreference(prefBirthday);
            }

//            var currentAddress = user.getCurrentAddress();
//            if (birthday != null){
//                var birthdayTitle = birthday.format(NORMAL_FORMATTER);
//                var prefBirthday = createPreference(birthdayTitle, R.drawable.ic_birthday);
//                preferenceScreen.addPreference(prefBirthday);
//            }
//
//            var birthday = user.getBirthday();
//            if (birthday != null){
//                var birthdayTitle = birthday.format(NORMAL_FORMATTER);
//                var prefBirthday = createPreference(birthdayTitle, R.drawable.ic_birthday);
//                preferenceScreen.addPreference(prefBirthday);
//            }
        }
    }

    private Preference createPreference(String title, int icon) {
        var pref = new Preference(requireActivity());
        pref.setTitle(title);
        pref.setIcon(icon);
        pref.setLayoutResource(R.layout.item_preference_content);
        pref.setIntent(new Intent(requireActivity(), EditDetailsActivity.class));
        return pref;
    }

    private void refreshPreferenceScreen(){
        requireActivity().runOnUiThread(() -> {
            setPreferenceScreen(null);
            var preferenceScreen = getPreferenceManager().createPreferenceScreen(requireContext());
            inflatePreferences(preferenceScreen);
            setPreferenceScreen(preferenceScreen);
        });
    }
}
