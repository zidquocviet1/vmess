package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class RequestPeopleViewModel extends CurrentUserViewModel {
    private final FriendRequestRepository repository;
    private final MutableLiveData<Result<FriendRequestStatus>> friendRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> responseRequestResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> requestConnectResult = new MutableLiveData<>();

    @Inject
    public RequestPeopleViewModel(FriendRequestRepository repository) {
        this.repository = repository;

        loadFirebaseUser();
    }

    public LiveData<Result<FriendRequestStatus>> getFriendRequestStatus() {
        return friendRequestStatus;
    }

    public LiveData<Result<Boolean>> getResponseRequestResult() {
        return responseRequestResult;
    }

    public LiveData<Result<Boolean>> getRequestConnectResult() {
        return requestConnectResult;
    }

    public void getFriendRequestStatusByUid(String uid) {
        repository.findFriendRequestStatusByReceiverId(getFirebaseUser().getValue(), uid,
                observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            var code = response.getStatusCode();

                            if (code == HttpURLConnection.HTTP_OK) {
                                friendRequestStatus.setValue(Result.Success(response.getSuccess()));
                            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                friendRequestStatus.setValue(Result.Fail(R.string.error_authentication_fail));
                            }
                        }, t -> {
                            var message = t.getMessage();

                            if (message != null && message.equals("HTTP 404 ")) {
                                friendRequestStatus.setValue(Result.Fail(R.string.error_user_id_not_found));
                            } else
                                friendRequestStatus.setValue(Result.Fail(R.string.error_connect_server_fail));
                        })),
                e -> friendRequestStatus.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    public void responseFriendRequest(FriendRequest request) {
        responseRequestResult.setValue(Result.Loading());

        repository.responseFriendRequest(getFirebaseUser().getValue(), request,
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

    public void requestConnect(FriendRequest request) {
        requestConnectResult.setValue(Result.Loading());

        repository.requestConnect(getFirebaseUser().getValue(), request, observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            var code = response.getStatusCode();

                            if (code == HttpURLConnection.HTTP_CREATED) {
                                requestConnectResult.setValue(Result.Success(response.getSuccess()));
                            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                requestConnectResult.setValue(Result.Fail(R.string.error_authentication_fail));
                            } else if (code == HttpURLConnection.HTTP_CONFLICT) {
                                requestConnectResult.setValue(Result.Fail(R.string.error_response_request_conflict));
                            }
                        }, t -> {
                            var message = t.getMessage();

                            if (message != null && message.equals("HTTP 404 ")) {
                                requestConnectResult.setValue(Result.Fail(R.string.error_user_id_not_found));
                            } else
                                requestConnectResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                        })),
                e -> requestConnectResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }
}
