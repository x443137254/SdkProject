package com.zxyw.sdk.face_sdk;

public interface CameraDataListener {
    void onPreviewCallback(byte[] rgbData, byte[] irData);
}
