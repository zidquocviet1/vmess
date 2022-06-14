package com.mqv.vmess.crypto.storage

import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.data.dao.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

abstract class DatabaseSessionInteractor(database: MyDatabase) {
    protected val preKeyDao: PreKeyDao = database.preKeyDao
    protected val signedPreKeyDao: SignedPreKeyDao = database.signedPreKeyDao
    protected val senderKeyDao: SenderKeyDao = database.senderKeyDao
    protected val identityKeyDao: IdentityKeyDao = database.identityKeyDao
    protected val accountDao: AccountDao = database.accountDao
    protected val sessionDao: SessionDao = database.sessionDao

    protected fun Completable.internalExecute() {
        this.subscribeOn(MAIN_SCHEDULER).subscribe()
    }

    protected fun <T : Any> Single<T>.internalGet(): T? {
        return this.subscribeOn(MAIN_SCHEDULER).onErrorComplete().blockingGet()
    }

    protected fun <T : Any> Single<T>.requireGet(): T =
        this.subscribeOn(MAIN_SCHEDULER).blockingGet()

    companion object {
        private val MAIN_SCHEDULER = Schedulers.io()
    }
}