package com.mqv.vmess.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.AbstractMainViewModel;
import com.mqv.vmess.data.model.FriendNotification;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.network.exception.FirebaseUnauthorizedException;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.data.FriendNotificationMapper;
import com.mqv.vmess.ui.data.FriendNotificationState;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Event;
import com.mqv.vmess.util.LiveDataUtil;
import com.mqv.vmess.util.Logging;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class NotificationFragmentViewModel extends AbstractMainViewModel {
    private final NotificationRepository notificationRepository;

    private final MutableLiveData<List<FriendNotification>>         friendNotificationListResult = new MutableLiveData<>();
    private final MutableLiveData<Result<List<FriendNotification>>> refreshResult                = new MutableLiveData<>();
    private final MutableLiveData<List<People>>                     userList                     = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>>                   oneTimeEvent                 = new MutableLiveData<>();

    @Inject
    public NotificationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.notificationRepository = notificationRepository;

        //noinspection ResultOfMethodCallIgnored
        notificationRepository.observeFriendNotification()
                              .compose(RxHelper.applyFlowableSchedulers())
                              .subscribe(friendNotificationListResult::postValue, t ->{});

        //noinspection ResultOfMethodCallIgnored
        peopleRepository.getAll()
                        .compose(RxHelper.applyFlowableSchedulers())
                        .subscribe(userList::postValue, t -> Logging.show(t.getMessage()));
    }

    @Override
    public void onRefresh() {
        Disposable disposable = notificationRepository.fetchNotification(1)
                                                      .startWith(Completable.fromAction(() -> refreshResult.postValue(Result.Loading())))
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .doOnDispose(() -> refreshResult.postValue(Result.Terminate()))
                                                      .subscribe(data -> notificationRepository.saveCachedNotification(data)
                                                                                               .andThen(Completable.fromAction(() ->
                                                                                                       refreshResult.postValue(Result.Success(data))))
                                                                                               .subscribe(),
                                                      t -> {
                                                          if (t instanceof FirebaseUnauthorizedException) {
                                                              oneTimeEvent.setValue(new Event<>(((FirebaseUnauthorizedException) t).getError()));
                                                          } else if (t instanceof FirebaseNetworkException) {
                                                              oneTimeEvent.setValue(new Event<>(R.string.error_network_connection));
                                                          } else if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
                                                              oneTimeEvent.setValue(new Event<>(R.string.error_connect_server_fail));
                                                          } else {
                                                              oneTimeEvent.setValue(new Event<>(R.string.error_unknown));
                                                          }
                                                          refreshResult.setValue(Result.Fail(-1));
                                                      });

        cd.add(disposable);
    }

    public LiveData<List<FriendNotificationState>> getListFriendNotificationState() {
        return Transformations.map(LiveDataUtil.zip(friendNotificationListResult, userList), pair -> {
            if (pair == null) {
                return new ArrayList<>();
            }
            List<FriendNotification> fn = pair.first;
            List<People> peopleList = pair.second;

            return fn.stream()
                     .map(fn2 -> FriendNotificationMapper.fromFriendNotification(fn2, peopleList))
                     .collect(Collectors.toList());
        });
    }

    public LiveData<Result<List<FriendNotification>>> getRefreshResultSafe() {
        return refreshResult;
    }

    public LiveData<Event<Integer>> getOneTimeEvent() {
        return oneTimeEvent;
    }

    public void markAsRead(FriendNotificationState item) {
        Disposable disposable = notificationRepository.markAsRead(FriendNotificationMapper.fromFriendNotificationState(item))
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .subscribe(data -> {}, t -> {});
        cd.add(disposable);
    }

    public void removeNotification(FriendNotificationState item) {
        Disposable disposable = notificationRepository.removeNotification(FriendNotificationMapper.fromFriendNotificationState(item))
                                                      .compose(RxHelper.applyObservableSchedulers())
                                                      .compose(RxHelper.parseResponseData())
                                                      .subscribe(data -> {}, t -> {});
        cd.add(disposable);
    }
}
