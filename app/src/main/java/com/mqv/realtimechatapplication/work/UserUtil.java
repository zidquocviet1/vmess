package com.mqv.realtimechatapplication.work;

import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;

import java.time.LocalDateTime;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class UserUtil {
    private final UserDao userDao;
    private final HistoryLoggedInUserDao historyUserDao;

    @Inject
    public UserUtil(UserDao userDao, HistoryLoggedInUserDao historyUserDao) {
        this.userDao = userDao;
        this.historyUserDao = historyUserDao;
    }

    public Single<Boolean> isRecentLogin() {
        return historyUserDao.getLoggedInUser()
                             .flatMap(user -> userDao.findByUid(user.getUid()))
                             .map(user -> {
                                 var fetchTimeOut = 30;
                                 var now = LocalDateTime.now();

                                 return user.getAccessedDate().plusSeconds(fetchTimeOut).compareTo(now) <= 0;
                             })
                             .onErrorReturnItem(false);
    }
}
