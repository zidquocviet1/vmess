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
import com.mqv.vmess.data.repository.ChatRepository
import com.mqv.vmess.data.repository.ConversationRepository
import com.mqv.vmess.data.repository.PeopleRepository
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
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.ConversationListFragment
import com.mqv.vmess.ui.fragment.SuggestionFriendListFragment
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.MessageUtil
import com.mqv.vmess.work.SendMessageWorkWrapper
import com.mqv.vmess.work.WorkDependency
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@HiltViewModel
class UserSelectionListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: ChatRepository,
    application: Application
) : AndroidViewModel(application) {
    private val mCurrentUser = FirebaseAuth.getInstance().currentUser!!
    private val compositeDisposable = CompositeDisposable()
    private val _userSuggestionList = MutableLiveData(mutableListOf<UserSelection>())
    private val _userRecentSearchList = MutableLiveData(mutableListOf<UserSelection>())

    private var whoCreateWith: UserSelection? = null
    private var messageToSend: Chat? = null

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
        fetchSuggestedFriend(
            savedStateHandle.get<Boolean>(SuggestionFriendListFragment.ARG_INCLUDE_GROUP) ?: false
        )

        messageToSend = savedStateHandle.get(SuggestionFriendListFragment.ARG_MESSAGE_TO_SEND)
    }

    val userSuggestionList: LiveData<MutableList<UserSelection>> get() = _userSuggestionList
    val userRecentSearchList: LiveData<MutableList<UserSelection>> get() = _userRecentSearchList

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

    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}
