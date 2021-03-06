package com.mqv.vmess.activity.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.data.repository.EditUserPhotoRepository;
import com.mqv.vmess.data.result.UploadPhotoResult;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.FileProviderUtil;
import com.mqv.vmess.util.Logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class PreviewEditPhotoViewModel extends ViewModel {
    private final EditUserPhotoRepository repository;
    private final CompositeDisposable cd = new CompositeDisposable();
    private final MutableLiveData<UploadPhotoResult> uploadPhotoResult = new MutableLiveData<>();

    @Inject
    public PreviewEditPhotoViewModel(EditUserPhotoRepository repository) {
        this.repository = repository;
    }

    public LiveData<UploadPhotoResult> getUploadPhotoResult() {
        return uploadPhotoResult;
    }

    public void updateProfilePicture(Context context, String realFilePath) {
        File file = new File(realFilePath);
        uploadPhotoResult.setValue(UploadPhotoResult.Loading());

        var user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                var token = task.getResult().getToken();

                cd.add(Observable.fromFuture(FileProviderUtil.compressFileFuture(context, file))
                        .flatMap(compress -> repository.updateProfilePicture(Const.PREFIX_TOKEN + token, compress))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                uploadPhotoResult.setValue(UploadPhotoResult.Success(response.getSuccess()));
                            } else if (response.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_update_user_photo));
                            }
                        }, t -> {
                            if (t instanceof FileNotFoundException) {
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_file_not_found));
                            } else if (t instanceof SocketTimeoutException) {
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_connection_timeout));
                            } else {
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_update_user_photo));
                            }
                        }));
            } else {
                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_authentication_fail));
            }
        });
    }

    public void updateCoverPhoto() {

    }

    public void updateHistoryUserPhotoUrl(FirebaseUser user, String photoUrl) {
        cd.add(repository.updateCurrentUserPhotoUrl(user.getUid(), photoUrl)
                .andThen(repository.updateHistoryUserPhotoUrl(user.getUid(), photoUrl))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                    Logging.show("update history logged in user photo url successfully");
                }, Throwable::printStackTrace));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
