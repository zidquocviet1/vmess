package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.UserSocialLink;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class EditProfileLinkViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<User>> updateResult = new MutableLiveData<>();
    private final UserRepository userRepository;

    @Inject
    public EditProfileLinkViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
        loadLoggedInUser();
        loadFirebaseUser();
    }

    public LiveData<List<UserSocialLink>> getUserSocialLinkList() {
        return Transformations.map(getLoggedInUser(), user -> {
            if (user == null || user.getSocialLinks() == null)
                return null;
            return user.getSocialLinks()
                    .stream()
                    .map(u -> new UserSocialLink(u.getId(), u.getType(), u.getAccountName()))
                    .collect(Collectors.toList());
        });
    }

    public LiveData<Result<User>> getUpdateResult() {
        return updateResult;
    }

    public void updateUserSocialLink(List<UserSocialLink> links) {
        var user = getLoggedInUser().getValue();

        if (user != null) {
            var updateUserRequest = new User(
                    user.getUid(),
                    user.getBiographic(),
                    user.getGender(),
                    user.getBirthday(),
                    user.getCreatedDate(),
                    user.getModifiedDate(),
                    user.getAccessedDate(),
                    links);

            updateRemoteUser(updateUserRequest);
        }
    }

    private void updateRemoteUser(@NonNull User updateUserRequest) {
        var firebaseUser = getFirebaseUser().getValue();

        if (firebaseUser != null) {
            updateResult.setValue(Result.Loading());

            userRepository.editUser(updateUserRequest,
                    firebaseUser,
                    observable -> cd.add(observable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                var code = response.getStatusCode();

                                if (code == HttpURLConnection.HTTP_OK) {
                                    saveCallResult(response.getSuccess());
                                } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    updateResult.setValue(Result.Fail(R.string.error_authentication_fail));
                                } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                                    updateResult.setValue(Result.Fail(R.string.error_user_id_not_found));
                                }
                            }, t -> updateResult.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                    e -> updateResult.setValue(Result.Fail(R.string.error_authentication_fail)));
        } else {
            updateResult.setValue(Result.Fail(R.string.error_user_id_not_found));
        }
    }

    private void saveCallResult(User user) {
        cd.add(userRepository.addUserToDb(user)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> updateResult.setValue(Result.Success(user)),
                        t -> Logging.show("Insert user fail with id = " + user.getUid()))
        );
    }
}
