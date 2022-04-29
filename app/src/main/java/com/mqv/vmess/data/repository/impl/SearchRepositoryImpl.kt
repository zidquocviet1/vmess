package com.mqv.vmess.data.repository.impl

import com.mqv.vmess.data.dao.PeopleDao
import com.mqv.vmess.data.dao.RecentSearchDao
import com.mqv.vmess.data.model.RecentSearchPeople
import com.mqv.vmess.data.repository.SearchRepository
import com.mqv.vmess.ui.data.People
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    val dao: RecentSearchDao,
    val peopleDao: PeopleDao
) : SearchRepository {
    override fun insert(people: RecentSearchPeople) = dao.insert(people)

    override fun delete(people: RecentSearchPeople) = dao.delete(people)

    override fun deleteAll() = dao.deleteAll()

    override fun getAll() = dao.fetchAll()

    override fun searchPeople(query: String): Single<List<People>> = peopleDao.search(query)
}