package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class AllPeopleViewModel extends CurrentUserViewModel {
    private final MutableLiveData<Result<Boolean>> unfriendResult = new MutableLiveData<>();
    private final PeopleRepository peopleRepository;

    @Inject
    public AllPeopleViewModel(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }

    public LiveData<Result<Boolean>> getUnfriendResult() {
        return unfriendResult;
    }

    public void unfriend(People people) {
        unfriendResult.setValue(Result.Loading());

        peopleRepository.unfriend(people.getUid(),
                observable -> cd.add(observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                unfriendResult.setValue(Result.Success(response.getSuccess()));

                                deleteCachePeople(people);
                            }
                        }, t -> unfriendResult.setValue(Result.Fail(R.string.error_connect_server_fail)))
                ), e -> unfriendResult.setValue(Result.Fail(R.string.error_authentication_fail)));
    }

    private void deleteCachePeople(People people) {
        cd.add(peopleRepository.delete(people)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Logging.show("Delete people successfully")));
    }
}
