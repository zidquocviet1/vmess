package com.mqv.realtimechatapplication.activity.viewmodel;

import com.mqv.realtimechatapplication.data.repository.UserRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditProfileViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;

    @Inject
    public EditProfileViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;

        loadFirebaseUser();
        loadLoggedInUser();
    }
}
