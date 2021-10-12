package com.mqv.realtimechatapplication.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.RequestPeopleViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityRequestPeopleBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.LoadingDialog;
import com.mqv.realtimechatapplication.util.NetworkStatus;
import com.mqv.realtimechatapplication.work.FetchNotificationWorker;

import java.time.format.DateTimeFormatter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RequestPeopleActivity extends BaseActivity<RequestPeopleViewModel, ActivityRequestPeopleBinding>
        implements View.OnClickListener {
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private User user;
    private User currentUser;
    private FriendRequestStatus friendRequestStatus;
    private FriendRequestStatus responseStatus;

    @Override
    public void binding() {
        mBinding = ActivityRequestPeopleBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<RequestPeopleViewModel> getViewModelClass() {
        return RequestPeopleViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = getIntent().getParcelableExtra("user");
        currentUser = LoggedInUserManager.getInstance().getLoggedInUser();
        mViewModel.getFriendRequestStatusByUid(user.getUid());

        mBinding.buttonAddFriend.setOnClickListener(this);
        mBinding.buttonConfirm.setOnClickListener(this);
        mBinding.buttonCancel.setOnClickListener(this);
        mBinding.buttonMessage.setOnClickListener(this);
        mBinding.buttonBack.setOnClickListener(this);
        mBinding.buttonOverflow.setOnClickListener(this);
        mBinding.imageAvatar.setOnClickListener(this);
    }

    @Override
    public void setupObserver() {
        mViewModel.getFriendRequestStatus().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            showLoadingUi(status == NetworkStatus.LOADING, status == NetworkStatus.ERROR);

            switch (status) {
                case SUCCESS:
                    friendRequestStatus = result.getSuccess();
                    showUserUi(user);
                    break;
                case ERROR:
                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        mViewModel.getRequestConnectResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    mBinding.buttonAddFriend.setVisibility(View.GONE);
                    mBinding.buttonCancel.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();

                    break;
            }
        });

        mViewModel.getResponseRequestResult().observe(this, result -> {
            if (result == null) return;

            var status = result.getStatus();

            switch (status) {
                case LOADING:
                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.msg_loading);

                    break;
                case SUCCESS:
                    LoadingDialog.finishLoadingDialog();

                    if (responseStatus == FriendRequestStatus.CONFIRM)
                        fetchNotificationWithWork();

                    setResult(RESULT_OK);

                    finish();
                    break;
                case ERROR:
                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(getApplicationContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.buttonAddFriend.getId()) {
            mViewModel.requestConnect(new FriendRequest(currentUser.getUid(), user.getUid()));
        } else if (id == mBinding.buttonConfirm.getId()) {
            responseStatus = FriendRequestStatus.CONFIRM;
            mViewModel.responseFriendRequest(new FriendRequest(currentUser.getUid(), user.getUid(), responseStatus));
        } else if (id == mBinding.buttonCancel.getId()) {
            responseStatus = FriendRequestStatus.CANCEL;
            mViewModel.responseFriendRequest(new FriendRequest(currentUser.getUid(), user.getUid(), responseStatus));
        } else if (id == mBinding.buttonMessage.getId()) {
            // TODO: open message tap and check is friend or not
        } else if (id == mBinding.buttonBack.getId()) {
            onBackPressed();
        } else if (id == mBinding.buttonOverflow.getId()) {
            var popUp = new PopupMenu(this, mBinding.buttonOverflow);
            popUp.inflate(R.menu.menu_user_request);
            popUp.setOnMenuItemClickListener(item -> false);
            popUp.show();
        } else if (id == mBinding.imageAvatar.getId()) {
            startPhotoPreview();
        }
    }

    private void showLoadingUi(boolean isLoading, boolean isError) {
        mBinding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mBinding.layoutContent.setVisibility(isLoading ? View.GONE : (isError ? View.GONE : View.VISIBLE));
        mBinding.layoutError.setVisibility(isLoading ? View.GONE : (isError ? View.VISIBLE : View.GONE));
    }

    private void showUserUi(User user) {
        if (user != null) {
            var photoUrl = user.getPhotoUrl() == null ? null : user.getPhotoUrl().replace("localhost", Const.BASE_IP);

            GlideApp.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .error(R.drawable.ic_account_undefined)
                    .fallback(R.drawable.ic_round_account)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(mBinding.imageAvatar);

            mBinding.textDisplayName.setText(user.getDisplayName());

            var shouldShowBio = user.getBiographic() == null || user.getBiographic().equals("");
            var bio = user.getBiographic() == null ? "" : user.getBiographic();
            mBinding.textBio.setText(bio);
            mBinding.textBio.setVisibility(shouldShowBio ? View.GONE : View.VISIBLE);

            var joinTimeStamp = user.getCreatedDate();
            if (joinTimeStamp != null) {
                var joinedAtTitle = getString(R.string.title_joined_at, joinTimeStamp.format(MONTH_YEAR_FORMATTER));
                var a = Html.fromHtml(joinedAtTitle, Html.FROM_HTML_MODE_COMPACT);
                mBinding.textJoined.setText(a);
                mBinding.textJoined.setVisibility(View.VISIBLE);
            } else {
                mBinding.textJoined.setVisibility(View.GONE);
            }

            if (friendRequestStatus != null) {
                switch (friendRequestStatus) {
                    case CONFIRM:
                        mBinding.layoutButton.setVisibility(View.GONE);
                        mBinding.buttonAddFriend.setVisibility(View.GONE);
                        mBinding.buttonCancel.setVisibility(View.GONE);
                        mBinding.buttonConfirm.setVisibility(View.GONE);
                        break;
                    case CANCEL:
                        mBinding.layoutButton.setVisibility(View.VISIBLE);
                        mBinding.buttonAddFriend.setVisibility(View.VISIBLE);
                        mBinding.buttonCancel.setVisibility(View.GONE);
                        mBinding.buttonConfirm.setVisibility(View.GONE);
                        break;
                    case REQUEST:
                        mBinding.layoutButton.setVisibility(View.VISIBLE);
                        mBinding.buttonAddFriend.setVisibility(View.GONE);
                        mBinding.buttonCancel.setVisibility(View.VISIBLE);
                        mBinding.buttonConfirm.setVisibility(View.GONE);
                        break;
                    case ACKNOWLEDGE:
                        mBinding.layoutButton.setVisibility(View.VISIBLE);
                        mBinding.buttonAddFriend.setVisibility(View.GONE);
                        mBinding.buttonCancel.setVisibility(View.GONE);
                        mBinding.buttonConfirm.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }
    }

    private void startPhotoPreview() {
        var dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar_TranslucentDecor);
        dialog.setContentView(R.layout.dialog_image_preview);
//            dialog.getWindow().getAttributes().windowAnimations = R.style.SlideDialogAnimation; // use this method to override animation

        var img = (ImageView) dialog.findViewById(R.id.imgAvatar);
        var imgBack = dialog.findViewById(R.id.imgBack);
        var imgMoreVert = dialog.findViewById(R.id.imgMoreVert);

        imgBack.setOnClickListener(v12 -> dialog.dismiss());
        imgMoreVert.setOnClickListener(v1 -> {
//                PopupMenu popup = new PopupMenu(this, imgMoreVert);
//                popup.inflate(R.menu.mn_profile_image);
//                popup.setOnMenuItemClickListener(item -> {
//                    int id1 = item.getItemId();
//
//                    if (id1 == R.id.mnSave) {
//                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
//                                PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
//                            saveProfileImage();
//                        }else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
//                            ExplainReasonUsePermissionDialog explainDialog = new ExplainReasonUsePermissionDialog();
//                            explainDialog.show(getSupportFragmentManager(), "explain");
//                        }else{
//                            requestPermissionLauncher.mLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE, isGranted -> {
//                                if (isGranted){
//                                    saveProfileImage();
//                                }else{
//                                    AppInfoPermissionDialog appInfoDialog = new AppInfoPermissionDialog();
//                                    appInfoDialog.show(getSupportFragmentManager(), "app_info");
//                                }
//                            });
//                        }
//                    } else if (id1 == R.id.mnShareImage) {
//                        Intent sendLinkIntent = new Intent(Intent.ACTION_SEND);
//                        sendLinkIntent.setType("text/plain");
//                        sendLinkIntent.putExtra(Intent.EXTRA_TEXT, AppConstants.API_ENDPOINT + "user/avatar?userId=" + user.getId());
//                        startActivity(sendLinkIntent);
//                    } else if (id1 == R.id.mnOpenInBrowser) {
//                        Intent intent = new Intent(Intent.ACTION_VIEW);
//                        intent.setData(Uri.parse(AppConstants.API_ENDPOINT + "user/avatar?userId=" + user.getId()));
//                        startActivity(intent);
//                    }
//                    return false;
//                });
//                popup.show();
        });

        var cp = new CircularProgressDrawable(this);
        cp.setStrokeWidth(5f);
        cp.setCenterRadius(30f);
        cp.start();

        var photoUrl = user.getPhotoUrl() == null ? "" : user.getPhotoUrl().replace("localhost", Const.BASE_IP);

        GlideApp.with(this)
                .load(photoUrl)
                .centerCrop()
                .error(R.drawable.ic_round_account)
                .placeholder(cp)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(img);
        dialog.show();
    }

    private void fetchNotificationWithWork() {
        var constraint =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        var workRequest =
                new OneTimeWorkRequest.Builder(FetchNotificationWorker.class)
                        .setConstraints(constraint)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork("notification_worker", ExistingWorkPolicy.REPLACE, workRequest);
    }
}