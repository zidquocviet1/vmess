package com.mqv.vmess.activity.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mqv.vmess.data.repository.ConversationRepository
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.reactive.RxHelper.applyFlowableSchedulers
import com.mqv.vmess.util.Const
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.stream.Collectors
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    private val _loadCacheObserver = MutableLiveData<Boolean>()

    val loadCacheObserver: LiveData<Boolean> get() = _loadCacheObserver

    fun getCacheConversation() {
        //noinspection ResultOfMethodCallIgnored
        conversationRepository.conversationAndLastChat(ConversationStatusType.INBOX, Const.DEFAULT_CONVERSATION_PAGING_SIZE)
            .compose(applyFlowableSchedulers())
            .map { map ->
                this.mapToListConversation(
                    map
                )
            }
            .subscribe { value ->
                AppDependencies.getMemoryManager().setConversationList(value)
                _loadCacheObserver.postValue(true)
            }
    }

    private fun mapToListConversation(map: MutableMap<Conversation, Chat?>): List<Conversation> {
        return map.entries.stream()
            .map { entry ->
                val c = entry.key
                val m = entry.value
                c.chats = listOf(m)
                c
            }
            .collect(Collectors.toList())
    }

}