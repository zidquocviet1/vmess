package com.mqv.vmess.activity.viewmodel

import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.data.model.HistoryLoggedInUser
import com.mqv.vmess.data.repository.*
import com.mqv.vmess.network.model.User
import com.mqv.vmess.reactive.RxHelper
import io.reactivex.rxjava3.core.Completable

open class LogoutHandlerViewModel(
    private val historyUserRepository: HistoryLoggedInUserRepository,
    private val peopleRepository: PeopleRepository,
    private val notificationRepository: NotificationRepository,
    private val conversationRepository: ConversationRepository,
    private val loginRepository: LoginRepository
) : CurrentUserViewModel() {

    fun logout(
        signOutUser: FirebaseUser,
        signInUser: User,
        historyUser: HistoryLoggedInUser
    ): Completable =
        Completable.fromAction { loginRepository.logout(signOutUser) }
            .andThen(removeAfterLogout(signOutUser.uid))
            .andThen(loginRepository.saveLoggedInUser(signInUser, historyUser))

    fun removeAfterLogout(userId: String): Completable =
        Completable.mergeArray(historyUserRepository.signOut(userId))
            .mergeWith(peopleRepository.deleteAll())
            .mergeWith(notificationRepository.deleteAllLocal())
            .mergeWith(conversationRepository.deleteAll())
            .mergeWith(conversationRepository.deleteAllNotificationOption())
            .compose(RxHelper.applyCompleteSchedulers())

}