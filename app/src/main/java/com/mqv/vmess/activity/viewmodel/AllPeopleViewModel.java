package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.reactive.RxHelper;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Logging;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class AllPeopleViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<Boolean>> unfriendResult = new MutableLiveData<>();
    private final MutableLiveData<List<People>>    peopleList     = new MutableLiveData<>();

    private final PeopleRepository       peopleRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationRepository notificationRepository;

    @Inject
    public AllPeopleViewModel(PeopleRepository       peopleRepository,
                              ConversationRepository conversationRepository,
                              NotificationRepository notificationRepository) {
        this.peopleRepository       = peopleRepository;
        this.conversationRepository = conversationRepository;
        this.notificationRepository = notificationRepository;

        loadFriendPeople();
    }

    private void loadFriendPeople() {
        //noinspection ResultOfMethodCallIgnored
        peopleRepository.getAll()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(people -> peopleList.postValue(people.stream()
                                                       .filter(People::getFriend)
                                                       .collect(Collectors.toList())),
                                   t -> this.peopleList.setValue(new ArrayList<>()));
    }

    public LiveData<List<People>> getPeopleListObserver() {
        return peopleList;
    }

    public LiveData<Result<Boolean>> getUnfriendResult() {
        return unfriendResult;
    }

    public void unfriend(String userId, People people) {
        unfriendResult.setValue(Result.Loading());

        peopleRepository.unfriend(people.getUid(),
                observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                unfriendResult.setValue(Result.Success(response.getSuccess()));

                                deleteCachePeople(people);
                                deleteCacheConversation(userId, people.getUid());
                                deleteNotificationRelatedToUser(people.getUid());
                            }
                        }, t -> unfriendResult.setValue(Result.Fail(R.string.error_connect_server_fail)))
                ), e -> unfriendResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    private void deleteNotificationRelatedToUser(String userId) {
        Disposable disposable = notificationRepository.fetchAllNotificationRelatedToUser(userId)
                                                      .flatMapObservable(Observable::fromIterable)
                                                      .flatMap(notificationRepository::removeNotification)
                                                      .observeOn(Schedulers.io())
                                                      .subscribeOn(Schedulers.io())
                                                      .compose(RxHelper.parseResponseData())
                                                      .onErrorComplete()
                                                      .subscribe();
        cd.add(disposable);
    }

    private void deleteCachePeople(People people) {
        cd.add(peopleRepository.delete(people)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Logging.show("Delete people successfully")));
    }

    private void deleteCacheConversation(String userId, String otherUserId) {
        cd.add(Completable.fromAction(() -> conversationRepository.deleteByParticipantId(userId, otherUserId))
                          .subscribeOn(Schedulers.io())
                          .observeOn(Schedulers.io())
                          .subscribe());
    }
}
