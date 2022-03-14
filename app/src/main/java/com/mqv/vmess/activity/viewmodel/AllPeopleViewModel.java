package com.mqv.vmess.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.ConversationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.Logging;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class AllPeopleViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<Boolean>> unfriendResult = new MutableLiveData<>();
    private final PeopleRepository peopleRepository;
    private final ConversationRepository conversationRepository;

    @Inject
    public AllPeopleViewModel(PeopleRepository peopleRepository,
                              ConversationRepository conversationRepository) {
        this.peopleRepository = peopleRepository;
        this.conversationRepository = conversationRepository;
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
                            }
                        }, t -> unfriendResult.setValue(Result.Fail(R.string.error_connect_server_fail)))
                ), e -> unfriendResult.setValue(Result.Fail(R.string.error_authentication_fail)));
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
