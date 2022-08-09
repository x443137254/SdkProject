package com.zxyw.sdk.face_sdk;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zxyw.sdk.tools.MyLog;

import java.util.List;

@SuppressWarnings("all")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private CameraListener listener = null;
    private int cameraId;
    private SurfaceCreatedCallback surfaceCreatedCallback;
    private int width;//相机实际宽
    private int height;//相机实际高

    public CameraPreview(Context context) {
        super(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(CameraListener listener) {
        this.listener = listener;
    }

    public interface CameraListener {
        void onPreviewFrameCB(byte[] data);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            MyLog.d(TAG, "mCamera: " + mCamera + " surfaceCreated" + holder.toString());
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            MyLog.d(TAG, "surfaceCreated error：" + e.toString());
        }
        if (surfaceCreatedCallback != null) {
            surfaceCreatedCallback.onSurfaceCreated();
        }
    }

    public void setSurfaceCreatedCallback(SurfaceCreatedCallback surfaceCreatedCallback) {
        this.surfaceCreatedCallback = surfaceCreatedCallback;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        MyLog.d(TAG, "surfaceDestroyed");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        MyLog.d(TAG, "surfaceChanged, holder: " + holder);
    }

    public void openCamera(int camId) {
        synchronized (this) {
            cameraId = camId;
            try {
                MyLog.d(TAG, "openCamera，cameraId=" + cameraId);
                mCamera = Camera.open(camId);
                Log.d(TAG, "mCamera: " + mCamera);
                Camera.Parameters mParams = mCamera.getParameters();
                getCameraBastSize(mParams);
                mParams.setPreviewSize(width, height);
                mCamera.setDisplayOrientation(FaceSDK.Config.getPreviewOrientation());
                mCamera.setParameters(mParams);
                MyLog.d(TAG, "camera size: " + width + "*" + height);
            } catch (Exception e) {
                MyLog.d(TAG, e.toString());
            }
        }
    }

    private void getCameraBastSize(Camera.Parameters params) {
        final List<Camera.Size> sizeList = params.getSupportedPreviewSizes();
        int ratio = 0;
        for (Camera.Size size : sizeList) {
            if (ratio == 0) {
                ratio = size.width - size.height;
            }
            if (size.width == FaceSDK.Config.getCameraWidth() && size.height == FaceSDK.Config.getCameraHeight()) {
                width = size.width;
                height = size.height;
                return;
            }
        }
        width = sizeList.get(sizeList.size() / 2).width;
        height = sizeList.get(sizeList.size() / 2).height;
    }

    public void addCallbackBuffer(byte[] buffer) {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(buffer);
        }
    }

    public void startPreview(boolean isDisplay) {
        try {
            if (isDisplay) {
                mHolder = getHolder();
                mHolder.addCallback(this);
                Log.d(TAG, "camera: " + mCamera + " mHolder: " + mHolder);
            }

            mCamera.addCallbackBuffer(new byte[width * height * 3 / 2]);
            mCamera.startPreview();
            MyLog.d(TAG, "startPreview, camera id=" + cameraId);

            mCamera.setPreviewCallbackWithBuffer((data, camera) -> {
                if (listener != null) {
                    listener.onPreviewFrameCB(data);
                }
                camera.addCallbackBuffer(data);
            });
        } catch (Exception e) {
            MyLog.d(TAG, e.toString());
        }
    }

    public void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void release() {
        MyLog.d(TAG, "camera release，cameraId=" + cameraId);
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mHolder != null) {
            mHolder.removeCallback(this);
            mHolder = null;
        }
    }

    public void resume() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    interface SurfaceCreatedCallback {
        void onSurfaceCreated();
    }
}
