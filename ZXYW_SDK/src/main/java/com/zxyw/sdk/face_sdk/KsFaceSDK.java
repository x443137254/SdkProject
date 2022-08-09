package com.zxyw.sdk.face_sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;

import com.srp.AuthApi.AuthApi;
import com.zxyw.sdk.tools.MyLog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import mcv.facepass.FacePassException;
import mcv.facepass.FacePassHandler;
import mcv.facepass.types.FacePassAddFaceResult;
import mcv.facepass.types.FacePassConfig;
import mcv.facepass.types.FacePassDetectFacesResult;
import mcv.facepass.types.FacePassDetectionResult;
import mcv.facepass.types.FacePassImage;
import mcv.facepass.types.FacePassImageType;
import mcv.facepass.types.FacePassModel;
import mcv.facepass.types.FacePassPose;
import mcv.facepass.types.FacePassRecognitionResult;
import mcv.facepass.types.FacePassRecognitionState;
import mcv.facepass.types.FacePassSearchResult;

public class KsFaceSDK implements FaceSDK, CameraDataListener {
    private final String TAG = "KsFaceSDK";
    private final String DEFAULT_GROUP = "defaultGroup";
    private String currentGroup;
    private List<String> groupList;
    private FacePassHandler mFacePassHandler;
    private LinkedBlockingDeque<byte[]> mDetectResultQueue;
    private LinkedBlockingDeque<CameraDataPacket> mFeedFrameQueue;
    private DetectFaceCallback detectFaceCallback;
    private RecognizeCallback recognizeCallback;
    private ExecutorService executorService;
    private boolean running;
    private byte[] lastRbgData;
    private final String SP_NAME = "auth_sp";
    private final String KEY_AUTH = "auth";
    private boolean authStatus;

    @Override
    public void init(final Context context, final List<String> groupList) {
        this.groupList = groupList;
        executorService = Executors.newCachedThreadPool();
        mDetectResultQueue = new LinkedBlockingDeque<>(1);
        mFeedFrameQueue = new LinkedBlockingDeque<>(1);
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        String KEY_TIME = "last_init";
        String KEY_FREQUENCY = "times";
        int frequency = 0;
        final long lastInit = sp.getLong(KEY_TIME, 0);
        MyLog.d(TAG, "last init time: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss", Locale.getDefault())
                        .format(lastInit));
        if ((now - lastInit) < 2500) {
            frequency = sp.getInt(KEY_FREQUENCY, 0) + 1;
            MyLog.d(TAG, "init frequently times: " + frequency);
        }
        sp.edit().putLong(KEY_TIME, now).putInt(KEY_FREQUENCY, frequency).apply();
        if (frequency > 1) {
            final int checkCrashFile = checkCrashFile();
            MyLog.d(TAG, "checkCrashFile: " + checkCrashFile);
            if (checkCrashFile >= 0) {
                sp.edit().putBoolean(KEY_AUTH, false).apply();
                installApk(context);
                return;
            }
        }
        authStatus = sp.getBoolean(KEY_AUTH, false);
        if (authStatus) {
            initFacePassSDK(context);
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    auth(context, "\"{\"\"serial\"\":\"\"r0034d9da44a3ad61a7a53fdb531e53c39534\"\",\"\"key\"\":\"\"81a9c062b2322c026be4682f5a7e28d2de3f06b2b36c2936e319e44452dc754a210c9314a5f3842fbee791ef43027ffcf034ca35b39f3307d7d2025d635c8f365e8220bc0734ab9ac50d3dad529f221f789de9e5e5a2ba9ae656845643cfe1eb3b022a17dee403f33a4f2beca87b992ea84a449e217b38ec564df52074c0806b49e21ec6d6bb1d6e30cbdfbd4ce7a9a029e1b90b0fac48673f56a3189da3c3024094393b83ca552c29195193f77601829c16e9514e2244ff0905215310496a92\"\"}\"\n", null);
                }
            }, 2000);
        }
    }

    private void installApk(final Context context) {
        MyLog.d(TAG, "try reInstall app");
        executorService.execute(() -> {
            if (TextUtils.isEmpty(Config.getApkPath())) {
                MyLog.d(TAG, "apkPath is empty");
                return;
            }
            File folder = new File(Config.getApkPath());
            if (!folder.exists() || folder.isFile()) {
                MyLog.d(TAG, "apkPath error");
                return;
            }
            final File[] apks = folder.listFiles();
            if (apks == null || apks.length == 0) {
                MyLog.d(TAG, "no apks in apkPath");
                return;
            }
            PackageManager pm = context.getPackageManager();
            String apkPath = null;
            PackageInfo apkInfo;
            PackageInfo selfInfo;
            final String packageName = context.getPackageName();
            try {
                selfInfo = pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }
            for (File file : apks) {
                apkInfo = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
                if (apkInfo == null) continue;
                if (packageName.equals(apkInfo.applicationInfo.packageName)
                        && selfInfo.versionCode == apkInfo.versionCode) {
                    apkPath = Config.getApkPath() + File.separator + file;
                    MyLog.d(TAG, "apk is fond");
                    break;
                }
            }
            if (apkPath == null) {
                MyLog.d(TAG, "apk is not fond");
            } else {
                silentInstall(apkPath);
            }
        });
    }

    private void silentInstall(final String apkPath) {
        MyLog.d(TAG, "start install!");
        DataOutputStream dataOutputStream;
        try {
            final Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            String command = "pm install -r " + apkPath + "\n";
            dataOutputStream.write(command.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            dataOutputStream.close();
            process.waitFor();
            executorService.execute(() -> {
                final BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder msg = new StringBuilder();
                String line;
                // 读取命令的执行结果
                while (true) {
                    try {
                        if ((line = inputStream.readLine()) == null) break;
                    } catch (IOException e) {
                        MyLog.d(TAG, e.toString());
                        continue;
                    }
                    msg.append(line);
                }
                MyLog.d(TAG, "the response of installing: " + msg);
            });
            executorService.execute(() -> {
                final BufferedReader errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder msg = new StringBuilder();
                String line;
                // 读取错误提示
                while (true) {
                    try {
                        if ((line = errorStream.readLine()) == null) break;
                    } catch (IOException e) {
                        MyLog.d(TAG, e.toString());
                        continue;
                    }
                    msg.append(line);
                }
                MyLog.d(TAG, "the error of installing: " + msg);
            });
        } catch (Exception e) {
            MyLog.d(TAG, e.toString());
        }
    }

    private void initFacePassSDK(final Context context) {
        executorService.submit(() -> {
            MyLog.d(TAG, "FacePassHandler initSDK start");
            FacePassHandler.initSDK(context);
            while (!FacePassHandler.isAvailable()) {//等待初始化完成
                SystemClock.sleep(500);
            }
            FacePassConfig config = new FacePassConfig();
            /*
             * 配置人脸模型
             */
            config.poseBlurModel = FacePassModel.initModel(context.getAssets(), "attr.pose_blur.align.av200.190630.bin");

            if (Config.getCameraNum() == 1) {//单目使用CPU rgb活体模型
                config.livenessModel = FacePassModel.initModel(context.getAssets(), "liveness.CPU.rgb.int8.E.bin");
            } else {//双目使用CPU rgbir活体模型
                config.rgbIrLivenessModel = FacePassModel.initModel(context.getAssets(), "liveness.CPU.rgbir.int8.E.bin");
            }
            //当单目或者双目有一个使用GPU活体模型时，请设置livenessGPUCache
            //config.livenessGPUCache = FacePassModel.initModel(context.getAssets(), "liveness.GPU.AlgoPolicy.E.cache");
            config.searchModel = FacePassModel.initModel(context.getAssets(), "feat2.arm.H.v1.0_1core.bin");
            config.detectModel = FacePassModel.initModel(context.getAssets(), "detector.arm.E.bin");
            config.detectRectModel = FacePassModel.initModel(context.getAssets(), "detector_rect.arm.E.bin");
            config.landmarkModel = FacePassModel.initModel(context.getAssets(), "pf.lmk.arm.D.bin");
            config.rcAttributeModel = FacePassModel.initModel(context.getAssets(), "attr.RC.gray.12M.arm.200229.bin");
            //config.smileModel = FacePassModel.initModel(mContext.getAssets(), "attr.smile.mgf29.0.1.1.181229.bin");
            //config.ageGenderModel = FacePassModel.initModel(mContext.getAssets(), "attr.age_gender.surveillance.nnie.av200.0.1.0.190630.bin");
            config.occlusionFilterModel = FacePassModel.initModel(context.getAssets(), "occlusion.all_attr_configurable.occ.190816.bin");
            //如果不需要表情和年龄性别功能，smileModel和ageGenderModel可以为null
            //config.smileModel = null;
            //config.ageGenderModel = null;
            /*
             * 配置识别参数
             */
            config.occlusionFilterEnabled = Config.isOcclusionFilterEnabled();
            config.rcAttributeEnabled = false;
            config.searchThreshold = Config.getSearchThreshold();
            config.livenessThreshold = Config.getLivenessThreshold();

            config.livenessEnabled = Config.isLivenessEnabled();
            config.rgbIrLivenessEnabled = Config.isRgbIrLivenessEnabled();

//                ageGenderEnabledGlobal = (config.ageGenderModel != null);
            config.faceMinThreshold = 60;
            config.poseThreshold = new FacePassPose(Config.getPoseRoll(), Config.getPosePitch(), Config.getPoseYaw());
            config.blurThreshold = Config.getBlurThreshold();
            config.lowBrightnessThreshold = Config.getLowBrightnessThreshold();
            config.highBrightnessThreshold = Config.getHighBrightnessThreshold();
            config.brightnessSTDThreshold = Config.getBrightnessSTDThreshold();
            config.retryCount = Config.getRetryCount();
            config.smileEnabled = false;
            config.maxFaceEnabled = Config.isMaxFaceEnabled();

            File file = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (file == null) {
                MyLog.d(TAG, "init failed! file is not exist");
                return;
            }
            config.fileRootPath = file.getAbsolutePath();

            /* 创建SDK实例 */
            try {
                mFacePassHandler = new FacePassHandler(config);
                FacePassConfig addFaceConfig = mFacePassHandler.getAddFaceConfig();
                addFaceConfig.blurThreshold = Config.getAddFaceBlurThreshold();
                addFaceConfig.faceMinThreshold = Config.getAddFaceMinThreshold();
                mFacePassHandler.setAddFaceConfig(addFaceConfig);
                if (Config.getCameraNum() > 1) {
                    mFacePassHandler.setIRConfig(
                            0.99940616D,
                            0.03817749D,
                            0.99697757D,
                            -2.1230164D,
                            0.5D);
                }
                checkGroup();
            } catch (FacePassException e) {
                MyLog.d(TAG, "init failed! " + e.toString());
                return;
            }
            running = true;
            startRecognizeThread();
            startFeedFrameThread();
            MyLog.d(TAG, "FacePassHandler initSDK finish");
        });
    }

    private void startFeedFrameThread() {
        executorService.submit(() -> {
            MyLog.d(TAG, "start FeedFrameThread");
            CameraDataPacket dataPacket;
            while (running) {
                try {
                    dataPacket = mFeedFrameQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                if (dataPacket == null || mFacePassHandler == null) {
                    continue;
                }
                FacePassDetectionResult detectionResult;
                try {
                    if (Config.getCameraNum() == 1) {
                        detectionResult = mFacePassHandler.feedFrame(dataPacket.getImageRGB());
                    } else if (dataPacket.getImageIR() != null) {
                        detectionResult = mFacePassHandler.feedFrameRGBIR(dataPacket.getImageRGB(), dataPacket.getImageIR());
                    } else {
                        continue;
                    }
                } catch (FacePassException e) {
                    continue;
                }
                if (detectionResult != null) {
                    if (detectFaceCallback != null) {
                        RectF[] rectList = new RectF[detectionResult.faceList.length];
                        for (int i = 0; i < detectionResult.faceList.length; i++) {
                            rectList[i] = new RectF();
                            rectList[i].left = detectionResult.faceList[i].rect.left;
                            rectList[i].right = detectionResult.faceList[i].rect.right;
                            rectList[i].top = detectionResult.faceList[i].rect.top;
                            rectList[i].bottom = detectionResult.faceList[i].rect.bottom;
                            rectList[i] = convert(rectList[i]);
                        }
                        detectFaceCallback.onDetectFace(rectList);
                    }
                    if (detectionResult.message.length > 0) {
                        MyLog.d(TAG, "offer detectionResult，DetectResultQueue.size=" + mDetectResultQueue.size());
                        if ((detectionResult.faceList[0].rect.right - detectionResult.faceList[0].rect.left) > Config.getFaceMinThreshold()) {
                            if (mDetectResultQueue.size() > 0) mDetectResultQueue.poll();
                            mDetectResultQueue.offer(detectionResult.message);
                        } else {
                            if (recognizeCallback != null) {
                                recognizeCallback.recognizeResult(false, "请再靠近一些");
                            }
                        }
                    }
                }
            }
        });
    }

    private RectF convert(RectF rect) {
        boolean mirror = FaceSDK.Config.isMirror();
        int previewWidth = detectFaceCallback.getMeasuredWidth();
        int previewHeight = detectFaceCallback.getMeasuredHeight();
        int cameraWidth = FaceSDK.Config.getCameraWidth();
        int cameraHeight = FaceSDK.Config.getCameraHeight();
        float l = 0, r = 0, t = 0, b = 0;
        Matrix mat = new Matrix();
        switch (FaceSDK.Config.getRecognizeOrientation()) {
            case 0:
                l = rect.left;
                t = rect.top;
                r = rect.right;
                b = rect.bottom;
                mat.setScale(mirror ? -1 : 1, 1);
                mat.postTranslate(mirror ? (float) cameraWidth : 0f, 0f);
                mat.postScale((float) previewWidth / (float) cameraWidth, (float) previewHeight / (float) cameraHeight);
                break;
            case 90:
                l = rect.top;
                t = cameraWidth - rect.right;
                r = rect.bottom;
                b = cameraWidth - rect.left;
                mat.setScale(mirror ? -1 : 1, 1);
                mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                mat.postScale((float) previewWidth / (float) cameraHeight, (float) previewHeight / (float) cameraWidth);
                break;
            case 180:
                l = rect.right;
                t = rect.bottom;
                r = rect.left;
                b = rect.top;
                mat.setScale(1, mirror ? -1 : 1);
                mat.postTranslate(0f, mirror ? (float) cameraHeight : 0f);
                mat.postScale((float) previewWidth / (float) cameraWidth, (float) previewHeight / (float) cameraHeight);
                break;
            case 270:
                l = cameraHeight - rect.bottom;
                t = rect.left;
                r = cameraHeight - rect.top;
                b = rect.right;
                mat.setScale(mirror ? -1 : 1, 1);
                mat.postTranslate(mirror ? (float) cameraHeight : 0f, 0f);
                mat.postScale((float) previewWidth / (float) cameraHeight, (float) previewHeight / (float) cameraWidth);
        }
        RectF drect = new RectF();
        RectF srect = new RectF(l, t, r, b);

        mat.mapRect(drect, srect);
        return drect;
    }

    private void startRecognizeThread() {
        executorService.submit(() -> {
            byte[] detectionResult;
            FacePassRecognitionResult[] recognizeResult;
            MyLog.d(TAG, "start RecognizeThread");
            while (running) {
                try {
                    detectionResult = mDetectResultQueue.take();
                    if (recognizeCallback == null) {
                        MyLog.d(TAG, "recognizeCallback is null");
                        continue;
                    }
                    //检查底库，如果底库没有添加人脸，SDK直接不进行识别返回空数据，所以数据不提交SDK识别直接返回
                    if (mFacePassHandler.getLocalGroupFaceNum(getCurrentGroup()) <= 0) {
                        MyLog.d(TAG, "recognize : group is empty");
                        recognizeCallback.recognizeResult(false, "人脸未注册");
                        continue;
                    }
                    //人脸数据提交SDK进行识别
                    recognizeResult = mFacePassHandler.recognize(getCurrentGroup(), detectionResult);
                } catch (FacePassException | InterruptedException e) {
                    MyLog.d(TAG, "recognize : " + e.toString());
                    continue;
                }
                if (recognizeResult == null || recognizeResult.length == 0) {
                    MyLog.d(TAG, "recognize : Result is null");
                    if (recognizeCallback != null) {
                        recognizeCallback.recognizeResult(false, "人脸识别失败");
                    }
                    continue;
                }
                for (FacePassRecognitionResult result : recognizeResult) {
                    if (recognizeCallback != null) {
                        if (result.recognitionState == FacePassRecognitionState.RECOGNITION_PASS) {
                            recognizeCallback.recognizeResult(true, new String(result.faceToken));
                        } else {
                            String tip;
                            if (result.detail.searchScore < result.detail.searchThreshold) {
                                MyLog.d(TAG, "recognize : searchScore=" + result.detail.searchScore + " searchThreshold=" + result.detail.searchThreshold);
                                tip = "人脸未注册";
                            } else if (result.detail.livenessScore < result.detail.livenessThreshold) {
                                MyLog.d(TAG, "recognize : livenessScore=" + result.detail.livenessScore + " livenessThreshold=" + result.detail.livenessThreshold);
                                tip = "检测失败";
                            } else {
                                MyLog.d(TAG, "recognize :");
                                tip = "";
                            }
                            recognizeCallback.recognizeResult(false, tip);
                        }
                    }
//                    MyLog.d(TAG, "recognize : trackId reset");
//                    mFacePassHandler.setMessage(result.trackId, FacePassTrackIdState.TRACK_ID_RETRY);
                }
            }
        });
    }

    private void checkGroup() throws FacePassException {
        MyLog.d(TAG, "checkGroup...");
        String[] localGroups = mFacePassHandler.getLocalGroups();
        if (groupList != null && groupList.size() > 0) {
            for (String groupName : groupList) {
                boolean needCreateGroup = true;
                if (localGroups != null && localGroups.length > 0) {
                    for (String group : localGroups) {
                        if (groupName.equals(group)) {
                            MyLog.d(TAG, "init Group " + groupName);
                            mFacePassHandler.initLocalGroup(groupName);
                            needCreateGroup = false;
                            break;
                        }
                    }
                }
                if (needCreateGroup) {
                    createGroup(groupName);
                }
            }
        } else {
            if (localGroups != null && localGroups.length > 0) {
                for (String group : localGroups) {
                    if (DEFAULT_GROUP.equals(group)) {
                        MyLog.d(TAG, "init Group " + DEFAULT_GROUP);
                        mFacePassHandler.initLocalGroup(DEFAULT_GROUP);
                        return;
                    }
                }
            }
            createGroup(DEFAULT_GROUP);
        }
    }

    private void createGroup(String groupName) throws FacePassException {
        if (mFacePassHandler == null) {
            MyLog.d(TAG, "create group failed! mFacePassHandler is null");
            return;
        }
        boolean success = mFacePassHandler.createLocalGroup(groupName);
        MyLog.d(TAG, "create Group " + groupName + " " + success);
    }

    @Override
    public void addFace(String photoPath, AddFaceCallback addFaceCallback) {
        File imageFile = new File(photoPath);
        if (!imageFile.exists()) {
            if (addFaceCallback != null) {
                addFaceCallback.addResult(false, "图片文件不存在");
            }
            MyLog.d(TAG, "addFace error:图片文件不存在");
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        if (bitmap == null) {
            if (addFaceCallback != null) {
                addFaceCallback.addResult(false, "照片读取失败");
            }
            return;
        }
        if (mFacePassHandler == null) {
            if (addFaceCallback != null) {
                addFaceCallback.addResult(false, "SDK未初始化");
            }
            MyLog.d(TAG, "addFace error: mFacePassHandler is null");
            return;
        }
        try {
            FacePassAddFaceResult result = mFacePassHandler.addFace(bitmap);
            if (result != null) {
                if (result.result == 0) {//添加成功
                    String token = new String(result.faceToken);
                    boolean bindSuccess = mFacePassHandler.bindGroup(getCurrentGroup(), result.faceToken);
                    if (bindSuccess) {
                        MyLog.d(TAG, "addFace success! faceToken=" + token);
                        if (addFaceCallback != null) {
                            addFaceCallback.addResult(true, token);
                        }
                    } else {
                        MyLog.d(TAG, "addFace failed：bind group error");
                        if (addFaceCallback != null) {
                            addFaceCallback.addResult(false, "绑定底库失败");
                        }
                    }
                } else if (result.result == 1) {
                    MyLog.d(TAG, "addFace failed：no face");
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "没有检测到人脸");
                    }
                } else {
                    MyLog.d(TAG, "addFace failed：quality low");
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "人脸照片太模糊");
                    }
                }
            } else {
                MyLog.d(TAG, "addFace failed：人脸添加失败");
                if (addFaceCallback != null) {
                    addFaceCallback.addResult(false, "人脸添加失败");
                }
            }
        } catch (FacePassException e) {
            String message = e.toString();
            MyLog.d(TAG, "addFace failed：" + message);
            if (addFaceCallback != null) {
                addFaceCallback.addResult(false, message);
            }
        }
    }

    @Override
    public void deleteFace(String faceToken) {
        if (TextUtils.isEmpty(faceToken)) {
            MyLog.d(TAG, "deleteFace failed! faceToken is empty");
            return;
        }
        if (mFacePassHandler != null) {
            try {
                boolean b = mFacePassHandler.deleteFace(faceToken.getBytes());
                MyLog.d(TAG, "deleteFace faceToken=" + faceToken + " success=" + b);
            } catch (FacePassException e) {
                MyLog.d(TAG, "deleteFace error! " + e.toString());
            }
        } else {
            MyLog.d(TAG, "deleteFace failed! mFacePassHandler is null");
        }
    }

    @Override
    public void clearFace() {
        if (mFacePassHandler != null) {
            try {
                MyLog.d(TAG, "delete all group");
                if (groupList != null && groupList.size() > 0) {
                    for (String s : groupList) {
                        mFacePassHandler.deleteLocalGroup(s);
                    }
                } else {
                    mFacePassHandler.deleteLocalGroup(DEFAULT_GROUP);
                }
                mFacePassHandler.reset();
                checkGroup();
            } catch (FacePassException e) {
                MyLog.d(TAG, e.toString());
            }
        }
    }

    private String getCurrentGroup() {
        if (currentGroup == null) {
            if (groupList != null && groupList.size() > 0) {
                currentGroup = groupList.get(0);
            } else {
                currentGroup = DEFAULT_GROUP;
            }
        }
        return currentGroup;
    }

    @Override
    public void setCurrentGroup(String group) {
        this.currentGroup = group;
    }

    @Override
    public void clearFace(String groupName) {
        if (mFacePassHandler != null) {
            try {
                MyLog.d(TAG, "delete group: " + groupName);
                mFacePassHandler.deleteLocalGroup(groupName);
                mFacePassHandler.reset();
                checkGroup();
            } catch (FacePassException e) {
                MyLog.d(TAG, e.toString());
            }
        }
    }

    @Override
    public boolean capture(String savePath) {
        if (lastRbgData == null) {
            MyLog.d(TAG, "capture save failed! last frame data is null");
            return false;
        }
        FacePassImage imageRGB = null;
        try {
            imageRGB = new FacePassImage(
                    lastRbgData.clone(),
                    Config.getCameraWidth(),
                    Config.getCameraHeight(),
                    Config.getCaptureOrientation(),
                    FacePassImageType.NV21);
        } catch (FacePassException e) {
            MyLog.d(TAG, e.toString());
        }
        if (imageRGB == null) {
            MyLog.d(TAG, "capture save failed! create FacePassImage data error");
            return false;
        }
        YuvImage yuvImage = new YuvImage(imageRGB.image, ImageFormat.NV21, imageRGB.width, imageRGB.height, null);
        File file = new File(savePath);
        if (!file.exists() || file.isDirectory()) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists()) {
                    if (!parentFile.mkdirs()) {
                        MyLog.d(TAG, "capture save failed! can not create file folder");
                        return false;
                    }
                }
            }
            try {
                if (!file.createNewFile()) {
                    MyLog.d(TAG, "capture save failed! can not create file");
                    return false;
                }
            } catch (IOException e) {
                MyLog.d(TAG, "exception occur during create file! " + e.toString());
                return false;
            }
        }
        try {
            FileOutputStream fosImage = new FileOutputStream(file);
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), Config.getCompressQuality(), fosImage);
            fosImage.close();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Matrix matrix = new Matrix();
            matrix.postRotate(imageRGB.facePassImageRotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            fosImage = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, Config.getCompressQuality(), fosImage);
            fosImage.close();
            MyLog.d(TAG, "capture save success! path=" + savePath);
            return true;
        } catch (IOException e) {
            MyLog.d(TAG, "capture save failed! " + e.toString());
            return false;
        }
    }

    @Override
    public void auth(final Context context, String cert, final AuthCallback callback) {
        if (TextUtils.isEmpty(cert)) {
            if (callback != null) {
                callback.authResult(false);
            }
            MyLog.d(TAG, "auth failed! cert is empty");
        } else {
            new AuthApi().authDevice(context, cert, "", authApplyResponse -> {
                authStatus = authApplyResponse.errorCode == 0;
                MyLog.d(TAG, "auth " + authStatus);
                if (authStatus) {
                    context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTH, true).apply();
                    initFacePassSDK(context);
                }
                if (callback != null) {
                    callback.authResult(authStatus);
                }
            });
        }
    }

    @Override
    public void release() {
        MyLog.d(TAG, "FaceSDK release");
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mFeedFrameQueue != null) {
            mFeedFrameQueue.clear();
        }
        if (mDetectResultQueue != null) {
            mDetectResultQueue.clear();
        }
        if (mFacePassHandler != null) {
            mFacePassHandler.release();
        }
    }

    @Override
    public boolean isActive() {
        return authStatus;
    }

    @Override
    public String compare(String photoPath) {
        if (mFacePassHandler == null) {
            MyLog.d(TAG,"compare failed! SDK has not init");
            return null;
        }
        final Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        final FacePassDetectFacesResult detectFaces;
        try {
            detectFaces = mFacePassHandler.detectFaces(bitmap, 0);
        } catch (FacePassException e) {
            e.printStackTrace();
            MyLog.d(TAG,"compare failed! detectFaces error: " + e.toString());
            return null;
        }
        if (detectFaces == null || detectFaces.faceList == null || detectFaces.faceList.length == 0) {
            MyLog.d(TAG,"compare failed! no faces found");
            return null;
        }
        final FacePassSearchResult[] searchResults;
        try {
            searchResults = mFacePassHandler.compare1xN(
                    bitmap,
                    0,
                    detectFaces.faceList[0].lData,
                    getCurrentGroup(),
                    mFacePassHandler.getLocalGroupFaceNum(getCurrentGroup()));
        } catch (FacePassException e) {
            e.printStackTrace();
            MyLog.d(TAG,"compare failed! compare1xN error: " + e.toString());
            return null;
        }
        if (searchResults == null || searchResults.length == 0){
            MyLog.d(TAG,"compare failed! no searchResults");
            return null;
        }
        if (searchResults[0].searchScore > FaceSDK.Config.getSearchThreshold()){
            return new String(searchResults[0].faceToken);
        }
        MyLog.d(TAG,"compare failed! max searchScore=" + searchResults[0].searchScore);
        return null;
    }

    @Override
    public void onPreviewCallback(byte[] rgbData, byte[] irData) {
        if (!running) return;
        lastRbgData = rgbData;
        if (mFeedFrameQueue.size() > 0) mFeedFrameQueue.poll();
        CameraDataPacket packet = null;
        try {
            packet = new CameraDataPacket(rgbData, irData, Config.getCameraWidth(), Config.getCameraHeight(), Config.getRecognizeOrientation());
        } catch (FacePassException e) {
            e.printStackTrace();
        }
        if (packet != null) {
            mFeedFrameQueue.offer(packet);
        }
    }

    @Override
    public void setDetectFaceCallback(DetectFaceCallback detectFaceCallback) {
        this.detectFaceCallback = detectFaceCallback;
    }

    @Override
    public void setRecognizeCallback(RecognizeCallback recognizeCallback) {
        this.recognizeCallback = recognizeCallback;
    }

    static class CameraDataPacket {
        private FacePassImage imageRGB;
        private FacePassImage imageIR;

        public CameraDataPacket(byte[] rgb, byte[] ir, int width, int height, int rotation) throws FacePassException {
            if (rgb != null) {
                imageRGB = new FacePassImage(rgb.clone(), width, height, rotation, FacePassImageType.NV21);
            }
            if (ir != null) {
                imageIR = new FacePassImage(ir.clone(), width, height, rotation, FacePassImageType.NV21);
            }
        }

        FacePassImage getImageRGB() {
            return imageRGB;
        }

        FacePassImage getImageIR() {
            return imageIR;
        }
    }

    /**
     * 检查频繁启动原因，是否有报错日志
     *
     * @return -1 上次启动有报错日志，但不是UnsatisfiedLinkError，程序其他问题导致的崩溃，不予理会
     * 0 上次启动没有报错日志，可能是授权丢失被强制退出进程，需要重新走授权流程（so文件可能同时丢失，重置授权状态后重装比较安全一点）
     * 1 上次启动有报错日志，并且是UnsatisfiedLinkError，so文件丢失，需要重新安装app
     */
    private int checkCrashFile() {
        if (TextUtils.isEmpty(Config.getCrashPath())) return 0;
        File file = new File(Config.getCrashPath());
        if (!file.exists() || file.isFile()) return 0;
        final String[] list = file.list();
        if (list == null || list.length < 1) return 0;
        long t;
        try {
            String[] split = list[list.length - 1].split("\\.")[0].split("-");
            t = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(split[1]).getTime();
        } catch (Exception e) {
            return 0;
        }
        if ((System.currentTimeMillis() - t) < 5000) {
            try (BufferedReader reader = new BufferedReader(new FileReader(Config.getCrashPath() + File.separator + list[list.length - 1]))) {
                String s = reader.readLine();
                while (s != null) {
                    if (s.contains("UnsatisfiedLinkError")) {
                        return 1;
                    }
                    s = reader.readLine();
                }
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }
}
