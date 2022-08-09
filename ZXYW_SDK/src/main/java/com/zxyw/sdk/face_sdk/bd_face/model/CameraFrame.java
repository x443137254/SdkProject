package com.zxyw.sdk.face_sdk.bd_face.model;

public class CameraFrame {
    public byte[] rgbData;
    public byte[] irData;

    public CameraFrame(byte[] rgbData, byte[] irData) {
        this.rgbData = rgbData;
        this.irData = irData;
    }
}
