package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Logging;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class UsernameViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;

    @Inject
    public UsernameViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;

        loadLoggedInUser();
        loadFirebaseUser();
    }

    public LiveData<String> getUsername(){
        return Transformations.map(getLoggedInUser(), User::getBiographic);
    }

    public void editUsername(String username){
        var user = getLoggedInUser().getValue();
        var firebaseUser = getFirebaseUser().getValue();

        if (user != null && firebaseUser != null) {
            var updatedUser = new User(user);
            updatedUser.setBiographic("bkasjldkbjsdk");
            Logging.show("Old bio user = " + user.getBiographic() + " New bio user = " + updatedUser.getBiographic());
        }
    }
}
