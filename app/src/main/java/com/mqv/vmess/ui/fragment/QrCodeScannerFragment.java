package com.mqv.vmess.ui.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.ConnectPeopleActivity;
import com.mqv.vmess.activity.RequestPeopleActivity;
import com.mqv.vmess.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.vmess.databinding.FragmentQrCodeScannerBinding;
import com.mqv.vmess.util.NetworkStatus;

import java.io.FileNotFoundException;
import java.util.List;

public class QrCodeScannerFragment extends BaseFragment<ConnectPeopleViewModel, FragmentQrCodeScannerBinding>
        implements DecoratedBarcodeView.TorchListener {
    private CaptureManager mCaptureManager;
    private boolean isFlashOff = true;
    private boolean isBarcodeViewPaused = false;
    private boolean isPendingScan = false;
    private boolean isScanFromImage = false;

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            var code = result.getText();

            mBinding.decoratedBarcodeView.pause();

            isBarcodeViewPaused = true;

            mViewModel.getConnectUserByQrCode(code);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };

    public QrCodeScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void binding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = FragmentQrCodeScannerBinding.inflate(inflater, container, false);
    }

    @Override
    public Class<ConnectPeopleViewModel> getViewModelClass() {
        return ConnectPeopleViewModel.class;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCaptureManager = new CaptureManager(requireActivity(), mBinding.decoratedBarcodeView);

        requestCameraPermission();
    }

    @Override
    public void setupObserver() {
        mViewModel.getConnectUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            mBinding.progressBarLoading.setVisibility(status == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);
            mBinding.textAddCode.setEnabled(status != NetworkStatus.LOADING);
            mBinding.buttonFlash.setEnabled(status != NetworkStatus.LOADING);

            if (status != NetworkStatus.LOADING && isBarcodeViewPaused) {
                mBinding.decoratedBarcodeView.resume();
            }

            switch (status) {
                case SUCCESS:
                    var user = result.getSuccess();
                    var firebaseUser = mViewModel.getFirebaseUser().getValue();

                    if (firebaseUser != null) {
                        if (user.getUid().equals(firebaseUser.getUid())) {
                            Toast.makeText(requireContext(), R.string.msg_request_yourself, Toast.LENGTH_SHORT).show();
                        } else {
                            var intent = new Intent(requireActivity(), RequestPeopleActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    }
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }

            mViewModel.resetConnectUserResult();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isPendingScan) {
            requestCameraPermission();
            isPendingScan = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureManager.onResume();

        if (isScanFromImage) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mBinding.decoratedBarcodeView.pauseAndWait();
                isBarcodeViewPaused = true;
                isScanFromImage = false;
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureManager.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.removeQrCodeObservable();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureManager.onDestroy();
    }

    /**
     * Check if the device's camera has a Flashlight.
     *
     * @return true if there is Flashlight, otherwise false.
     */
    private boolean hasFlash() {
        return requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void switchFlashlight(View view) {
        if (isFlashOff) {
            mBinding.decoratedBarcodeView.setTorchOn();
        } else {
            mBinding.decoratedBarcodeView.setTorchOff();
        }
    }

    private void requestCameraPermission() {
        ((ConnectPeopleActivity) this.requireActivity()).permissionLauncher
                .launch(Manifest.permission.CAMERA, isGranted -> {
                    if (isGranted) startScan();
                    else {
                        mBinding.layoutPermission.setVisibility(View.VISIBLE);
                        mBinding.layoutScanner.setVisibility(View.GONE);
                        mBinding.buttonEnableCamera.setOnClickListener(v -> checkCameraPermission());
                    }
                });
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getImageContent();
        } else {
            ((ConnectPeopleActivity) this.requireActivity()).permissionLauncher
                    .launch(Manifest.permission.READ_EXTERNAL_STORAGE, isGranted -> {
                        if (isGranted) {
                            getImageContent();
                        } else {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                createDialog("Request Permission",
                                        "The app will run perfectly when have a storage permission. Do you want to grant it.",
                                        "Continue",
                                        "Not now",
                                        (dialog, which) -> requestStoragePermission());
                            } else {
                                createDialog("Request Camera Permission",
                                        "The app will run perfectly when have a storage permission. Go to Settings?",
                                        "Go to Settings",
                                        "Cancel",
                                        (dialog, which) -> {
                                            isPendingScan = true;
                                            var settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            settingsIntent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                                            startActivity(settingsIntent);
                                        });
                            }
                        }
                    });
        }
    }

    private void getImageContent() {
        var storageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        storageIntent.setType("image/*");
        ((ConnectPeopleActivity) this.requireActivity()).activityResultLauncher
                .launch(storageIntent, result -> {
                    var data = result.getData();
                    if (data != null) {
                        decodeQrCodeFromImage(data.getData());
                    }
                });
    }

    private void decodeQrCodeFromImage(Uri uri) {
        try {
            var is = requireActivity().getContentResolver().openInputStream(uri);
            var bitmap = BitmapFactory.decodeStream(is);

            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();
            var source = new RGBLuminanceSource(width, height, pixels);
            var bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            var reader = new MultiFormatReader();
            try {
                var result = reader.decode(bBitmap);

                isScanFromImage = true;

                mViewModel.getConnectUserByQrCode(result.getText());
                mBinding.textAddCode.setEnabled(false);
            } catch (NotFoundException e) {
                Toast.makeText(requireActivity(), "Not found the qr code in this image", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(requireActivity(), "Can't open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScan() {
        mBinding.layoutPermission.setVisibility(View.GONE);
        mBinding.layoutScanner.setVisibility(View.VISIBLE);

        mBinding.decoratedBarcodeView.decodeContinuous(callback);

        mBinding.decoratedBarcodeView.setTorchListener(this);
        mBinding.buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        mBinding.buttonFlash.setOnClickListener(this::switchFlashlight);
        mBinding.textAddCode.setOnClickListener(v -> requestStoragePermission());
        if (!hasFlash())
            mBinding.buttonFlash.setVisibility(View.GONE);
        if (!isFlashOff)
            mBinding.buttonFlash.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_flash_on));
        else mBinding.buttonFlash.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_flash_off));
    }

    private void checkCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            createDialog("Request Permission",
                    "The app will run perfectly when have a camera permission. Do you want to grant it.",
                    "Continue",
                    "Not now",
                    (dialog, which) -> requestCameraPermission());
        } else {
            createDialog("Request Camera Permission",
                    "The app will run perfectly when have a camera permission. Go to Settings?",
                    "Go to Settings",
                    "Cancel",
                    (dialog, which) -> {
                        isPendingScan = true;
                        var settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        settingsIntent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(settingsIntent);
                    });
        }
    }

    private void createDialog(String title, String message, String positive, String negative,
                              DialogInterface.OnClickListener positiveCallback) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, positiveCallback)
                .setNegativeButton(negative, null)
                .create()
                .show();
    }

    @Override
    public void onTorchOn() {
        isFlashOff = false;
        mBinding.buttonFlash.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_flash_on));
    }

    @Override
    public void onTorchOff() {
        isFlashOff = true;
        mBinding.buttonFlash.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_flash_off));
    }
}