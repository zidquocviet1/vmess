package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.EditUserPhotoRepository;
import com.mqv.realtimechatapplication.data.result.UploadPhotoResult;
import com.mqv.realtimechatapplication.util.Const;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
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

    public void updateProfilePicture(String realFilePath) {
        uploadPhotoResult.setValue(UploadPhotoResult.Loading());

        var user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null){
                var token = task.getResult().getToken();

                cd.add(repository.updateProfilePicture(Const.PREFIX_TOKEN + token, "firebase", realFilePath)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK){
                                uploadPhotoResult.setValue(UploadPhotoResult.Success(response.getSuccess()));
                            }else if (response.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST){
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_update_user_photo));
                            }
                        }, t -> {
                            if (t instanceof FileNotFoundException){
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_file_not_found));
                            }else if (t instanceof SocketTimeoutException){
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_connection_timeout));
                            } else{
                                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_update_user_photo));
                            }
                        }));
            }else{
                uploadPhotoResult.setValue(UploadPhotoResult.Fail(R.string.error_authentication_fail));
            }
        });
    }

    public void updateCoverPhoto() {

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cd.dispose();
    }
}
