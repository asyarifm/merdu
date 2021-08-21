package com.asyarifm.merdu.music.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.asyarifm.merdu.R;

// this class use to create, show and hide loading dialog
public class LoadingDialog {
    private AlertDialog loadingDialog;

    // class constructor
    public LoadingDialog(Activity activity) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View loadingDialogView = inflater.inflate(R.layout.layout_loading_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(loadingDialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }

    // show dialog if dialog is not showing
    public void startLoading() {
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    // hide dialog if dialog is showing
    public void stopLoading() {
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
