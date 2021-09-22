package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.HistoryLoggedInUserRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.Gender;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class EditDetailsViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final HistoryLoggedInUserRepository historyUserRepository;
    private final MutableLiveData<Result<User>> updateResult = new MutableLiveData<>();

    @Inject
    public EditDetailsViewModel(UserRepository userRepository, HistoryLoggedInUserRepository historyUserRepository) {
        this.userRepository = userRepository;
        this.historyUserRepository = historyUserRepository;

        loadFirebaseUser();
        loadLoggedInUser();
    }

    public void setFirebaseUser(FirebaseUser user) {
        firebaseUser.postValue(user);
    }

    public void resetUpdateResult() {
        updateResult.setValue(null);
    }

    public LiveData<String> getDisplayName() {
        return Transformations.map(getFirebaseUser(), FirebaseUser::getDisplayName);
    }

    public LiveData<Integer> getGenderAsKey() {
        return Transformations.map(getLoggedInUser(), user -> {
            if (user == null || user.getGender() == null)
                return -1;
            return user.getGender().getKey();
        });
    }

    public LiveData<LocalDateTime> getBirthday() {
        return Transformations.map(getLoggedInUser(), user -> {
            if (user == null || user.getBirthday() == null)
                return null;
            return user.getBirthday();
        });
    }

    public LiveData<Result<User>> getUpdateResult() {
        return updateResult;
    }

    public void updateUserGender(Gender newGender) {
        var user = getLoggedInUser().getValue();

        if (user != null) {
            var updateUserRequest = new User(user);
            updateUserRequest.setGender(newGender);

            updateRemoteUser(updateUserRequest);
        }
    }

    public void updateUserCurrentAddress(LocalDateTime newBirthday) {
        // TODO: not yet handled
    }

    public void updateUserHomeTown(LocalDateTime newBirthday) {
        // TODO: not yet handled
    }

    public void updateUserBirthday(LocalDateTime newBirthday) {
        var user = getLoggedInUser().getValue();

        if (user != null) {
            var updateUserRequest = new User(user);
            updateUserRequest.setBirthday(newBirthday);

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

    public void updateHistoryUserDisplayName(String uid, String newName) {
        cd.add(historyUserRepository.updateDisplayName(uid, newName)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Logging.show("Update history user display name successfully"),
                        Throwable::printStackTrace));
    }
}
