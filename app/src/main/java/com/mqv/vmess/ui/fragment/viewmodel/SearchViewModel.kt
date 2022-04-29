package com.mqv.vmess.ui.fragment.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mqv.vmess.data.repository.SearchRepository
import com.mqv.vmess.data.result.Result
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.ui.data.SearchResultState
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.Logging
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val TAG = SearchViewModel::class.java.simpleName

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val searchSubject: PublishSubject<String> = PublishSubject.create()
    private val _searchResult = MutableLiveData<Result<MutableList<People>>?>()
    private val _historyResult = linkedMapOf<String, SearchResultState<MutableList<People>>>()

    private var searchDisposable: Disposable? = null

    val searchResult: LiveData<Result<MutableList<People>>?> get() = _searchResult

    init {
        searchSubject
            .debounce(300, TimeUnit.MILLISECONDS)
            .switchMap { search ->
                Observable.create<String> { emitter ->
                    if (!emitter.isDisposed) {
                        emitter.onNext(search)
                    }
                }
            }
            .compose(RxHelper.applyObservableSchedulers())
            .subscribe {
                Logging.debug(
                    TAG,
                    "Make remote call the find user with the display name contain: $it"
                )

                checkForRemoteCall(it)
            }
    }

    fun requestSearchName(name: String, forceStart: Boolean) {
        Logging.debug(TAG, "Receive search request with name = $name and force start = $forceStart")

        if (forceStart) {
            checkForRemoteCall(name)
        } else {
            searchSubject.onNext(name)
        }
    }

    fun resetSearchResult() {
        _searchResult.postValue(null)
    }

    private fun checkForRemoteCall(name: String) {
        if (_historyResult.containsKey(name)) {
            val state = _historyResult[name]
            if (state != null && state.searchTime > LocalDateTime.now().toLong() - 60) {
                Logging.debug(
                    TAG,
                    "The query name has been recorded in the memory, then return result instead of make a remote call."
                )
                _searchResult.postValue(state.result)
            } else {
                makeCall(name)
            }
        } else {
            makeCall(name)
        }
    }

    private fun makeCall(name: String) {
        searchDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }

        searchDisposable = searchRepository.searchPeople(name)
            .startWith(Completable.fromAction { _searchResult.postValue(Result.Loading()) })
            .compose(RxHelper.applyFlowableSchedulers())
            .subscribe({ people ->
                if (people.isEmpty()) {
                    val value = Result.Fail<MutableList<People>>(-1)
                    _searchResult.postValue(value)
                    _historyResult[name] = SearchResultState(value)
                } else {
                    val value = Result.Success(people.toMutableList())
                    _searchResult.postValue(value)
                    _historyResult[name] = SearchResultState(value)
                }
            }, { _ ->
                val value = Result.Fail<MutableList<People>>(-1)
                _searchResult.postValue(value)
                _historyResult[name] = SearchResultState(value)
            })

        compositeDisposable.add(searchDisposable!!)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}