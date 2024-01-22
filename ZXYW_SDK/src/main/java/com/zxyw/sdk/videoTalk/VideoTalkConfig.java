package com.zxyw.sdk.videoTalk;

public final class VideoTalkConfig {
    private VideoTalkConfig(){}

    private static int FPS = 5;
    private static int Bitrate = 100;
    private static int cameraId = 0;
    private static int cameraMirror = 1;
    private static int rotation = 90;
    private static String serverIp = "192.168.1.20";
    private static ResponseType responseType = ResponseType.PICKUP_AUTO;
    private static boolean showLog;

    public enum ResponseType{
        PICKUP_AUTO,PICKUP_AUTO_DELAY,PICKUP_HAND
    }

    public static int getFPS() {
        return FPS;
    }

    public static void setFPS(int FPS) {
        VideoTalkConfig.FPS = FPS;
        if (VideoTalkConfig.FPS < 1) VideoTalkConfig.FPS = 1;
        if (VideoTalkConfig.FPS > 24) VideoTalkConfig.FPS = 24;
    }

    public static int getBitrate() {
        return Bitrate;
    }

    public static void setBitrate(int bitrate) {
        Bitrate = bitrate;
        if (Bitrate < 1) Bitrate = 1;
        if (Bitrate > 1000) Bitrate = 1000;
    }

    public static int getCameraId() {
        return cameraId;
    }

    public static void setCameraId(int cameraId) {
        VideoTalkConfig.cameraId = cameraId;
    }

    public static int getCameraMirror() {
        return cameraMirror;
    }

    public static void setCameraMirror(boolean mirror) {
        VideoTalkConfig.cameraMirror = mirror ? 1 : 0;
    }

    public static int getRotation() {
        return rotation;
    }

    public static void setRotation(int rotation) {
        VideoTalkConfig.rotation = rotation;
    }

    public static String getServerIp() {
        return serverIp;
    }

    public static void setServerIp(String serverIp) {
        VideoTalkConfig.serverIp = serverIp;
    }

    public static ResponseType getResponseType() {
        return responseType;
    }

    public static void setResponseType(ResponseType responseType) {
        VideoTalkConfig.responseType = responseType;
    }

    public static boolean isShowLog() {
        return showLog;
    }

    public static void setShowLog(boolean showLog) {
        VideoTalkConfig.showLog = showLog;
    }
}
