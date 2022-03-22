package com.mqv.vmess.activity.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mqv.vmess.data.repository.PeopleRepository
import com.mqv.vmess.network.model.User
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.ConversationOptionHandler
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.ConversationListFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.stream.Collectors
import javax.inject.Inject

@HiltViewModel
class UserSelectionListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val peopleRepository: PeopleRepository
) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val _userSuggestionList = MutableLiveData(mutableListOf<UserSelection>())

    private var whoCreateWith: UserSelection? = null

    init {
        val firstSelectedUser = savedStateHandle.get<User>(ConversationListFragment.EXTRA_USER)
        firstSelectedUser?.let { user ->
            with(user) {
                whoCreateWith = UserSelection(
                    uid, photoUrl, displayName,
                    isOnline = false,
                    isSelected = true
                )
                _userSuggestionList.postValue(mutableListOf(whoCreateWith!!))
            }
        }
        fetchSuggestedFriend()
    }

    val userSuggestionList: LiveData<MutableList<UserSelection>> get() = _userSuggestionList

    fun notifyUserSelect(item: UserSelection) {
        createNewList().let {
            val position = it.indexOf(item)
            val updatedUserSelection = with(item) {
                UserSelection(uid, photoUrl, displayName, isOnline, isSelected = !isSelected)
            }

            it[position] = updatedUserSelection

            _userSuggestionList.postValue(it)
        }
    }

    fun clearAllSelect() {
        _userSuggestionList.postValue(
            createNewList().stream()
                .map { u -> UserSelection(u.uid, u.photoUrl, u.displayName, u.isOnline, isSelected = false) }
                .collect(Collectors.toList())
        )
    }

    private fun createNewList(): MutableList<UserSelection> {
        val currentList = _userSuggestionList.value
        val updatedList = mutableListOf<UserSelection>().apply {
            currentList?.let { addAll(it) }
        }
        return updatedList
    }

    private fun fetchSuggestedFriend() {
        val disposable = peopleRepository.suggestionList
            .compose(RxHelper.applySingleSchedulers())
            .subscribe { data, _ ->
                val suggestionList = data.stream()
                    .map { people ->
                        with(people) {
                            return@with UserSelection(
                                uid,
                                photoUrl,
                                displayName,
                                isOnline = false,
                                isSelected = false
                            )
                        }
                    }
                    .collect(Collectors.toList())

                if (suggestionList.contains(whoCreateWith)) {
                    suggestionList.remove(whoCreateWith)
                }
                whoCreateWith?.let { suggestionList.add(0, whoCreateWith) }

                val groupMemberId = savedStateHandle.get<ArrayList<String>>(ConversationOptionHandler.EXTRA_GROUP_MEMBER_ID)
                groupMemberId?.let { ids ->
                    val groupMemberSelection = ids.stream()
                        .map { id -> UserSelection(id, null, "", isOnline = false, isSelected = false) }
                        .collect(Collectors.toList())
                    suggestionList.removeAll(groupMemberSelection)
                }
                _userSuggestionList.postValue(suggestionList)
            }

        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
