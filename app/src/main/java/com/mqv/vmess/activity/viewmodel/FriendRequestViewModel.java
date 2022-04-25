package com.mqv.vmess.activity.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.vmess.R;
import com.mqv.vmess.data.DatabaseObserver;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.network.model.FriendRequest;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.util.Logging;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Retriever;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class FriendRequestViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<List<FriendRequest>>> friendRequestList = new MutableLiveData<>();
    private final MutableLiveData<Result<FriendRequest>> responseRequestResult = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> connectUserResult = new MutableLiveData<>();
    private final FriendRequestRepository repository;
    private final UserRepository userRepository;
    private final PeopleRepository peopleRepository;
    private final NotificationRepository notificationRepository;
    private final DatabaseObserver.NoneFriendRequestListener listener;

    @Inject
    public FriendRequestViewModel(FriendRequestRepository repository,
                                  UserRepository userRepository,
                                  PeopleRepository peopleRepository,
                                  NotificationRepository notificationRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.peopleRepository = peopleRepository;
        this.notificationRepository = notificationRepository;

        loadFirebaseUser();
        loadPendingFriendRequest();

        listener = new DatabaseObserver.NoneFriendRequestListener() {
            @Override
            public void onUnfriend(@NonNull String userId) {
            }

            @Override
            public void onRequest(@NonNull String userId) {
                addFriendRequest(userId);
            }

            @Override
            public void onConfirm(@NonNull String userId) {
                removeFriendRequest(userId);
            }

            @Override
            public void onCancel(@NonNull String userId) {
                removeFriendRequest(userId);
            }
        };

        AppDependencies.getDatabaseObserver().registerNoneFriendRequestListener(listener);
    }

    public LiveData<Result<List<FriendRequest>>> getFriendRequestList() {
        return friendRequestList;
    }

    public LiveData<Result<FriendRequest>> getResponseRequestResult() {
        return responseRequestResult;
    }

    public LiveData<Result<User>> getConnectUserResult() {
        return connectUserResult;
    }

    private void loadPendingFriendRequest() {
        friendRequestList.setValue(Result.Loading());

        repository.getAllPendingRequest(getFirebaseUser().getValue(),
                observable -> cd.add(observable
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

    public void getConnectUserByUid(String uid) {
        connectUserResult.setValue(Result.Loading());

        userRepository.getConnectUserByUid(getFirebaseUser().getValue(),
                uid,
                observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            var code = response.getStatusCode();

                            if (code == HttpURLConnection.HTTP_OK) {
                                connectUserResult.setValue(Result.Success(response.getSuccess()));
                            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail));
                            }
                        }, t -> {
                            var message = t.getMessage();

                            if (message != null && message.equals("HTTP 404 ")) {
                                connectUserResult.setValue(Result.Fail(R.string.error_user_id_not_found));
                            } else
                                connectUserResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                        })),
                e -> connectUserResult.setValue(Result.Fail(R.string.error_authentication_fail))
        );
    }

    public void confirmFriendRequest(String uid) {
        //noinspection ResultOfMethodCallIgnored
        peopleRepository.getConnectPeopleByUid(uid)
                        .compose(RxHelper.parseResponseData())
                        .flatMapCompletable(people -> {
                            people.setFriend(true);
                            return peopleRepository.save(people);
                        })
                        .subscribeOn(Schedulers.io())
                        .onErrorComplete()
                        .subscribe(() -> Logging.show("Insert new people into database successfully"));
    }

    // Remove request notification whenever the user accept or cancel request.
    public void removeRequestNotification(String senderId) {
        Disposable disposable = notificationRepository.fetchRequestNotificationBySenderId(senderId)
                                                      .flatMapObservable(notificationRepository::removeNotification)
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .onErrorComplete()
                                                      .subscribe();

        cd.add(disposable);
    }

    private List<FriendRequest> getCurrentList() {
        Result<List<FriendRequest>> result = friendRequestList.getValue();

        if (result != null) {
            if (result.getStatus() == NetworkStatus.SUCCESS) {
                return Retriever.getOrDefault(result.getSuccess(), new ArrayList<>());
            }
            return null;
        }
        return null;
    }

    private void removeFriendRequest(String userId) {
        List<FriendRequest> list = getCurrentList();

        if (list != null) {
            FriendRequest exists = list.stream()
                                       .filter(request -> request.getSenderId().equals(userId))
                                       .findAny()
                                       .orElse(null);

            list.remove(exists);

            friendRequestList.postValue(Result.Success(list));
        }
    }

    private void addFriendRequest(String userId) {
        Result<List<FriendRequest>> result = friendRequestList.getValue();

        if (result != null && result.getStatus() != NetworkStatus.LOADING) {
            repository.getAllPendingRequest(getFirebaseUser().getValue(),
                    observable -> cd.add(observable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(response -> {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    friendRequestList.setValue(Result.Success(response.getSuccess()));
                                }
                            }, t-> {})),
                    e -> {});
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        AppDependencies.getDatabaseObserver().unregisterNoneFriendRequestListener(listener);
    }
}
