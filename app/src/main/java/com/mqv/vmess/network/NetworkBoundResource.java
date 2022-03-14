package com.mqv.vmess.network;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

// ResultType: Type for the Resource data.
// RequestType: Type for the API response.
public abstract class NetworkBoundResource<ResultType, RequestType> {
    private Flowable<ResultType> result;

    public NetworkBoundResource(boolean isCallInListener) {
        result = loadFromDb();

        result.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSubscriber<>() {
                    @Override
                    public void onNext(ResultType resultType) {
                        if (shouldFetch(resultType)) {
                            if (isCallInListener) {
                                callAndSaveResult();
                            } else {
                                makeCallFromRemote();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        /*
                        * Don't need handle this error.
                        * Because Flowable will not throw any exception
                        * */
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void makeCallFromRemote() {
        createCall().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<>() {
                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull RequestType requestType) {
                        saveCallResult(requestType);
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        onFetchFailed(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    // Called to save the result of the API response into the database
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestType item);

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the network.
    @MainThread
    protected abstract Boolean shouldFetch(@Nullable ResultType data);

    // Called to get the cached data from the database.
    @MainThread
    protected abstract Flowable<ResultType> loadFromDb();

    // Called to create the API call.
    @MainThread
    protected abstract Observable<RequestType> createCall();

    protected abstract void callAndSaveResult();

    // Called when the fetch fails. The child class may want to reset components
    // like rate limiter.
    protected void onFetchFailed(Throwable throwable) {
    }

    public Observable<ResultType> asObservable() {
        return result.toObservable();
    }
}
