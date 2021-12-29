package com.mqv.realtimechatapplication.work;

import android.util.Pair;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
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

    public static Observable<String> getBearerTokenObservable(FirebaseUser user) {
        return Observable.fromCallable(() -> {
                    try {
                        return new Pair<Optional<String>, Throwable>(UserTokenUtil.getToken(user), null);
                    } catch (Throwable t) {
                        return new Pair<>(Optional.<String>empty(), t);
                    }
                })
                .flatMap(pair -> {
                    Optional<String> tokenOptional = pair.first;
                    Throwable throwable = pair.second;

                    if (tokenOptional.isPresent()) {
                        String token = tokenOptional.get();
                        String bearerToken = Const.PREFIX_TOKEN + token;

                        return Observable.just(bearerToken);
                    } else {
                        return Observable.create(emitter -> {
                            if (!emitter.isDisposed()) {
                                if (throwable instanceof FirebaseNetworkException) {
                                    emitter.onError(new FirebaseNetworkException("Network error"));
                                } else {
                                    emitter.onError(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                                }
                            }
                        });
                    }
                });
    }
}
