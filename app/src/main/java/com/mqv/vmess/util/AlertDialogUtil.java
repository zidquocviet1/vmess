package com.mqv.vmess.util;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.vmess.R;

public class AlertDialogUtil {
    private static AlertDialog loadingDialog;

    public static void startLoadingDialog(Context context, LayoutInflater inflater, int content) {
        var builder = new MaterialAlertDialogBuilder(context);
        var view = inflater.inflate(R.layout.dialog_loading_with_text, null, false);
        var textUploading = (TextView) view.findViewById(R.id.text_uploading);
        var animBlink = AnimationUtils.loadAnimation(context, R.anim.blink);

        textUploading.setText(content);
        textUploading.startAnimation(animBlink);
        builder.setView(view);

        loadingDialog = builder.create();
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
    }

    public static void finishLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing())
            loadingDialog.dismiss();
    }

    public static void show(Context context,
                            @StringRes int title,
                            @StringRes int message,
                            @StringRes int positiveButton,
                            @StringRes int negativeButton,
                            DialogInterface.OnClickListener positiveClick) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, positiveClick)
                .setNegativeButton(negativeButton, null)
                .show();
    }
}
