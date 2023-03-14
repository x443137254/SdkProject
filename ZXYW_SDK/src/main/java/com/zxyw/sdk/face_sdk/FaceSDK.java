package com.zxyw.sdk.face_sdk;

import android.content.Context;
import android.graphics.RectF;
import android.hardware.Camera;

import java.util.List;

public interface FaceSDK extends CameraDataListener {

    /**
     * SDK初始化
     *
     * @param context   上下文
     * @param groupList 需要创建的人脸分组，当传入参数为null时，需创建一个默认分组
     */
    void init(Context context, List<String> groupList, String url, InitFinishCallback callback);

    /**
     * 手动获取授权文件
     *
     * @param context 上下文
     * @param url     授权文件地址
     */
    void getCertificate(Context context, String url, AuthCallback callback);

    /**
     * 添加人脸
     *
     * @param photoPath 照片路径
     * @param callback  添加结果回调方法
     */
    void addFace(String photoPath, AddFaceCallback callback);

    /**
     * 删除人脸
     *
     * @param faceToken 删除的人脸对应的faceToken
     */
    void deleteFace(String faceToken);

    /**
     * 删除所有人脸数据
     */
    void clearFace();

    /**
     * 设置当前使用的人脸组
     *
     * @param group 人脸分组名称
     */
    void setCurrentGroup(String group);

    /**
     * 删除人脸数据
     *
     * @param groupName 人脸分组
     */
    void clearFace(String groupName);

    /**
     * 保存抓怕照片
     *
     * @param savePath 保存的路径
     * @return 是否保存成功
     */
    boolean capture(String savePath);

    /**
     * 授权激活
     *
     * @param context  上下文
     * @param cert     激活码
     * @param callback 激活结果回调
     */
    void auth(Context context, String cert, AuthCallback callback);

    /**
     * 释放所有资源
     */
    void release();

    /**
     * 是否已激活
     */
    boolean isActive();

    /**
     * 使用当前底库进行照片比对
     *
     * @param photoPath 比对的照片
     * @return 比对结果，找到相似人脸返回对应token，否则返回null
     */
    String compare(Context context, String photoPath);

    void setRecognizeCallback(RecognizeCallback recognizeCallback);

    void setDetectFaceCallback(DetectFaceCallback detectFaceCallback);

    @Override
    void onPreviewCallback(byte[] rgbData, byte[] irData);

    interface AddFaceCallback {
        /**
         * 人脸添加结果回调
         *
         * @param success true 添加成功，false 添加失败
         * @param message 添加失败时的提示消息
         */
        void addResult(boolean success, String message);
    }

    interface InitFinishCallback {
        void initFinish(boolean success);
    }

    interface RecognizeCallback {
        /**
         * 人脸识别结果回调
         *
         * @param success true 识别成功，false 识别失败
         * @param message 识别成功时返回对应faceToken，否则返回提示消息
         */
        void recognizeResult(boolean success, String message);
    }

    interface AuthCallback {
        /**
         * 授权激活结果回调
         *
         * @param success true 激活成功，false 激活失败
         */
        void authResult(boolean success);
    }

    interface DetectFaceCallback {
        /**
         * 检测到人脸后回调，在预览画面上绘出人脸位置框
         *
         * @param rectList 人脸矩形框（已经过旋转缩放处理）
         */
        void onDetectFace(RectF[] rectList);

        /**
         * 获取显示窗口宽度
         *
         * @return 宽度
         */
        int getMeasuredWidth();

        /**
         * 获取显示窗口高度
         *
         * @return 高度
         */
        int getMeasuredHeight();
    }

    class Config {
        //rbg预览画面旋转角度
        private static int previewOrientation = 0;
        //ir画面旋转角度
        private static int recognizeOrientation = 0;
        //抓拍照片旋转角度
        private static int captureOrientation = 0;
        private static int cameraWidth = 480;
        private static int cameraHeight = 640;
        @SuppressWarnings("deprecation")
        private static int cameraNum = Camera.getNumberOfCameras();
        //人脸框镜像
        private static boolean mirror = false;

        public static void setCameraNum(int num) {
            if (num > 0 && num <= cameraNum) {
                cameraNum = num;
            }
        }

        private static String crashPath;
        private static String apkPath;
        private static int previewCameraId = 0;
        //识别阈值(0-99)
        private static float searchThreshold = 65f;
        //活体阈值(0-99)
        private static float livenessThreshold = 60f;
        //活体检测开关
        private static boolean livenessEnabled = true;
        //红外活体开关
        private static boolean rgbIrLivenessEnabled = false;
        //遮挡使能开关
        private static boolean occlusionFilterEnabled = false;
        //最小人脸尺寸(0-512像素)
        private static int faceMinThreshold = 140;
        //添加人脸时的最小人脸尺寸(0-512像素)
        private static int addFaceMinThreshold = 100;
        //人脸旋转角度阈值（0-90）
        private static float poseRoll = 30f;
        //人脸垂直角度阈值（0-90）
        private static float posePitch = 30f;
        //人脸水平角度阈值（0-90）
        private static float poseYaw = 30f;
        //人脸识别模糊度阈值（0-1），值越大表示照片越模糊，超过该值则不进行识别
        private static float blurThreshold = 0.8f;
        //添加人脸的照片模糊度（0-1）
        private static float addFaceBlurThreshold = 0.6f;
        //人脸亮度最低值(0-255)
        private static float lowBrightnessThreshold = 70f;
        //人脸亮度最高值(0-255)
        private static float highBrightnessThreshold = 210f;
        //人脸对比度阈值(0-255)
        private static float brightnessSTDThreshold = 80f;
        //识别失败时的重试次数 (>=0)
        private static int retryCount = 9999;
        //仅识别最大人脸
        private static boolean maxFaceEnabled = true;
        //抓拍照片保存压缩率（0-100），值越小越模糊
        private static int compressQuality = 75;

        public static int getPreviewOrientation() {
            return previewOrientation;
        }

        public static void setPreviewOrientation(int previewOrientation) {
            Config.previewOrientation = previewOrientation;
        }

        public static int getRecognizeOrientation() {
            return recognizeOrientation;
        }

        public static void setRecognizeOrientation(int recognizeOrientation) {
            Config.recognizeOrientation = recognizeOrientation;
        }

        public static int getCaptureOrientation() {
            return captureOrientation;
        }

        public static void setCaptureOrientation(int captureOrientation) {
            Config.captureOrientation = captureOrientation;
        }

        public static int getCameraWidth() {
            return cameraWidth;
        }

        public static void setCameraWidth(int cameraWidth) {
            Config.cameraWidth = cameraWidth;
        }

        public static int getCameraHeight() {
            return cameraHeight;
        }

        public static void setCameraHeight(int cameraHeight) {
            Config.cameraHeight = cameraHeight;
        }

        public static int getCameraNum() {
            return cameraNum;
        }

        public static boolean isMirror() {
            return mirror;
        }

        public static void setMirror(boolean mirror) {
            Config.mirror = mirror;
        }

        public static String getCrashPath() {
            return crashPath;
        }

        public static void setCrashPath(String crashPath) {
            Config.crashPath = crashPath;
        }

        public static String getApkPath() {
            return apkPath;
        }

        public static void setApkPath(String apkPath) {
            Config.apkPath = apkPath;
        }

        public static int getPreviewCameraId() {
            return previewCameraId;
        }

        public static void setPreviewCameraId(int previewCameraId) {
            Config.previewCameraId = previewCameraId;
        }

        public static float getSearchThreshold() {
            return searchThreshold;
        }

        public static void setSearchThreshold(float searchThreshold) {
            Config.searchThreshold = searchThreshold;
        }

        public static float getLivenessThreshold() {
            return livenessThreshold;
        }

        public static void setLivenessThreshold(float livenessThreshold) {
            Config.livenessThreshold = livenessThreshold;
        }

        public static boolean isLivenessEnabled() {
            return livenessEnabled;
        }

        public static void setLivenessEnabled(boolean livenessEnabled) {
            Config.livenessEnabled = livenessEnabled;
        }

        public static boolean isRgbIrLivenessEnabled() {
            return rgbIrLivenessEnabled;
        }

        public static void setRgbIrLivenessEnabled(boolean rgbIrLivenessEnabled) {
            Config.rgbIrLivenessEnabled = rgbIrLivenessEnabled;
        }

        public static boolean isOcclusionFilterEnabled() {
            return occlusionFilterEnabled;
        }

        public static void setOcclusionFilterEnabled(boolean occlusionFilterEnabled) {
            Config.occlusionFilterEnabled = occlusionFilterEnabled;
        }

        public static int getFaceMinThreshold() {
            return faceMinThreshold;
        }

        public static void setFaceMinThreshold(int faceMinThreshold) {
            Config.faceMinThreshold = faceMinThreshold;
        }

        public static int getAddFaceMinThreshold() {
            return addFaceMinThreshold;
        }

        public static void setAddFaceMinThreshold(int addFaceMinThreshold) {
            Config.addFaceMinThreshold = addFaceMinThreshold;
        }

        public static float getPoseRoll() {
            return poseRoll;
        }

        public static void setPoseRoll(float poseRoll) {
            Config.poseRoll = poseRoll;
        }

        public static float getPosePitch() {
            return posePitch;
        }

        public static void setPosePitch(float posePitch) {
            Config.posePitch = posePitch;
        }

        public static float getPoseYaw() {
            return poseYaw;
        }

        public static void setPoseYaw(float poseYaw) {
            Config.poseYaw = poseYaw;
        }

        public static float getBlurThreshold() {
            return blurThreshold;
        }

        public static void setBlurThreshold(float blurThreshold) {
            Config.blurThreshold = blurThreshold;
        }

        public static float getAddFaceBlurThreshold() {
            return addFaceBlurThreshold;
        }

        public static void setAddFaceBlurThreshold(float addFaceBlurThreshold) {
            Config.addFaceBlurThreshold = addFaceBlurThreshold;
        }

        public static float getLowBrightnessThreshold() {
            return lowBrightnessThreshold;
        }

        public static void setLowBrightnessThreshold(float lowBrightnessThreshold) {
            Config.lowBrightnessThreshold = lowBrightnessThreshold;
        }

        public static float getHighBrightnessThreshold() {
            return highBrightnessThreshold;
        }

        public static void setHighBrightnessThreshold(float highBrightnessThreshold) {
            Config.highBrightnessThreshold = highBrightnessThreshold;
        }

        public static float getBrightnessSTDThreshold() {
            return brightnessSTDThreshold;
        }

        public static void setBrightnessSTDThreshold(float brightnessSTDThreshold) {
            Config.brightnessSTDThreshold = brightnessSTDThreshold;
        }

        public static int getRetryCount() {
            return retryCount;
        }

        public static void setRetryCount(int retryCount) {
            Config.retryCount = retryCount;
        }

        public static boolean isMaxFaceEnabled() {
            return maxFaceEnabled;
        }

        public static void setMaxFaceEnabled(boolean maxFaceEnabled) {
            Config.maxFaceEnabled = maxFaceEnabled;
        }

        public static int getCompressQuality() {
            return compressQuality;
        }

        public static void setCompressQuality(int compressQuality) {
            Config.compressQuality = compressQuality;
        }
    }
}
