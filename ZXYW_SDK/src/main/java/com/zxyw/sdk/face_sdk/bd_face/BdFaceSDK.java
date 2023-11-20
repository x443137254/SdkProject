package com.zxyw.sdk.face_sdk.bd_face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.baidu.idl.main.facesdk.FaceDarkEnhance;
import com.baidu.idl.main.facesdk.FaceDetect;
import com.baidu.idl.main.facesdk.FaceFeature;
import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.FaceLive;
import com.baidu.idl.main.facesdk.model.BDFaceDetectListConf;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
import com.baidu.idl.main.facesdk.model.BDFaceInstance;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.model.BDFaceSDKConfig;
import com.baidu.idl.main.facesdk.model.Feature;
import com.zxyw.sdk.auth.AESUtil;
import com.zxyw.sdk.auth.Auth;
import com.zxyw.sdk.face_sdk.FaceSDK;
import com.zxyw.sdk.face_sdk.bd_face.model.CameraFrame;
import com.zxyw.sdk.face_sdk.bd_face.model.DetectBean;
import com.zxyw.sdk.face_sdk.bd_face.model.GlobalSet;
import com.zxyw.sdk.face_sdk.bd_face.model.SingleBaseConfig;
import com.zxyw.sdk.net.http.HttpUtil;
import com.zxyw.sdk.speaker.Speaker;
import com.zxyw.sdk.tools.MyLog;
import com.zxyw.sdk.tools.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BdFaceSDK implements FaceSDK {
    private final String TAG = "DbFaceSDK";
    private final String spName = "auth_cache";
    private final static String KEY_UUID = "uuid";
    private final String keyName = "sn";
    private FaceAuth faceAuth;
    private FaceDetect faceDetect;
    private FaceDetect faceDetectNir;
    private FaceFeature faceFeature;
    private FaceLive faceLiveness;
    private FaceDarkEnhance faceDarkEnhance;
//    private final Map<String, FaceFeature> faceFeatureMap = new HashMap<>();

    private FeatureBd db;
    private boolean init;
    private final BlockingQueue<CameraFrame> cameraFrameList = new LinkedBlockingQueue<>();
    private final BlockingQueue<DetectBean> detectBeanList = new LinkedBlockingQueue<>();

    private float scaleW;
    private float scaleH;
    private DetectFaceCallback detectFaceCallback;
    private RecognizeCallback recognizeCallback;

    //    private List<String> groupList;
    private final String groupName = "defaultGroup";
    private boolean running;
    private InitFinishCallback initFinishCallback;
    private Thread featureThread;
    private Thread trackThread;
    private Thread addThread;
    private final Map<String, AddFaceCallback> addFaceMap = new HashMap<>();
    private final Object addFaceLock = new Object();
    private final Object recognizeLock = new Object();
    private byte[] cacheFrame;

    @Override
    public void init(final Context context, final List<String> groupList, final String url, InitFinishCallback callback) {
//        if (groupList != null) {
//            this.groupList = groupList;
//            if (this.groupList.isEmpty()) {
//                this.groupList.add("defaultGroup");
//            }
//        } else {
//            this.groupList = new ArrayList<>();
//            this.groupList.add("defaultGroup");
//        }
        initFinishCallback = callback;
//        String s = checkLostFile();
//        MyLog.d(TAG, "missing file: " + s);
//        if (s != null && !s.equals("")) {
//            if (!replaceFile(context, new File(s))) return;
//        }
        faceAuth = new FaceAuth();
        faceAuth.setActiveLog(BDFaceSDKCommon.BDFaceLogInfo.BDFACE_LOG_ERROR_MESSAGE, 0);
        faceAuth.setCoreConfigure(BDFaceSDKCommon.BDFaceCoreRunMode.BDFACE_LITE_POWER_NO_BIND, 2);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getCertificate(context, url, null);
            }
        }, 3000);
    }

    private void toast(Context context, String s) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, s, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void getCertificate(Context context, String url, AuthCallback callback) {
        String cert = context.getSharedPreferences(spName, Context.MODE_PRIVATE).getString(keyName, null);
        if (cert == null) {
            getCertOnline(context, url, callback);
        } else {
            authOnline(cert, context, callback, false);
        }
    }

    public void clearCert(Context context) {
        context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().remove(keyName).apply();
    }

    public void getCertOnline(Context context, String url, AuthCallback callback) {
        if (!TextUtils.isEmpty(url)) {
            String uuid = context.getSharedPreferences(spName, Context.MODE_PRIVATE).getString(KEY_UUID, null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString(KEY_UUID, uuid).apply();
            }
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("android_id", Utils.string2MD5(Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)));
                jsonObject.put("cpu_sn", a());
                jsonObject.put("uuid", Utils.string2MD5(uuid));
                jsonObject.put("bd_device_id", new FaceAuth().getDeviceId(context));
                jsonObject.put("sn", Auth.readSN(context));
            } catch (JSONException e) {
                if (callback != null) {
                    callback.authResult(false);
                }
                if (initFinishCallback != null) {
                    initFinishCallback.initFinish(false);
                    initFinishCallback = null;
                }
                toast(context, e.toString());
                return;
            }
            final String data = jsonObject.toString();
            MyLog.d(TAG, "http request body: " + data);
            HttpUtil.post(url, AESUtil.encode(data), new HttpUtil.HttpCallback() {
                @Override
                public void onFailed(String error) {
                    if (callback != null) {
                        callback.authResult(false);
                    }
                    if (initFinishCallback != null) {
                        initFinishCallback.initFinish(false);
                        initFinishCallback = null;
                    }
                    toast(context, error);
                }

                @Override
                public void onSuccess(String bodyString) {
                    JSONObject json;
                    try {
                        json = new JSONObject(bodyString);
                    } catch (JSONException e) {
                        MyLog.e(TAG, "获取百度人脸识别激活码失败！返回数据格式错误");
                        if (callback != null) {
                            callback.authResult(false);
                        }
                        if (initFinishCallback != null) {
                            initFinishCallback.initFinish(false);
                            initFinishCallback = null;
                        }
                        toast(context, "获取百度人脸识别激活码失败！数据格式错误");
                        return;
                    }
                    if (json.optInt("status") == 200) {
                        final String s;
                        try {
                            s = json.optString("result");
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (callback != null) {
                                callback.authResult(false);
                            }
                            if (initFinishCallback != null) {
                                initFinishCallback.initFinish(false);
                                initFinishCallback = null;
                            }
                            toast(context, "获取百度人脸识别激活码失败！result is null");
                            return;
                        }
                        if (!TextUtils.isEmpty(s)) {
                            authOnline(s, context, callback, true);
                        }
                    } else {
                        final String s = "获取百度人脸识别激活码失败！" + json.optString("msg");
                        MyLog.e(TAG, s);
                        if (initFinishCallback != null) {
                            initFinishCallback.initFinish(false);
                            initFinishCallback = null;
                        }
                        toast(context, s);
                    }
                }
            });
        } else {
            final String s = "获取百度人脸识别激活码失败！未设置自动激活url";
            MyLog.e(TAG, s);
            if (initFinishCallback != null) {
                initFinishCallback.initFinish(false);
                initFinishCallback = null;
            }
            toast(context, s);
        }
    }

    public void authInput(@NonNull String cert, @NonNull Context context, @Nullable AuthCallback callback) {
        faceAuth.initLicenseOnLine(context, cert, (code, response) -> {
            if (code == 0) {
                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString(keyName, AESUtil.encode(cert)).apply();
                if (callback != null) callback.authResult(true);
                onAuthSuccess(context);
            } else if (callback != null) {
                callback.authResult(false);
            }
        });
    }

    private void authOnline(String cert, Context context, AuthCallback callback, boolean toast) {
        faceAuth.initLicenseOnLine(context, AESUtil.decode(cert), (code, response) -> {
            if (code == 0) {
                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString(keyName, cert).apply();
                if (callback != null) callback.authResult(true);
                onAuthSuccess(context);
                if (toast) {
                    Speaker.getInstance().speak("人脸识别激活成功");
                }
//            } else if (!active) {//如果激活失败并且使用本地激活码的时候，清除激活码重新在线获取一次激活码
//                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().remove(keyName).apply();
//                getCertificate(context, authUrl, null);
            } else {
                if (callback != null) {
                    callback.authResult(false);
                }
                if (initFinishCallback != null) {
                    initFinishCallback.initFinish(false);
                    initFinishCallback = null;
                }
                if (toast) {
                    Speaker.getInstance().speak("人脸识别激活失败");
                }
                toast(context, "百度人脸识别授权失败");
            }
        });
    }

    private String a() {
        String s1, s2 = "0000000000000000";
        try {
            Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                s1 = input.readLine();
                if (s1 != null) {
                    if (s1.contains("Serial")) {
                        s2 = s1.substring(s1.indexOf(":") + 1).trim();
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return s2;
    }

    private void onAuthSuccess(Context context) {
        MyLog.d(TAG, "active check success!");
        BDFaceInstance bdFaceInstance = new BDFaceInstance();
        bdFaceInstance.creatInstance();
        faceDetect = new FaceDetect(bdFaceInstance);
        if (!Config.isSingleCamera()) {
            BDFaceInstance IrBdFaceInstance = new BDFaceInstance();
            IrBdFaceInstance.creatInstance();
            faceDetectNir = new FaceDetect(IrBdFaceInstance);
        }
//        for (String groupName : groupList) {
//            faceFeatureMap.put(groupName, new FaceFeature());
//        }
        faceFeature = new FaceFeature();

        faceLiveness = new FaceLive();
        // 暗光檢測
        faceDarkEnhance = new FaceDarkEnhance();
        //加载配置
        initConfig();
        //加载各种模型
        initModel(context);
        //加载数据库中人脸数据
        initDatabases(context);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                init = true;
                if (initFinishCallback != null) {
                    initFinishCallback.initFinish(true);
                    initFinishCallback = null;
                }
                running = true;
                trackFace();
                featureFace();
                startAddFaceThread();
            }
        }, 3000);
    }

    private void startAddFaceThread() {
        addThread = new Thread(() -> {
            while (running) {
                try {
                    if (addFaceMap.isEmpty()) {
                        synchronized (recognizeLock) {
                            recognizeLock.notifyAll();
                        }
                        synchronized (addFaceLock) {
                            addFaceLock.wait();
                        }
                    }
                    MyLog.d(TAG, "addThread active, map size=" + addFaceMap.size());
                    final Iterator<String> iterator = addFaceMap.keySet().iterator();
                    if (iterator.hasNext()) {
                        final String photoPath = iterator.next();
                        _addFace(photoPath, addFaceMap.get(photoPath));
                        addFaceMap.remove(photoPath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        addThread.start();
    }

    /**
     * 活体检测、人脸特征检测、人脸比对
     */
    private void featureFace() {
        featureThread = new Thread(() -> {
            while (running) {
                try {
//                    MyLog.d(TAG, "featureThread: add list size=" + addFaceMap.size());
                    if (addFaceMap.size() > 0) {
                        synchronized (addFaceLock) {
                            addFaceLock.notifyAll();
                        }
                        synchronized (recognizeLock) {
                            recognizeLock.wait();
                        }
                    }
                    final DetectBean detectBean = detectBeanList.take();
//                    MyLog.d(TAG, "detectBeanList take, size left=" + detectBeanList.size());
                    if (detectBean == null
                            || detectBean.rgbImage == null
                            || detectBean.fastFaceInfos == null
                            || detectBean.fastFaceInfos.length == 0) {
                        if (detectBean != null && detectBean.rgbImage != null) {
                            detectBean.rgbImage.destory();
                        }
                        MyLog.d(TAG, "detectBean is null");
                        continue;
                    }
                    BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
                    bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
                            = SingleBaseConfig.getBaseConfig().isQualityControl();
                    bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
                    final FaceInfo[] faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
                            BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                            detectBean.rgbImage, detectBean.fastFaceInfos, bdFaceDetectListConfig);

                    //rgb活体分数
                    float rgbScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
                            detectBean.rgbImage, faceInfos[0].landmarks);
                    if (rgbScore < SingleBaseConfig.getBaseConfig().getRgbLiveScore()) {
                        MyLog.d(TAG, "活体检测失败: rgb score=" + rgbScore);
                        detectBean.rgbImage.destory();
                        continue;
                    }

                    if (detectBean.irData != null) {
                        BDFaceImageInstance irImage = new BDFaceImageInstance(detectBean.irData, Config.getCameraHeight(),
                                Config.getCameraWidth(), BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_YUV_NV21,
                                SingleBaseConfig.getBaseConfig().getNirDetectDirection(),
                                SingleBaseConfig.getBaseConfig().getMirrorDetectNIR());
                        BDFaceDetectListConf bdFaceDetectListConf = new BDFaceDetectListConf();
                        bdFaceDetectListConf.usingDetect = true;
                        FaceInfo[] faceInfosIr = faceDetectNir.detect(BDFaceSDKCommon.DetectType.DETECT_NIR,
                                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_NIR_ACCURATE,
                                irImage, null, bdFaceDetectListConf);
                        if (faceInfosIr != null && faceInfosIr.length > 0) {
                            float irScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_NIR,
                                    irImage, faceInfosIr[0].landmarks);
                            irImage.destory();
                            if (irScore * 100 < Config.getLivenessThreshold()) {
                                MyLog.d(TAG, "活体检测失败: ir score=" + irScore);
                                detectBean.rgbImage.destory();
                                continue;
                            }
                        } else {
                            MyLog.d(TAG, "活体检测失败: ir detect is null");
                            irImage.destory();
                            detectBean.rgbImage.destory();
                            continue;
                        }
                    }

                    //活体检查通过，开始提取特征及比对搜索
                    final List<String> faceTokenList = new ArrayList<>();
                    byte[] featureBytes = new byte[512];
//                    final FaceFeature faceFeature = faceFeatureMap.get(getCurrentGroup());
                    if (faceFeature != null) {
                        for (FaceInfo faceInfo : faceInfos) {
                            float featureSize = faceFeature.feature(
                                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                                    detectBean.rgbImage, faceInfo.landmarks, featureBytes);
                            if ((int) featureSize == GlobalSet.FEATURE_SIZE / 4) {
                                ArrayList<Feature> featureResult = faceFeature.featureSearch(featureBytes,
                                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
                                if (featureResult != null && featureResult.size() > 0) {
                                    final Feature feature = featureResult.get(0);
                                    if (feature != null && feature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
                                        Feature query = db.query(feature.getId(), groupName);
                                        if (query != null) {
                                            faceTokenList.add(String.valueOf(query.getId()));
                                            MyLog.d(TAG, "recognize success! query success! id=" + query.getId());
                                        } else {
                                            MyLog.e(TAG, "recognize success! query failed! id=" + feature.getId());
                                        }
                                    } else {
                                        MyLog.e(TAG, "recognize success! featureSearch failed, score=" + (feature == null ? 0 : feature.getScore()));
                                    }
                                } else {
                                    MyLog.e(TAG, "recognize success! featureSearch failed");
                                }
                            } else {
                                MyLog.e(TAG, "recognize failed! feature failed");
                            }
                        }
                    } else {
                        MyLog.e(TAG, "current group setting error! faceFeature is null");
                    }
                    detectBean.rgbImage.destory();
                    if (recognizeCallback != null) {
                        if (faceTokenList.size() > 0) {
                            recognizeCallback.recognizeSuccess(faceTokenList);
                        } else {
                            recognizeCallback.recognizeFailed("验证不通过");
                        }
                    }
                } catch (Exception e) {
                    MyLog.d(TAG, "Face detect error! " + e.toString());
                }
            }
        });
        featureThread.start();
    }

    /**
     * 查找人脸
     */
    private void trackFace() {
        trackThread = new Thread(() -> {
            while (running) {
                try {
                    if (addFaceMap.size() > 0 && detectBeanList.isEmpty()) {
                        synchronized (addFaceLock) {
                            addFaceLock.notifyAll();
                        }
                    }
                    if (addFaceMap.size() > 0) {
                        synchronized (recognizeLock) {
                            recognizeLock.wait();
                        }
                    }
                    CameraFrame cameraFrame = cameraFrameList.take();
                    if (cameraFrame == null) continue;
                    BDFaceImageInstance rgbInstance = new BDFaceImageInstance(cameraFrame.rgbData,
                            Config.getCameraHeight(), Config.getCameraWidth(),
                            BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_YUV_NV21,
                            SingleBaseConfig.getBaseConfig().getRgbDetectDirection(),
                            SingleBaseConfig.getBaseConfig().getMirrorDetectRGB());
                    // 判断暗光恢复
                    if (SingleBaseConfig.getBaseConfig().isDarkEnhance()) {
                        rgbInstance = faceDarkEnhance.faceDarkEnhance(rgbInstance);
                    }
                    // 快速检测获取人脸信息
                    FaceInfo[] faceInfos = faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
                            BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, rgbInstance);
//                    MyLog.d(TAG, "trackFace: face num: " + (faceInfos == null ? 0 : faceInfos.length));
                    if (faceInfos != null && faceInfos.length > 0) {
                        if (detectFaceCallback != null) {//绘制人脸框
                            RectF[] rectfs = new RectF[faceInfos.length];
                            if (scaleW == 0) {
                                boolean p = Config.getPreviewOrientation() % 180 == 0;
                                scaleW = detectFaceCallback.getMeasuredWidth() * 1.0f / (p ? Config.getCameraWidth() : Config.getCameraHeight());
                                scaleH = detectFaceCallback.getMeasuredHeight() * 1.0f / (p ? Config.getCameraHeight() : Config.getCameraWidth());
                            }
                            for (int i = 0; i < faceInfos.length; i++) {
                                rectfs[i] = new RectF();
                                float halfWidth = faceInfos[i].width * scaleW / 2;
                                float halfHeight = faceInfos[i].height * scaleH / 2;
                                float centerX = faceInfos[i].centerX * scaleW;
                                float centerY = faceInfos[i].centerY * scaleH;
                                rectfs[i].left = centerX - halfWidth;
                                rectfs[i].right = centerX + halfWidth;
                                rectfs[i].top = centerY - halfHeight;
                                rectfs[i].bottom = centerY + halfHeight;
                                //noinspection deprecation
                                if (Config.isMirror() ^ Config.getRgbCameraId() == Camera.getNumberOfCameras() - 1) {
                                    rectfs[i].left = detectFaceCallback.getMeasuredWidth() - rectfs[i].left;
                                    rectfs[i].right = detectFaceCallback.getMeasuredWidth() - rectfs[i].right;
                                }
                            }
                            detectFaceCallback.onDetectFace(rectfs);
                        }
                        if (FaceSDK.Config.getFaceDetectNum() < 2 && faceInfos[0].width < FaceSDK.Config.getFaceMinThreshold()) {
                            rgbInstance.destory();
                            if (recognizeCallback != null) {
                                recognizeCallback.recognizeFailed("请再靠近一些");
                            }
                            MyLog.d(TAG, "recognize false! face too small");
                        } else {
                            if (detectBeanList.size() > 1) {
                                final DetectBean detectBean = detectBeanList.take();
                                if (detectBean != null && detectBean.rgbImage != null) {
                                    detectBean.rgbImage.destory();
                                }
                            }
                            DetectBean detectBean = new DetectBean();
                            detectBean.rgbImage = rgbInstance;
                            detectBean.fastFaceInfos = faceInfos;
                            detectBean.irData = cameraFrame.irData;
                            detectBeanList.offer(detectBean);
                        }
                    } else {
                        rgbInstance.destory();
                        if (detectFaceCallback != null) {
                            detectFaceCallback.onDetectFace(null);
                        }
                    }
                } catch (Exception e) {
                    MyLog.d(TAG, "track face exception occurred! " + e.toString());
                }
            }
        });
        trackThread.start();
    }

    private void initDatabases(Context context) {
        db = new FeatureBd(context);
//        for (String s : groupList) {
//            final FaceFeature faceFeature = faceFeatureMap.get(s);
//            if (faceFeature != null) {
//                faceFeature.featurePush(db.queryAll(s));
//            }
//        }
        faceFeature.featurePush(db.queryAll());
    }

    private void initModel(Context context) {
        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_TRACK_MODEL,
                BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init track model success!");
                    } else {
                        MyLog.d(TAG, "init track model failed! " + response);
                    }
                });

        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_RGB_MODEL,
                BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init rgb model success!");
                    } else {
                        MyLog.d(TAG, "init rgb model failed! " + response);
                    }
                });

        if (!Config.isSingleCamera()) {
            faceDetectNir.initModel(context,
                    GlobalSet.DETECT_NIR_MODE,
                    GlobalSet.ALIGN_NIR_MODEL,
                    BDFaceSDKCommon.DetectType.DETECT_NIR,
                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_NIR_ACCURATE,
                    (code, response) -> {
                        if (code == 0) {
                            MyLog.d(TAG, "init nir model success!");
                        } else {
                            MyLog.d(TAG, "init nir model failed! " + response);
                        }
                    });
        }

        faceDetect.initQuality(context,
                GlobalSet.BLUR_MODEL,
                GlobalSet.OCCLUSION_MODEL, (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init Quality model success!");
                    } else {
                        MyLog.d(TAG, "init Quality model failed! " + response);
                    }
                });

        faceDetect.initAttrEmo(context, GlobalSet.ATTRIBUTE_MODEL, GlobalSet.EMOTION_MODEL, (code, response) -> {
            if (code == 0) {
                MyLog.d(TAG, "init AttrEmo model success!");
            } else {
                MyLog.d(TAG, "init AttrEmo model failed! " + response);
            }
        });

        faceDetect.initBestImage(context, GlobalSet.BEST_IMAGE, (code, response) -> {
            if (code == 0) {
                MyLog.d(TAG, "init Best face model success!");
            } else {
                MyLog.d(TAG, "init Best face model failed! " + response);
            }
        });

        // 初始化暗光恢复
        faceDarkEnhance.initFaceDarkEnhance(context,
                GlobalSet.DARK_ENHANCE_MODEL, (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init model success!");
                    } else {
                        MyLog.d(TAG, "init model failed! " + response);
                    }
                });

        faceLiveness.initModel(context,
                GlobalSet.LIVE_VIS_MODEL,
                GlobalSet.LIVE_NIR_MODEL,
                GlobalSet.LIVE_DEPTH_MODEL,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init Liveness model success!");
                    } else {
                        MyLog.d(TAG, "init Liveness model failed! " + response);
                    }
                });

        // 初始化特征提取模型
//        for (String s : groupList) {
//            final FaceFeature faceFeature = faceFeatureMap.get(s);
//            if (faceFeature != null) {
//                faceFeature.initModel(context,
//                        GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
//                        GlobalSet.RECOGNIZE_VIS_MODEL,
//                        GlobalSet.RECOGNIZE_NIR_MODEL,
//                        GlobalSet.RECOGNIZE_RGBD_MODEL,
//                        (code, response) -> {
//                            if (code == 0) {
//                                MyLog.d(TAG, "init Feature model success! groupName=" + s);
//                            } else {
//                                MyLog.d(TAG, "init Feature model failed! " + response);
//                            }
//                        });
//            }
//        }
        faceFeature.initModel(context,
                GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
                GlobalSet.RECOGNIZE_VIS_MODEL,
                GlobalSet.RECOGNIZE_NIR_MODEL,
                GlobalSet.RECOGNIZE_RGBD_MODEL,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init Feature model success!");
                    } else {
                        MyLog.d(TAG, "init Feature model failed! ");
                    }
                });
    }

    private void initConfig() {
        if (faceDetect != null) {
            SingleBaseConfig.getBaseConfig().setRgbDetectDirection(Config.getRecognizeOrientation());
            SingleBaseConfig.getBaseConfig().setNirDetectDirection(Config.getRecognizeOrientation());
            SingleBaseConfig.getBaseConfig().setMirrorNIR(Config.isMirror() ? 1 : 0);

            BDFaceSDKConfig config = new BDFaceSDKConfig();
            //最小人脸个数检查，默认设置为1,根据需求调整
            config.maxDetectNum = FaceSDK.Config.getFaceDetectNum();

            //默认为80px。可传入大于30px的数值，小于此大小的人脸不予检测，生效时间第一次加载模型
            config.minFaceSize = SingleBaseConfig.getBaseConfig().getMinimumFace();

            //默认为0.5。可传入大于0.3的数值
            config.notRGBFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
            config.notNIRFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();

            // 是否进行属性检测，默认关闭
            config.isAttribute = SingleBaseConfig.getBaseConfig().isAttribute();
//
//            //模糊，遮挡，光照三个质量检测和姿态角查默认关闭，如果要开启，设置页启动
            config.isCheckBlur = config.isOcclusion
                    = config.isIllumination = config.isHeadPose
                    = SingleBaseConfig.getBaseConfig().isQualityControl();

            faceDetect.loadConfig(config);
        }
    }

    @Override
    public void addFace(final String photoPath, final AddFaceCallback callback) {
        if (!init) {
            if (callback != null) {
                callback.addResult(false, "SDK未初始化");
            }
            return;
        }
        addFaceMap.put(photoPath, callback);
        if (cameraFrameList.isEmpty()) {
            trackThread.interrupt();
        }
//        BDFaceInstance bdFaceInstance = new BDFaceInstance();
//        bdFaceInstance.creatInstance();
//        FaceDetect faceDetect = new FaceDetect(bdFaceInstance);
//        BDFaceSDKConfig config = new BDFaceSDKConfig();
//        config.maxDetectNum = 1;
//        config.minFaceSize = SingleBaseConfig.getBaseConfig().getMinimumFace();
//        config.notRGBFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
//        config.notNIRFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
//        config.isAttribute = SingleBaseConfig.getBaseConfig().isAttribute();
//        config.isCheckBlur = config.isOcclusion
//                = config.isIllumination = config.isHeadPose
//                = SingleBaseConfig.getBaseConfig().isQualityControl();
//        faceDetect.loadConfig(config);
//        final Map<String, Boolean> initModelResult = new HashMap<>();
//        initModelResult.put("track", false);
//        initModelResult.put("rgb", false);
//        initModelResult.put("Quality", false);
//        initModelResult.put("AttrEmo", false);
//        initModelResult.put("Best", false);
//        faceDetect.initModel(context,
//                GlobalSet.DETECT_VIS_MODEL,
//                GlobalSet.ALIGN_TRACK_MODEL,
//                BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
//                (code, response) -> {
//                    if (code == 0) {
//                        initModelResult.put("track", true);
//                        MyLog.d(TAG, "init track model success!");
//                        trackFace(context, photoPath, faceDetect, initModelResult, callback);
//                    } else {
//                        MyLog.d(TAG, "init track model failed! " + response);
//                    }
//                });
//        faceDetect.initModel(context,
//                GlobalSet.DETECT_VIS_MODEL,
//                GlobalSet.ALIGN_RGB_MODEL,
//                BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
//                (code, response) -> {
//                    if (code == 0) {
//                        initModelResult.put("rgb", true);
//                        MyLog.d(TAG, "init rgb model success!");
//                        trackFace(context, photoPath, faceDetect, initModelResult, callback);
//                    } else {
//                        MyLog.d(TAG, "init rgb model failed! " + response);
//                    }
//                });
//        faceDetect.initQuality(context,
//                GlobalSet.BLUR_MODEL,
//                GlobalSet.OCCLUSION_MODEL, (code, response) -> {
//                    if (code == 0) {
//                        initModelResult.put("Quality", true);
//                        MyLog.d(TAG, "init Quality model success!");
//                        trackFace(context, photoPath, faceDetect, initModelResult, callback);
//                    } else {
//                        MyLog.d(TAG, "init Quality model failed! " + response);
//                    }
//                });
//        faceDetect.initAttrEmo(context, GlobalSet.ATTRIBUTE_MODEL, GlobalSet.EMOTION_MODEL, (code, response) -> {
//            if (code == 0) {
//                initModelResult.put("AttrEmo", true);
//                MyLog.d(TAG, "init AttrEmo model success!");
//                trackFace(context, photoPath, faceDetect, initModelResult, callback);
//            } else {
//                MyLog.d(TAG, "init AttrEmo model failed! " + response);
//            }
//        });
//        faceDetect.initBestImage(context, GlobalSet.BEST_IMAGE, (code, response) -> {
//            if (code == 0) {
//                initModelResult.put("Best", true);
//                MyLog.d(TAG, "init Best face model success!");
//                trackFace(context, photoPath, faceDetect, initModelResult, callback);
//            } else {
//                MyLog.d(TAG, "init Best face model failed! " + response);
//            }
//        });
    }

//    private void trackFace(Context context,
//                           String photoPath,
//                           FaceDetect faceDetect,
//                           Map<String, Boolean> initModelResult,
//                           AddFaceCallback callback) {
//        Boolean result = initModelResult.get("track");
//        if (result == null || !result) return;
//        result = initModelResult.get("rgb");
//        if (result == null || !result) return;
//        result = initModelResult.get("Quality");
//        if (result == null || !result) return;
//        result = initModelResult.get("AttrEmo");
//        if (result == null || !result) return;
//        result = initModelResult.get("Best");
//        if (result == null || !result) return;
//        if (TextUtils.isEmpty(photoPath)) {
//            if (callback != null) {
//                callback.addResult(false, "图片路径错误");
//            }
//            MyLog.d(TAG, "add face failed! the photo path is null");
//            return;
//        }
//        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
//        if (bitmap == null) {
//            if (callback != null) {
//                callback.addResult(false, "图片加载失败");
//            }
//            MyLog.d(TAG, "add face failed! bitmap decodeFile error");
//            return;
//        }
//        final BDFaceImageInstance image = new BDFaceImageInstance(bitmap);
//        final FaceInfo[] fastFaceInfos = faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, image);
//        if (fastFaceInfos != null && fastFaceInfos.length > 0) {
//            DetectBean bean = new DetectBean();
//            bean.rgbImage = image;
//            bean.fastFaceInfos = fastFaceInfos;
//            if (bean.fastFaceInfos.length == 0) {
//                MyLog.d(TAG, "照片没有人脸数据");
//                faceDetect.uninitModel();
//                if (callback != null) {
//                    callback.addResult(false, "照片没有人脸数据");
//                }
//                return;
//            }
//            BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
//            bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
//                    = SingleBaseConfig.getBaseConfig().isQualityControl();
//            bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
//            final FaceInfo[] faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
//                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
//                    bean.rgbImage, bean.fastFaceInfos, bdFaceDetectListConfig);
//            float rgbScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
//                    bean.rgbImage, faceInfos[0].landmarks);
//            if (rgbScore < SingleBaseConfig.getBaseConfig().getRgbLiveScore()) {
//                MyLog.d(TAG, "活体检测失败: rgb score=" + rgbScore);
//                bean.rgbImage.destory();
//                faceDetect.uninitModel();
//                if (callback != null) {
//                    callback.addResult(false, "活体检测失败");
//                }
//                return;
//            }
//            final byte[] feature = new byte[512];
//            final FaceFeature faceFeature = new FaceFeature();
//            faceFeature.initModel(context,
//                    GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
//                    GlobalSet.RECOGNIZE_VIS_MODEL,
//                    GlobalSet.RECOGNIZE_NIR_MODEL,
//                    GlobalSet.RECOGNIZE_RGBD_MODEL,
//                    (code, response) -> {
//                        if (code == 0) {
//                            MyLog.d(TAG, "init Feature model success!");
//                            float featureSize = faceFeature.feature(
//                                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
//                                    bean.rgbImage, faceInfos[0].landmarks, feature);
//                            if ((int) featureSize == FEATURE_SIZE / 4) {
//
//                                ArrayList<Feature> featureResult = faceFeature.featureSearch(feature,
//                                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
//                                if (featureResult != null && featureResult.size() > 0) {
//                                    Feature topFeature = featureResult.get(0);
//                                    if (topFeature != null && topFeature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
//                                        Feature query = db.query(topFeature.getId(), getCurrentGroup());
//                                        if (query != null) {
//                                            faceDetect.uninitModel();
//                                            bean.rgbImage.destory();
//                                            faceFeature.uninitModel();
//                                            MyLog.d(TAG, "人脸添加失败，照片已存在");
//                                            if (callback != null) {
//                                                callback.addResult(false, "人脸添加失败，照片已存在");
//                                            }
//                                            return;
//                                        }
//                                    }
//                                }
//
//                                int id = db.insert(feature, getCurrentGroup());
//                                faceFeature.featurePush(db.queryAll());
//                                MyLog.d(TAG, "人脸添加成功！");
//                                if (callback != null) {
//                                    callback.addResult(true, String.valueOf(id));
//                                }
//                            } else {
//                                MyLog.d(TAG, "featureSize error!");
//                                if (callback != null) {
//                                    callback.addResult(false, "featureSize error!");
//                                }
//                            }
//                        } else {
//                            MyLog.d(TAG, "init Feature model failed! " + response);
//                            if (callback != null) {
//                                callback.addResult(false, "加载模型失败");
//                            }
//                        }
//                        faceDetect.uninitModel();
//                        bean.rgbImage.destory();
//                        faceFeature.uninitModel();
//                    });
//        } else {
//            MyLog.d(TAG, "照片没有检测到人脸");
//            if (callback != null) {
//                callback.addResult(false, "照片没有检测到人脸");
//            }
//            faceDetect.uninitModel();
//            image.destory();
//        }
//    }

    private void _addFace(String photoPath, AddFaceCallback callback) {
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        if (bitmap == null) {
            if (callback != null) {
                callback.addResult(false, "图片加载失败");
            }
            MyLog.d(TAG, "add face failed! bitmap decodeFile error");
            return;
        }
        final BDFaceImageInstance image = new BDFaceImageInstance(bitmap);
        final FaceInfo[] fastFaceInfos = faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, image);
        if (fastFaceInfos != null && fastFaceInfos.length > 0) {
            DetectBean bean = new DetectBean();
            bean.rgbImage = image;
            bean.fastFaceInfos = fastFaceInfos;
            if (bean.fastFaceInfos.length == 0) {
                MyLog.d(TAG, "照片没有人脸数据");
                if (callback != null) {
                    callback.addResult(false, "照片没有人脸数据");
                }
                return;
            }
            BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
            bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
                    = SingleBaseConfig.getBaseConfig().isQualityControl();
            bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
            final FaceInfo[] faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                    bean.rgbImage, bean.fastFaceInfos, bdFaceDetectListConfig);
            float rgbScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
                    bean.rgbImage, faceInfos[0].landmarks);
            if (rgbScore < SingleBaseConfig.getBaseConfig().getRgbLiveScore()) {
                MyLog.d(TAG, "活体检测失败: rgb score=" + rgbScore);
                image.destory();
                if (callback != null) {
                    callback.addResult(false, "活体检测失败");
                }
                return;
            }

            final byte[] feature = new byte[512];
//            final FaceFeature faceFeature = faceFeatureMap.get(getCurrentGroup());
            if (faceFeature != null) {
                float featureSize = faceFeature.feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                        bean.rgbImage, faceInfos[0].landmarks, feature);
                if ((int) featureSize == GlobalSet.FEATURE_SIZE / 4) {
                    ArrayList<Feature> featureResult = faceFeature.featureSearch(feature,
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
                    if (featureResult != null && featureResult.size() > 0) {
                        Feature topFeature = featureResult.get(0);
                        if (topFeature != null && topFeature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
                            Feature query = db.query(topFeature.getId(), groupName);
                            if (query != null) {
                                image.destory();
                                MyLog.d(TAG, "人脸添加失败，照片已存在");
                                if (callback != null) {
                                    callback.addResult(false, String.valueOf(query.getId()));
                                }
                                return;
                            }
                        }
                    }
                    int id = db.insert(feature, groupName);
                    faceFeature.featurePush(db.queryAll());
                    MyLog.d(TAG, "人脸添加成功！");
                    if (callback != null) {
                        callback.addResult(true, String.valueOf(id));
                    }
                } else {
                    MyLog.d(TAG, "featureSize error!");
                    if (callback != null) {
                        callback.addResult(false, "featureSize error!");
                    }
                }
            } else {
                MyLog.d(TAG, "faceFeature null!");
                if (callback != null) {
                    callback.addResult(false, "faceFeature null!");
                }
            }
        } else {
            MyLog.d(TAG, "照片没有检测到人脸");
            if (callback != null) {
                callback.addResult(false, "照片没有检测到人脸");
            }
        }
        image.destory();
    }

    @Override
    public void deleteFace(String faceToken) {
        if (db != null && !TextUtils.isEmpty(faceToken)) {
            try {
                db.delete(Integer.parseInt(faceToken));
            } catch (NumberFormatException e) {
                MyLog.d(TAG, "delete face failed! id=" + faceToken);
            }
        }
    }

//    public String addFace(Context context, String photoPath) {
//        if (!init) {
//            MyLog.d(TAG, "SDK未初始化");
//            return null;
//        }
//        if (TextUtils.isEmpty(photoPath)) {
//            MyLog.d(TAG, "add face failed! the photo path is null");
//            return null;
//        }
//        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
//        if (bitmap == null) {
//            MyLog.d(TAG, "add face failed! bitmap decodeFile error");
//            return null;
//        }
//        BDFaceImageInstance image = new BDFaceImageInstance(bitmap);
//        BDFaceInstance bdFaceInstance = new BDFaceInstance();
//        bdFaceInstance.creatInstance();
//        FaceDetect faceDetect = new FaceDetect(bdFaceInstance);
//        BDFaceSDKConfig config = new BDFaceSDKConfig();
//        config.maxDetectNum = 1;
//        config.minFaceSize = SingleBaseConfig.getBaseConfig().getMinimumFace();
//        config.notRGBFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
//        config.notNIRFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
//        config.isAttribute = SingleBaseConfig.getBaseConfig().isAttribute();
//        config.isCheckBlur = config.isOcclusion
//                = config.isIllumination = config.isHeadPose
//                = SingleBaseConfig.getBaseConfig().isQualityControl();
//        faceDetect.loadConfig(config);
//        faceDetect.initModel(context,
//                GlobalSet.DETECT_VIS_MODEL,
//                GlobalSet.ALIGN_TRACK_MODEL,
//                BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
//                (code, response) -> {
//                    if (code == 0) {
//                        MyLog.d(TAG, "init track model success!");
//                    } else {
//                        MyLog.d(TAG, "init track model failed! " + response);
//                    }
//                });
//        faceDetect.initModel(context,
//                GlobalSet.DETECT_VIS_MODEL,
//                GlobalSet.ALIGN_RGB_MODEL,
//                BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
//                (code, response) -> {
//                    if (code == 0) {
//                        MyLog.d(TAG, "init rgb model success!");
//                    } else {
//                        MyLog.d(TAG, "init rgb model failed! " + response);
//                    }
//                });
//        faceDetect.initQuality(context,
//                GlobalSet.BLUR_MODEL,
//                GlobalSet.OCCLUSION_MODEL, (code, response) -> {
//                    if (code == 0) {
//                        MyLog.d(TAG, "init Quality model success!");
//                    } else {
//                        MyLog.d(TAG, "init Quality model failed! " + response);
//                    }
//                });
//        faceDetect.initAttrEmo(context, GlobalSet.ATTRIBUTE_MODEL, GlobalSet.EMOTION_MODEL, (code, response) -> {
//            if (code == 0) {
//                MyLog.d(TAG, "init AttrEmo model success!");
//            } else {
//                MyLog.d(TAG, "init AttrEmo model failed! " + response);
//            }
//        });
//        faceDetect.initBestImage(context, GlobalSet.BEST_IMAGE, (code, response) -> {
//            if (code == 0) {
//                MyLog.d(TAG, "init Best face model success!");
//            } else {
//                MyLog.d(TAG, "init Best face model failed! " + response);
//            }
//        });
//        FaceInfo[] faceInfos = faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
//                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, image);
//        Log.d(TAG, "add face: face num: " + (faceInfos == null ? 0 : faceInfos.length));
//        if (faceInfos != null && faceInfos.length > 0) {
//            DetectBean bean = new DetectBean();
//            bean.rgbImage = image;
//            bean.fastFaceInfos = faceInfos;
//            if (bean.fastFaceInfos.length == 0) {
//                MyLog.d(TAG, "照片没有人脸数据");
//                faceDetect.uninitModel();
//                return null;
//            }
//            BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
//            bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
//                    = SingleBaseConfig.getBaseConfig().isQualityControl();
//            bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
//            faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
//                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
//                    bean.rgbImage, bean.fastFaceInfos, bdFaceDetectListConfig);
//            float rgbScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
//                    bean.rgbImage, faceInfos[0].landmarks);
//            if (rgbScore < SingleBaseConfig.getBaseConfig().getRgbLiveScore()) {
//                MyLog.d(TAG, "活体检测失败: rgb score=" + rgbScore);
//                bean.rgbImage.destory();
//                MyLog.d(TAG, "活体检测失败");
//                faceDetect.uninitModel();
//                return null;
//            }
//            byte[] feature = new byte[512];
//            FaceFeature faceFeature = new FaceFeature();
//            faceFeature.initModel(context,
//                    GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
//                    GlobalSet.RECOGNIZE_VIS_MODEL,
//                    GlobalSet.RECOGNIZE_NIR_MODEL,
//                    GlobalSet.RECOGNIZE_RGBD_MODEL,
//                    (code, response) -> {
//                        if (code == 0) {
//                            MyLog.d(TAG, "init Feature model success!");
//                        } else {
//                            MyLog.d(TAG, "init Feature model failed! " + response);
//                        }
//                    });
//            float featureSize = faceFeature.feature(
//                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
//                    bean.rgbImage, faceInfos[0].landmarks, feature);
//            bean.rgbImage.destory();
//            if ((int) featureSize == FEATURE_SIZE / 4) {
//                int id = db.insert(feature, getCurrentGroup());
//                faceFeature.featurePush(db.queryAll());
//                MyLog.d(TAG, "人脸添加成功！");
//                faceDetect.uninitModel();
//                return String.valueOf(id);
//            }
//            faceFeature.uninitModel();
//        } else {
//            MyLog.d(TAG, "照片没有检测到人脸");
//        }
//        faceDetect.uninitModel();
//        return null;
//    }

    @Override
    public void clearFace() {
        if (db != null) {
            db.deleteAll();
        }
    }

    @Override
    public void setCurrentGroup(String group) {
//        this.currentGroup = group;
    }

    @Override
    public void clearFace(String groupName) {
        db.delete(groupName);
    }

//    private String getCurrentGroup() {
//        if (currentGroup == null) {
//            currentGroup = groupList.get(0);
//        }
//        MyLog.d(TAG, "currentGroup: " + currentGroup);
//        return currentGroup;
//    }

    @Override
    public boolean capture(String savePath) {
        if (TextUtils.isEmpty(savePath)) {
            MyLog.d(TAG, "capture save failed! the save path error");
            return false;
        }
        File file = new File(savePath);
        if (!file.exists() || file.isDirectory()) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists() || parentFile.isFile()) {
                    if (!parentFile.mkdirs()) {
                        MyLog.d(TAG, "capture save failed! mkdirs failed");
                        return false;
                    }
                }
            }
            try {
                if (!file.createNewFile()) {
                    MyLog.d(TAG, "capture save failed! createNewFile failed");
                    return false;
                }
            } catch (IOException e) {
                MyLog.d(TAG, "capture save failed! createNewFile IOException");
                return false;
            }
        }

        int waitCount = 0;
        while (cacheFrame == null) {
            waitCount++;
            SystemClock.sleep(10);
            if (waitCount > 10) break;
        }

        if (cacheFrame == null) {
            MyLog.d(TAG, "capture save failed! frame buff is empty");
            return false;
        }
        YuvImage yuvImage = new YuvImage(cacheFrame.clone(), ImageFormat.NV21,
                Config.getCameraWidth(), Config.getCameraHeight(), null);
        try {
            FileOutputStream fosImage = new FileOutputStream(file);
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, fosImage);
            fosImage.close();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Matrix matrix = new Matrix();
            matrix.postRotate(Config.getCaptureOrientation());
            if (Config.isMirror()) {
                matrix.postScale(-1, 1);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            fosImage = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fosImage);
            fosImage.close();
            bitmap.recycle();
            MyLog.d(TAG, "capture save success! path=" + savePath);
            return true;
        } catch (IOException e) {
            MyLog.d(TAG, e.toString());
            return false;
        }
    }

    @Override
    public void auth(final Context context, String cert, AuthCallback callback) {
        if (faceAuth == null) {
            if (callback != null) {
                callback.authResult(false);
            }
            return;
        }
        faceAuth.initLicenseOnLine(context, cert, (code, response) -> {
            if (code == 0) {
                if (callback != null) {
                    callback.authResult(true);
                }
                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString(keyName, cert).apply();
                onAuthSuccess(context);
            } else {
                if (callback != null) {
                    callback.authResult(false);
                }
            }
        });
    }

    @Override
    public void release() {
        running = false;
//        for (String s : groupList) {
//            final FaceFeature faceFeature = faceFeatureMap.get(s);
        if (faceFeature != null) {
            faceFeature.uninitModel();
        }
//        }
        if (faceDetect != null) {
            faceDetect.uninitModel();
        }
        if (faceDetectNir != null) {
            faceDetectNir.uninitModel();
        }
        if (faceLiveness != null) {
            faceLiveness.uninitModel();
        }
        if (trackThread != null) {
            trackThread.interrupt();
        }
        if (featureThread != null) {
            featureThread.interrupt();
        }
        if (addThread != null) {
            addThread.interrupt();
        }
    }

    @Override
    public boolean isActive() {
        return init;
    }

    @Override
    public String compare(Context context, String photoPath) {
        BDFaceImageInstance image = new BDFaceImageInstance(BitmapFactory.decodeFile(photoPath));
        BDFaceInstance bdFaceInstance = new BDFaceInstance();
        bdFaceInstance.creatInstance();
        FaceDetect faceDetect = new FaceDetect(bdFaceInstance);

        BDFaceSDKConfig config = new BDFaceSDKConfig();
        config.maxDetectNum = 1;
        config.minFaceSize = SingleBaseConfig.getBaseConfig().getMinimumFace();
        config.notRGBFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
        config.notNIRFaceThreshold = SingleBaseConfig.getBaseConfig().getFaceThreshold();
        config.isAttribute = SingleBaseConfig.getBaseConfig().isAttribute();
        config.isCheckBlur = config.isOcclusion
                = config.isIllumination = config.isHeadPose
                = SingleBaseConfig.getBaseConfig().isQualityControl();
        faceDetect.loadConfig(config);
        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_TRACK_MODEL,
                BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init track model success!");
                    } else {
                        MyLog.d(TAG, "init track model failed! " + response);
                    }
                });
        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_RGB_MODEL,
                BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init rgb model success!");
                    } else {
                        MyLog.d(TAG, "init rgb model failed! " + response);
                    }
                });
        faceDetect.initQuality(context,
                GlobalSet.BLUR_MODEL,
                GlobalSet.OCCLUSION_MODEL, (code, response) -> {
                    if (code == 0) {
                        MyLog.d(TAG, "init Quality model success!");
                    } else {
                        MyLog.d(TAG, "init Quality model failed! " + response);
                    }
                });
        faceDetect.initAttrEmo(context, GlobalSet.ATTRIBUTE_MODEL, GlobalSet.EMOTION_MODEL, (code, response) -> {
            if (code == 0) {
                MyLog.d(TAG, "init AttrEmo model success!");
            } else {
                MyLog.d(TAG, "init AttrEmo model failed! " + response);
            }
        });
        faceDetect.initBestImage(context, GlobalSet.BEST_IMAGE, (code, response) -> {
            if (code == 0) {
                MyLog.d(TAG, "init Best face model success!");
            } else {
                MyLog.d(TAG, "init Best face model failed! " + response);
            }
        });
        FaceInfo[] faceInfo = faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, image);
        if (faceInfo != null && faceInfo.length > 0) {
            try {
                BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
                bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
                        = SingleBaseConfig.getBaseConfig().isQualityControl();
                bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
                FaceInfo[] faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
                        BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                        image, faceInfo, bdFaceDetectListConfig);

                //提取特征及比对搜索
                byte[] feature = new byte[512];
                FaceFeature faceFeature = new FaceFeature();
                faceFeature.initModel(context,
                        GlobalSet.RECOGNIZE_IDPHOTO_MODEL,
                        GlobalSet.RECOGNIZE_VIS_MODEL,
                        GlobalSet.RECOGNIZE_NIR_MODEL,
                        GlobalSet.RECOGNIZE_RGBD_MODEL,
                        (code, response) -> {
                            if (code == 0) {
                                MyLog.d(TAG, "init Feature model success!");
                            } else {
                                MyLog.d(TAG, "init Feature model failed! " + response);
                            }
                        });
                float featureSize = faceFeature.feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                        image, faceInfos[0].landmarks, feature);
                image.destory();
                faceFeature.uninitModel();
                faceDetect.uninitModel();
                if ((int) featureSize == GlobalSet.FEATURE_SIZE / 4) {
                    ArrayList<Feature> featureResult = faceFeature.featureSearch(feature,
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
                    if (featureResult != null && featureResult.size() > 0) {
                        Feature topFeature = featureResult.get(0);
                        if (topFeature != null && topFeature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
                            Feature query = db.query(topFeature.getId(), groupName);
                            if (query != null) {
                                return String.valueOf(query.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void setRecognizeCallback(RecognizeCallback callback) {
        this.recognizeCallback = callback;
    }

    @Override
    public void setDetectFaceCallback(DetectFaceCallback detectFaceCallback) {
        scaleW = 0;
        this.detectFaceCallback = detectFaceCallback;
    }

    @Override
    public List<String> getAllFaceToken() {
        if (db == null) {
            return new ArrayList<>();
        }
        final List<Feature> list = db.queryAll();
        if (list == null || list.size() == 0) {
            return new ArrayList<>();
        }
        List<String> arrayList = new ArrayList<>();
        for (Feature feature : list) {
            arrayList.add(String.valueOf(feature.getId()));
        }
        return arrayList;
    }

    @Override
    public void onPreviewCallback(byte[] rgbData, byte[] irData) {
//        cameraFrame = new CameraFrame(rgbData, irData);
//        trackFace();
        cacheFrame = rgbData;
        if (cameraFrameList.size() > 1) {
            try {
                cameraFrameList.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cameraFrameList.offer(new CameraFrame(rgbData, irData));
    }

//    /**
//     * 检查是否有so文件丢失
//     */
//    private String checkLostFile() {
//        if (TextUtils.isEmpty(Config.getCrashPath())) return null;
//        File file = new File(Config.getCrashPath());
//        if (!file.exists() || file.isFile()) return null;
//        File[] files = file.listFiles();
//        if (files == null || files.length < 1) return null;
//        long t;
//        try {
//            String[] split = files[files.length - 1].getName().split("\\.")[0].split("-");
//            t = Long.parseLong(split[split.length - 1]);
//        } catch (Exception e) {
//            return null;
//        }
//        if ((System.currentTimeMillis() - t) < 5000) {
//            try (BufferedReader reader = new BufferedReader(new FileReader(files[files.length - 1]))) {
//                String s = reader.readLine();
//                while (s != null) {
//                    if (s.contains("UnsatisfiedLinkError")) {
//                        int index1 = s.indexOf("\"");
//                        int index2 = s.lastIndexOf("\"");
//                        return s.substring(index1 + 1, index2);
//                    }
//                    s = reader.readLine();
//                }
//            } catch (Exception e) {
//                return null;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 替换文件
//     *
//     * @param context 上下文环境
//     * @param toFile  被替换的文件
//     */
//    private boolean replaceFile(Context context, File toFile) {
//        final String name = toFile.getName();
//        Log.d(TAG, "replaceFile: file name=" + name);
//        File temp = new File(context.getExternalCacheDir(), name);
//        try (InputStream inputStream = context.getAssets().open(name);
//             OutputStream outputStream = new FileOutputStream(temp)) {
//            byte[] buff = new byte[10240];
//            int len = inputStream.read(buff);
//            while (len > 0) {
//                outputStream.write(buff, 0, len);
//                outputStream.flush();
//                len = inputStream.read(buff);
//            }
//        } catch (Exception e) {
//            Log.d(TAG, "copy file to temp Exception: " + e.toString());
//            return false;
//        }
//        Log.d(TAG, "copy file to temp success!");
//
//        Process process = null;
//        DataOutputStream os = null;
//        try {
//            process = Runtime.getRuntime().exec("su");
//            os = new DataOutputStream(process.getOutputStream());
//            final String cmd = "cp " + temp.toString() + toFile.toString() + "\n";
//            Log.d(TAG, "copy file cmd: " + cmd);
//            os.writeBytes(cmd);
//            os.writeBytes("exit\n");
//            os.flush();
//        } catch (Exception e) {
//            return false;
//        } finally {
//            try {
//                if (os != null) {
//                    os.close();
//                }
//                if (process != null) {
//                    process.destroy();
//                }
//            } catch (Exception ignore) {
//            }
//        }
//        Log.d(TAG, "copy temp file to target success!");
//        //noinspection ResultOfMethodCallIgnored
//        temp.delete();
//        return true;
//    }
}
