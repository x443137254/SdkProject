package com.zxyw.sdk.face_sdk;

import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.zxyw.sdk.R;
import com.zxyw.sdk.tools.MyLog;

public class PreViewFragment extends Fragment implements FaceSDK.DetectFaceCallback, CameraPreview.SurfaceCreatedCallback {

    private final String TAG = "PreViewFragment";
    private CameraPreview mCameraRGBPreview = null;
    private CameraPreview mCameraIRPreview = null;
    private FaceView faceView;

    private byte[] irDataBuff;
    private long resumeTime;

    private CameraDataListener cameraDataListener;
    private View loadingView;
    private OnPreviewChangeListener rgbListener;
    private OnPreviewChangeListener irListener;

    public PreViewFragment() {
        super();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pre_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MyLog.d(TAG, "onViewCreated");
        initView(view);
    }

    public void setRgbListener(OnPreviewChangeListener rgbListener) {
        this.rgbListener = rgbListener;
    }

    public void setIrListener(OnPreviewChangeListener irListener) {
        this.irListener = irListener;
    }

    public void setCameraDataListener(CameraDataListener cameraDataListener) {
        this.cameraDataListener = cameraDataListener;
    }

    private void initCamera() {
        mCameraRGBPreview.openCamera(FaceSDK.Config.getPreviewCameraId());//Camera.CameraInfo.CAMERA_FACING_BACK
        mCameraRGBPreview.setListener(data -> {
            if (rgbListener != null) rgbListener.change();
            if ((System.currentTimeMillis() - resumeTime) < 1000) {
                return;
            }
            if (cameraDataListener != null) {
                cameraDataListener.onPreviewCallback(data, irDataBuff);
            }
        });
        mCameraRGBPreview.startPreview(true);

        new Handler(Looper.getMainLooper()).postDelayed(()->{
            if (FaceSDK.Config.getCameraNum() == 2) {
                mCameraIRPreview.openCamera(FaceSDK.Config.getPreviewCameraId() == 0 ? 1 : 0);//Camera.CameraInfo.CAMERA_FACING_FRONT
                mCameraIRPreview.setListener(data -> {
                    if (irListener != null) irListener.change();
                    irDataBuff = data;
                });
                mCameraIRPreview.startPreview(true);
            }
        },1000);

    }

    private void initView(View view) {
        mCameraRGBPreview = view.findViewById(R.id.pre_view);
        mCameraRGBPreview.setSurfaceCreatedCallback(this);
//        mCameraIRPreview = new CameraPreview(getContext());
        mCameraIRPreview = view.findViewById(R.id.pre_view2);
        faceView = view.findViewById(R.id.FaceView);
        loadingView = view.findViewById(R.id.loading_text);
    }

    public void previewPause() {
        if (mCameraRGBPreview != null) {
            mCameraRGBPreview.stop();
        }
        if (mCameraIRPreview != null) {
            mCameraIRPreview.stop();
        }
    }

    public void previewResume() {
        if (mCameraRGBPreview != null) {
            mCameraRGBPreview.resume();
        }
        if (mCameraIRPreview != null) {
            mCameraIRPreview.resume();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MyLog.d(TAG, "onStart");
        resumeTime = System.currentTimeMillis();
        initCamera();
    }

    @Override
    public void onStop() {
        super.onStop();
        MyLog.d(TAG, "onStop");
        if (mCameraRGBPreview != null) {
            mCameraRGBPreview.release();
        }
        if (mCameraIRPreview != null){
            mCameraIRPreview.release();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MyLog.d(TAG, "onDestroyView");
        faceView = null;
    }

    @Override
    public void onDetectFace(RectF[] rectList) {
        if (faceView == null) return;
        faceView.clear();
        if (rectList != null) {
            for (RectF rect : rectList) {
                faceView.addRect(rect);
            }
        }

        faceView.post(() -> {
            if (faceView != null) {
                faceView.invalidate();
            }
        });
    }

    @Override
    public int getMeasuredWidth() {
        return mCameraRGBPreview == null ? 0 : mCameraRGBPreview.getMeasuredWidth();
    }

    @Override
    public int getMeasuredHeight() {
        return mCameraRGBPreview == null ? 0 : mCameraRGBPreview.getMeasuredHeight();
    }

    @Override
    public void onSurfaceCreated() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> loadingView.setVisibility(View.GONE));
        }
    }

    public interface OnPreviewChangeListener {
        void change();
    }
}
