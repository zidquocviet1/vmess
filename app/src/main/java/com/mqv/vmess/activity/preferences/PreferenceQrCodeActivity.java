package com.mqv.vmess.activity.preferences;

import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.databinding.ActivityPreferenceQrCodeBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.network.model.User;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceQrCodeActivity extends ToolbarActivity<AndroidViewModel, ActivityPreferenceQrCodeBinding> {
    private static final float QR_CODE_WIDTH_RATIO = 0.75f;
    private static final float QR_CODE_HEIGHT_RATIO = 0.5f;

    @Override
    public void binding() {
        mBinding = ActivityPreferenceQrCodeBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_qr_code);

        var loggedInUser = LoggedInUserManager.getInstance().getLoggedInUser();

        if (loggedInUser != null)
            generateQrCode(loggedInUser);
    }

    @Override
    public void setupObserver() {

    }

    private void generateQrCode(@NonNull User user) {
        var qrContent = encodeQr(user.getUid());

        var displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        var actualWidth = displayMetrics.widthPixels;
        var actualHeight = displayMetrics.heightPixels;

        try {
            var barcodeEncoder = new BarcodeEncoder();
            var bitmap = barcodeEncoder.encodeBitmap(qrContent,
                    BarcodeFormat.QR_CODE,
                    Double.valueOf(QR_CODE_WIDTH_RATIO * actualWidth).intValue(),
                    Double.valueOf(QR_CODE_HEIGHT_RATIO * actualHeight).intValue());
            mBinding.imageQrCode.setImageBitmap(bitmap);
        } catch (Exception ignore) {
        }
    }

    public String encodeQr(String inputText) {
        var bytes = Base64.encodeBase64(inputText.getBytes(StandardCharsets.UTF_8));

        return new String(bytes);
    }
}