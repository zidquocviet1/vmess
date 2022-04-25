package com.mqv.vmess.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.mqv.vmess.R;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.model.FriendRequest;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.FriendRequestStatus;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class RequestPeopleViewModel extends CurrentUserViewModel {
    private final FriendRequestRepository repository;
    private final MutableLiveData<Result<FriendRequestStatus>> friendRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Result<FriendRequest>> responseRequestResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Boolean>> requestConnectResult = new MutableLiveData<>();
    private final MutableLiveData<FriendRequestStatus> statusObserver = new MutableLiveData<>();
    private final DatabaseObserver.FriendRequestListener friendRequestListener;

    @Inject
    public RequestPeopleViewModel(FriendRequestRepository repository,
                                  SavedStateHandle savedStateHandle) {
        this.repository = repository;

        User user = savedStateHandle.get("user");

        if (user == null) {
            throw new IllegalArgumentException();
        }

        loadFirebaseUser();

        friendRequestListener = new DatabaseObserver.FriendRequestListener() {
            @Override
            public void onCancel(@NonNull String userId) {
                if (userId.equals(user.getUid())) {
                    statusObserver.postValue(FriendRequestStatus.CANCEL);
                }
            }

            @Override
            public void onRequest(@NonNull String userId) {
                if (userId.equals(user.getUid())) {
                    statusObserver.postValue(FriendRequestStatus.ACKNOWLEDGE);
                }
            }

            @Override
            public void onConfirm(@NonNull String userId) {
                if (userId.equals(user.getUid())) {
                    statusObserver.postValue(FriendRequestStatus.CONFIRM);
                }
            }

            @Override
            public void onUnfriend(@NonNull String userId) {
                if (userId.equals(user.getUid())) {
                    statusObserver.postValue(FriendRequestStatus.CANCEL);
                }
            }
        };

        AppDependencies.getDatabaseObserver().registerFriendRequestListener(friendRequestListener);
    }

    public LiveData<Result<FriendRequestStatus>> getFriendRequestStatus() {
        return friendRequestStatus;
    }

    public LiveData<Result<FriendRequest>> getResponseRequestResult() {
        return responseRequestResult;
    }

    public LiveData<Result<Boolean>> getRequestConnectResult() {
        return requestConnectResult;
    }

    public LiveData<FriendRequestStatus> getFriendStatusObserver() {
        return statusObserver;
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
                                responseRequestResult.setValue(Result.Success(request));
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

    @Override
    protected void onCleared() {
        super.onCleared();

        AppDependencies.getDatabaseObserver().unregisterFriendRequestListener(friendRequestListener);
    }
}
