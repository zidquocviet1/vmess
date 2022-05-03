package com.mqv.vmess.activity.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.room.rxjava3.EmptyResultSetException
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.activity.AddGroupConversationActivity
import com.mqv.vmess.data.model.RecentSearchPeople
import com.mqv.vmess.data.repository.ChatRepository
import com.mqv.vmess.data.repository.ConversationRepository
import com.mqv.vmess.data.repository.PeopleRepository
import com.mqv.vmess.data.repository.SearchRepository
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.manager.LoggedInUserManager
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.MessageType
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.reactive.RxHelper.applyCompleteSchedulers
import com.mqv.vmess.ui.ConversationOptionHandler
import com.mqv.vmess.ui.data.ConversationMapper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.ui.data.RecentSearch
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.MessageUtil
import com.mqv.vmess.work.SendMessageWorkWrapper
import com.mqv.vmess.work.WorkDependency
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.inject.Inject
import kotlin.streams.toList

@HiltViewModel
class UserSelectionListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: ChatRepository,
    private val recentSearchRepository: SearchRepository,
    application: Application
) : AndroidViewModel(application) {
    private val mCurrentUser = FirebaseAuth.getInstance().currentUser!!
    private val compositeDisposable = CompositeDisposable()
    private val _userSuggestionList = MutableLiveData(mutableListOf<UserSelection>())
    private val _userRecentSearchList = MutableLiveData(mutableListOf<RecentSearch>())

    private var whoCreateWith: UserSelection? = null
    private var messageToSend: Chat? = null

    init {
        val firstSelectedUser = savedStateHandle.get<User>(AddGroupConversationActivity.EXTRA_USER)
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
        fetchSuggestedFriend(
            savedStateHandle.get<Boolean>(SuggestionFriendListFragment.ARG_INCLUDE_GROUP) ?: false
        )
        fetchRecentSearchList()

        messageToSend = savedStateHandle.get(SuggestionFriendListFragment.ARG_MESSAGE_TO_SEND)

        AppDependencies.getWebSocket().presenceUserList
            .delay(2, TimeUnit.SECONDS)
            .compose(RxHelper.applyObservableSchedulers())
            .subscribe { presence ->
                val suggestion = _userSuggestionList.value
                val recentSearch = _userRecentSearchList.value

                suggestion?.let {
                    _userSuggestionList.postValue(
                        it.stream().peek { selection ->
                            selection.isOnline = presence.contains(selection.uid)
                        }.toList().toMutableList()
                    )
                }

                recentSearch?.let {
                    _userRecentSearchList.postValue(
                        it.stream().peek { rs -> rs.isOnline = presence.contains(rs.uid) }
                            .toList()
                            .toMutableList()
                    )
                }
            }
    }

    val userSuggestionList: LiveData<MutableList<UserSelection>> get() = _userSuggestionList
    val userRecentSearchList: LiveData<MutableList<RecentSearch>> get() = _userRecentSearchList

    fun notifyUserSelect(item: UserSelection) {
        createNewList().let {
            if (whoCreateWith != null && whoCreateWith!!.uid == item.uid) {
                return@let
            } else {
                val position = it.indexOf(item)
                val updatedUserSelection = with(item) {
                    UserSelection(uid, photoUrl, displayName, isOnline, isSelected = !isSelected)
                }

                it[position] = updatedUserSelection

                _userSuggestionList.postValue(it)
            }
        }
    }

    fun clearAllSelect() {
        _userSuggestionList.postValue(
            createNewList().stream()
                .map { u ->
                    UserSelection(
                        u.uid,
                        u.photoUrl,
                        u.displayName,
                        u.isOnline,
                        isSelected = false
                    )
                }
                .collect(Collectors.toList())
        )
    }

    fun sendMessage(context: Context, selection: UserSelection) {
        messageToSend?.let {
            if (selection.isConversation) {
                val conversationId = selection.uid
                val message = createReplicaMessage(it, conversationId)

                val disposable = conversationRepository.isExists(conversationId)
                    .flatMapCompletable { isExists ->
                        if (isExists) {
                            messageRepository.saveCached(message)
                        } else Completable.error(
                            EmptyResultSetException("empty")
                        )
                    }
                    .compose(applyCompleteSchedulers())
                    .subscribe {
                        AppDependencies.getDatabaseObserver()
                            .notifyConversationUpdated(conversationId)
                        AppDependencies.getDatabaseObserver()
                            .notifyMessageInserted(message.conversationId, message.id)

                        sendMessage(context, message.id)
                    }

                compositeDisposable.add(disposable)
            } else {
                Logging.show("Can not send message to undefined conversation")
            }
        }
    }

    private fun createReplicaMessage(message: Chat, conversationId: String): Chat {
        val builder = Chat.builder()
            .setSenderId(mCurrentUser.uid)
            .setConversationId(conversationId)
            .setType(MessageType.GENERIC)

        when {
            MessageUtil.isShareMessage(message) -> {
                builder.withShare(message.share)
                builder.setContent(message.content)
                builder.setType(MessageType.SHARE)
            }
            MessageUtil.isVideoMessage(message) -> builder.withVideos(message.videos)
            MessageUtil.isFileMessage(message) -> builder.withFile(message.files)
            MessageUtil.isPhotoMessage(message) -> builder.withPhoto(message.photos)
            MessageUtil.isCallMessage(message) -> {
                builder.setType(MessageType.CALL)
            }
            else -> {
                builder.setContent(message.content)
            }
        }

        return builder.create()
    }

    private fun sendMessage(context: Context, messageId: String) {
        val input = Data.Builder()
            .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_ID, messageId)
            .putBoolean(SendMessageWorkWrapper.EXTRA_FORWARD_MESSAGE, true)
            .build()

        WorkDependency.enqueue(SendMessageWorkWrapper(context, input))
    }

    private fun createNewList(): MutableList<UserSelection> {
        val currentList = _userSuggestionList.value
        val updatedList = mutableListOf<UserSelection>().apply {
            currentList?.let { addAll(it) }
        }
        return updatedList
    }

    private fun fetchSuggestedFriend(isIncludeConversation: Boolean) {
        if (isIncludeConversation) {
            fetchSuggestionListIncludeConversation()

        } else {
            fetchSuggestionListWithoutConversation()
        }
    }

    private fun fetchSuggestionListWithoutConversation() {
        val disposable = peopleRepository.suggestionList
            .compose(RxHelper.applySingleSchedulers())
            .subscribe { data, _ ->
                val suggestionList = mapPeopleToUserSelection(data)

                if (suggestionList.contains(whoCreateWith)) {
                    suggestionList.remove(whoCreateWith)
                }
                whoCreateWith?.let { suggestionList.add(0, it) }

                val groupMemberId =
                    savedStateHandle.get<ArrayList<String>>(ConversationOptionHandler.EXTRA_GROUP_MEMBER_ID)
                groupMemberId?.let { ids ->
                    val groupMemberSelection = ids.stream()
                        .map { id ->
                            UserSelection(
                                id,
                                null,
                                "",
                                isOnline = false,
                                isSelected = false
                            )
                        }
                        .collect(Collectors.toList())
                    suggestionList.removeAll(groupMemberSelection)
                }
                _userSuggestionList.postValue(suggestionList)
            }

        compositeDisposable.add(disposable)
    }

    private fun fetchSuggestionListIncludeConversation() {
        val disposable = peopleRepository.suggestionList
            .zipWith(conversationRepository.suggestConversation(10)) { peopleList, conversationList ->
                val conversation = conversationList.stream()
                    .map { conversation ->
                        with(conversation) {
                            return@with UserSelection(
                                id,
                                null,
                                "",
                                isOnline = false,
                                isSelected = false,
                                isConversation = true,
                                isGroup = group != null,
                                conversationMetadata = ConversationMapper.mapToMetadata(
                                    conversation,
                                    LoggedInUserManager.getInstance()
                                        .parseFirebaseUser(mCurrentUser),
                                    getApplication()
                                )
                            )
                        }
                    }
                    .collect(Collectors.toList())

                return@zipWith conversation
            }
            .compose(RxHelper.applySingleSchedulers())
            .subscribe { data, _ ->
                _userSuggestionList.postValue(data)
            }
        compositeDisposable.add(disposable)
    }

    private fun mapPeopleToUserSelection(peopleList: List<People>): MutableList<UserSelection> =
        peopleList.stream()
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

    private fun fetchRecentSearchList() {
        compositeDisposable.add(
            recentSearchRepository.getAll()
                .flatMapObservable { Observable.fromIterable(it) }
                .concatMapDelayError { search ->
                    Observable.just(search)
                        .zipWith(peopleRepository.getCachedByUid(search.userId).filter { it.friend }
                            .toObservable()) { recentSearch, people ->
                            RecentSearch(
                                people.uid,
                                people.photoUrl,
                                people.displayName,
                                isOnline = false,
                                recentSearch.timestamp
                            )
                        }
                }
                .toList()
                .doOnError { Logging.show("Cannot fetch recent search people because: ${it.message}") }
                .onErrorReturnItem(mutableListOf())
                .subscribeOn(Schedulers.io())
                .subscribe { data ->
                    _userRecentSearchList.postValue(
                        data.stream()
                            .sorted { o1, o2 -> o2.timestamp.compareTo(o1.timestamp) }
                            .toList()
                            .toMutableList()
                    )
                }
        )
    }

    fun insertRecentSearch(userId: String) {
        compositeDisposable.add(
            recentSearchRepository.insert(RecentSearchPeople(userId, LocalDateTime.now().toLong()))
                .subscribeOn(Schedulers.io())
                .onErrorComplete()
                .subscribe {
                    Logging.show("Insert recent search successfully")
                    fetchRecentSearchList()
                }
        )
    }

    fun removeRecentSearch(item: RecentSearchPeople) {
        compositeDisposable.add(
            recentSearchRepository.delete(item)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onErrorComplete()
                .subscribe {
                    Logging.show("Delete recent search successfully")
                    fetchRecentSearchList()
                }
        )
    }

    fun searching(name: String) {

    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
