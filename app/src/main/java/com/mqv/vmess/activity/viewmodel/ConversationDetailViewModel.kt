package com.mqv.vmess.activity.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.room.rxjava3.EmptyResultSetException
import com.mqv.vmess.R
import com.mqv.vmess.activity.ConversationDetailActivity
import com.mqv.vmess.activity.preferences.MessageMediaSort
import com.mqv.vmess.data.DatabaseObserver
import com.mqv.vmess.data.model.ConversationColor
import com.mqv.vmess.data.model.ConversationNotificationOption
import com.mqv.vmess.data.repository.*
import com.mqv.vmess.data.result.Result
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.notification.toUser
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.ConversationDetail
import com.mqv.vmess.ui.data.ConversationMapper
import com.mqv.vmess.ui.data.LinkMedia
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.Event
import com.mqv.vmess.util.Logging
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    override val conversationRepository: ConversationRepository,
    override val chatRepository: ChatRepository,
    override val peopleRepository: PeopleRepository,
    override val friendRequestRepository: FriendRequestRepository,
    private val linkMetadataRepository: LinkMetadataRepository,
    savedStateHandle: SavedStateHandle,
    application: Application
) : MessageHandlerViewModel(
    application,
    conversationRepository,
    chatRepository,
    peopleRepository,
    friendRequestRepository
) {
    private var _mConversation: Conversation? = null
    private val mConversationId: String =
        savedStateHandle.get<String>(ConversationDetailActivity.EXTRA_CONVERSATION_ID)!!
    private val _conversationDetail = MutableLiveData<ConversationDetail>()
    private val _conversationObserver: DatabaseObserver.ConversationListener
    private val _requestState = MutableLiveData<Event<Result<Boolean>>>()
    private val _singleToast = MutableLiveData<Event<Int>>()
    private val _conversationColor = MutableLiveData<ConversationColor>()
    private val _mediaSortType =
        MutableLiveData(AppDependencies.getAppPreferences().messageMediaSort)
    private val _linkMediaResult = MutableLiveData<Result<List<LinkMedia>>>()

    init {
        _conversationObserver = object : DatabaseObserver.ConversationListener {
            override fun onConversationInserted(conversationId: String) {
            }

            override fun onConversationUpdated(conversationId: String) {
                if (conversationId == mConversationId) fetch(conversationId)
            }

        }

        AppDependencies.getDatabaseObserver().registerConversationListener(_conversationObserver)

        fetch(mConversationId)
    }

    val conversation: Conversation get() = _mConversation!!
    val conversationDetail: LiveData<ConversationDetail> get() = _conversationDetail
    val requestState: LiveData<Event<Result<Boolean>>> get() = _requestState
    val singleToast: LiveData<Event<Int>> get() = _singleToast
    val conversationColor: LiveData<ConversationColor> get() = _conversationColor
    val mediaSortType: LiveData<MessageMediaSort> get() = _mediaSortType
    val linkMediaResult: LiveData<Result<List<LinkMedia>>> get() = _linkMediaResult

    private fun fetch(conversationId: String) {
        fetchConversationDetail(conversationId)
        observeConversationColor(conversationId)
    }

    private fun observeConversationColor(conversationId: String) {
        addDisposable(
            conversationRepository.fetchConversationColor(conversationId)
                .compose(RxHelper.applyFlowableSchedulers())
                .doOnError { Logging.show("Don't have any conversation color yet with id = $conversationId") }
                .defaultIfEmpty(listOf(ConversationColor(mConversationId)))
                .subscribe({ list ->
                    _conversationColor.postValue(list[0])
                }, {
                    _conversationColor.postValue(ConversationColor(mConversationId))
                })
        )
    }

    private fun fetchConversationDetail(conversationId: String) {
        addDisposable(
            conversationRepository.fetchCachedById(conversationId)
                .zipWith(
                    conversationRepository.fetchConversationOption(conversationId)
                        .onErrorReturnItem(
                            ConversationNotificationOption.DEFAULT
                        )
                ) { conversation, option ->
                    _mConversation = conversation
                    ConversationDetail(
                        ConversationMapper.mapToMetadata(
                            conversation,
                            mUser.toUser(),
                            getApplication<Application>().applicationContext
                        ), option
                    )
                }
                .compose(RxHelper.applySingleSchedulers())
                .subscribe { detail, _ ->
                    _conversationDetail.postValue(detail)
                }
        )
    }

    fun muteNotification(until: Long) {
        muteNotification(mConversationId, until)
        fetch(mConversationId)
    }

    fun unMuteNotification() {
        unMuteNotification(mConversationId)
        fetch(mConversationId)
    }

    fun changeGroupName(name: String) {
        changeGroupName(name, mConversationId, {
            _requestState.postValue(Event(Result.Loading()))
        }, {
            _requestState.postValue(Event(Result.Success(true)))
        }, {
            _requestState.postValue(Event(Result.Fail(R.string.msg_permission_denied)))
        }) {
            _singleToast.postValue(it)
        }
    }

    fun changeGroupThumbnail(file: File) {
        changeGroupThumbnail(file, mConversationId, {
            _requestState.postValue(Event(Result.Loading()))
        }, {
            _requestState.postValue(Event(Result.Success(true)))
        }, {
            _requestState.postValue(Event(Result.Fail(R.string.msg_permission_denied)))
        }) {
            _singleToast.postValue(it)
        }
    }

    private fun getOrCreateColor(): ConversationColor {
        return _conversationColor.value ?: ConversationColor(mConversationId)
    }

    private fun saveConversationColor(color: ConversationColor) {
        addDisposable(
            conversationRepository.saveConversationColor(color)
                .compose(RxHelper.applyCompleteSchedulers())
                .doOnError { Logging.show("Can't insert conversation color because: ${it.message}") }
                .onErrorComplete()
                .subscribe()
        )
    }

    fun changeChatColor(colorCode: String) {
        getOrCreateColor().run {
            chatColor = colorCode
            saveConversationColor(this)
        }
    }

    fun changeWallpaperColor(colorCode: String) {
        getOrCreateColor().run {
            wallpaperColor = colorCode
            saveConversationColor(this)
        }
    }

    fun resetChatColor() {
        getOrCreateColor().run {
            chatColor = ConversationColor.DEFAULT_CHAT_COLOR
            saveConversationColor(this)
        }
    }

    fun resetWallpaperColor() {
        getOrCreateColor().run {
            wallpaperColor = ConversationColor.DEFAULT_WALL_PAPER_COLOR
            saveConversationColor(this)
        }
    }

    fun submitChangeSortType(sort: MessageMediaSort) {
        _mediaSortType.postValue(sort)
    }

    fun loadBunchOfLinkPreview(shareMessages: List<Chat>) {
        Observable.fromIterable(shareMessages)
            .startWith(Completable.fromAction { _linkMediaResult.postValue(Result.Loading()) })
            .flatMapSingle { message ->
                linkMetadataRepository.findById(message.id).onErrorResumeNext {
                    return@onErrorResumeNext if (it is EmptyResultSetException) {
                        linkMetadataRepository.refreshLinkMetadata(message.id, message.share.link, message.timestamp.toLong())
                    } else {
                        Single.error(it)
                    }
                }
            }
            .flatMapSingle { link ->
                return@flatMapSingle if (link.maxAge <= LocalDateTime.now().toLong()) {
                    linkMetadataRepository.refreshLinkMetadata(link.id, link.url, link.sendTime)
                } else {
                    Single.just(link)
                }
            }
            .map { metadata ->
                with(metadata) {
                    LinkMedia(id, url, thumbnail, title, description, sendTime)
                }
            }
            .toList()
            .compose(RxHelper.applySingleSchedulers())
            .subscribe(
                { _linkMediaResult.postValue(Result.Success(it)) },
                { _linkMediaResult.postValue(Result.Fail(-1)) }
            )
    }

    override fun onCleared() {
        super.onCleared()

        AppDependencies.getDatabaseObserver().unregisterConversationListener(_conversationObserver)
    }
}