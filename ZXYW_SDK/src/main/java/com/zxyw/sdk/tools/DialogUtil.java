package com.zxyw.sdk.tools;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.zxyw.sdk.R;

public class DialogUtil {
    public static AlertDialog getDialog(@NonNull final Activity activity, @NonNull View view) {
        final MyDialog alertDialog = new MyDialog(activity, R.style.MyDialog);
        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(dialog -> {
            Window window = alertDialog.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        });
        try {
            alertDialog.show();
        } catch (Exception e) {
            MyLog.e("DialogUtil", e.toString());
        }
        alertDialog.setOnCancelListener(dialog -> {
            final Window window = activity.getWindow();
            if (window != null) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        });
        return alertDialog;
    }

    static class MyDialog extends AlertDialog {

        MyDialog(@NonNull Context context, int themeResId) {
            super(context, themeResId);
        }

        @Override
        public void show() {
            final Window window = this.getWindow();
            if (window != null) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                super.show();
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        }
    }
}
