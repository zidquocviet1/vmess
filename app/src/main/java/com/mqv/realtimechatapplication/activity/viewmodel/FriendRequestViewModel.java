package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Arrays;
import java.util.List;

public class FriendRequestViewModel extends CurrentUserViewModel {
    private final MutableLiveData<List<FriendRequest>> friendRequestList = new MutableLiveData<>();

    public FriendRequestViewModel() {
        loadFirebaseUser();

        loadPendingFriendRequest();
    }

    public LiveData<List<FriendRequest>> getFriendRequestList() {
        return friendRequestList;
    }

    private void loadPendingFriendRequest() {
        var list = Arrays.asList(
                new FriendRequest(1L, "abc", "bcd", Const.DUMMIES_IMAGES_URL[0], "Messika", FriendRequestStatus.PENDING),
                new FriendRequest(2L, "abcf", "bcd", Const.DUMMIES_IMAGES_URL[1], "Cristiano Ronaldo", FriendRequestStatus.PENDING),
                new FriendRequest(3L, "abcd", "bcd", Const.DUMMIES_IMAGES_URL[2], "Cúc Tịnh Y", FriendRequestStatus.PENDING),
                new FriendRequest(4L, "abce", "bcd", Const.DUMMIES_IMAGES_URL[3], "David Beckham", FriendRequestStatus.PENDING),
                new FriendRequest(5L, "abcg", "bcd", Const.DUMMIES_IMAGES_URL[4], "Địch Lệ Nhiệt Ba", FriendRequestStatus.PENDING),
                new FriendRequest(6L, "abch", "bcd", Const.DUMMIES_IMAGES_URL[5], "Cúc Tịnh YY", FriendRequestStatus.PENDING),
                new FriendRequest(7L, "abcn", "bcd", Const.DUMMIES_IMAGES_URL[6], "Toni Kross", FriendRequestStatus.PENDING),
                new FriendRequest(8L, "abchd", "bcd", Const.DUMMIES_IMAGES_URL[7], "Messika", FriendRequestStatus.PENDING),
                new FriendRequest(9L, "abcfdfgw", "bcd", Const.DUMMIES_IMAGES_URL[8], "Cristiano Ronaldo", FriendRequestStatus.PENDING),
                new FriendRequest(10L, "abcdhw", "bcd", Const.DUMMIES_IMAGES_URL[9], "Cúc Tịnh Y", FriendRequestStatus.PENDING),
                new FriendRequest(11L, "abceh", "bcd", Const.DUMMIES_IMAGES_URL[10], "David Beckham", FriendRequestStatus.PENDING));
        friendRequestList.setValue(list);
    }
}
