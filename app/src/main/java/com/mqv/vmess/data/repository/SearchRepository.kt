package com.mqv.vmess.data.repository

import com.mqv.vmess.data.model.RecentSearchPeople
import com.mqv.vmess.ui.data.People
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface SearchRepository {
    fun insert(people: RecentSearchPeople): Completable
    fun delete(people: RecentSearchPeople): Completable
    fun deleteAll(): Completable
    fun getAll(): Single<List<RecentSearchPeople>>
    fun searchPeople(query: String): Single<List<People>>
}