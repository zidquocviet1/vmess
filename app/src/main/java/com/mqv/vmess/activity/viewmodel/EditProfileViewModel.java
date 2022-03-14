package com.mqv.vmess.activity.viewmodel;

import com.mqv.vmess.data.repository.UserRepository;

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
