package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.FriendRequest;

import java.net.HttpURLConnection;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class FriendRequestViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<List<FriendRequest>>> friendRequestList = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> responseRequestResult = new MutableLiveData<>();
    private final FriendRequestRepository repository;

    @Inject
    public FriendRequestViewModel(FriendRequestRepository repository) {
        this.repository = repository;

        loadPendingFriendRequest();
    }

    public LiveData<Result<List<FriendRequest>>> getFriendRequestList() {
        return friendRequestList;
    }

    public LiveData<Result<Boolean>> getResponseRequestResult() {
        return responseRequestResult;
    }

    private void loadPendingFriendRequest() {
        friendRequestList.setValue(Result.Loading());

        repository.getAllPendingRequest(observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                friendRequestList.setValue(Result.Success(response.getSuccess()));
                            } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                friendRequestList.setValue(Result.Fail(R.string.error_authentication_fail));
                            }
                        }, t -> friendRequestList.setValue(Result.Fail(R.string.error_connect_server_fail)))),
                e -> friendRequestList.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    public void responseFriendRequest(FriendRequest request) {
        responseRequestResult.setValue(Result.Loading());

        repository.responseFriendRequest(request,
                observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            var code = response.getStatusCode();

                            if (code == HttpURLConnection.HTTP_OK) {
                                responseRequestResult.setValue(Result.Success(response.getSuccess()));
                            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                responseRequestResult.setValue(Result.Fail(R.string.error_authentication_fail));
                            } else if (code == HttpURLConnection.HTTP_CONFLICT) {
                                responseRequestResult.setValue(Result.Fail(R.string.error_response_request_conflict));
                            }
                        }, t -> {
                            var message = t.getMessage();

                            if (message != null && message.equals("HTTP 404 ")) {
                                responseRequestResult.setValue(Result.Fail(R.string.error_user_id_not_found));
                            } else
                                responseRequestResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                        })),
                e -> responseRequestResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }
}
