package com.mqv.realtimechatapplication.activity.viewmodel;

public class EditDetailsViewModel extends CurrentUserViewModel {
    public EditDetailsViewModel() {
        loadFirebaseUser();
        loadLoggedInUser();
    }
}
