package com.mqv.realtimechatapplication.ui.fragment;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.ConnectPeopleViewModel;
import com.mqv.realtimechatapplication.databinding.FragmentQrCodeScannerBinding;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.List;

public class QrCodeScannerFragment extends BaseFragment<ConnectPeopleViewModel, FragmentQrCodeScannerBinding>
        implements DecoratedBarcodeView.TorchListener {
    private CaptureManager mCaptureManager;
    private boolean isFlashOff = true;
    private boolean isBarcodeViewPaused = false;

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
        mBinding.decoratedBarcodeView.decodeContinuous(callback);

        mBinding.decoratedBarcodeView.setTorchListener(this);
        mBinding.buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        mBinding.buttonFlash.setOnClickListener(this::switchFlashlight);

        if (!hasFlash())
            mBinding.buttonFlash.setVisibility(View.GONE);
    }

    @Override
    public void setupObserver() {
        mViewModel.getConnectUserResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            mBinding.progressBarLoading.setVisibility(status == NetworkStatus.LOADING ? View.VISIBLE : View.GONE);

            if (status != NetworkStatus.LOADING && isBarcodeViewPaused) {
                mBinding.decoratedBarcodeView.resume();
            }

            switch (status) {
                case SUCCESS:
                    Logging.show(result.getSuccess().getUid());
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }

            mViewModel.resetConnectUserResult();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureManager.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureManager.onResume();
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