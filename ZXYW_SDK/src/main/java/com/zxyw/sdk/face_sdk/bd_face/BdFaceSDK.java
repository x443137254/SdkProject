package com.zxyw.sdk.face_sdk.bd_face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

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
import com.zxyw.sdk.tools.MyLog;
import com.zxyw.sdk.tools.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.zxyw.sdk.face_sdk.bd_face.model.GlobalSet.FEATURE_SIZE;

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

    private FeatureBd db;
    private boolean init;

    private ExecutorService trackThread;
    private ExecutorService featureThread;
    private volatile boolean trackThreadBusy;
    private volatile boolean featureThreadBusy;
    private CameraFrame cameraFrame;

    private float scaleW;
    private float scaleH;
    private DetectFaceCallback detectFaceCallback;
    private RecognizeCallback recognizeCallback;
    private AddFaceCallback addFaceCallback;

    private List<String> groupList;
    private String currentGroup;

    @Override
    public void init(final Context context, final List<String> groupList, final String url) {
        this.groupList = groupList;
        String s = checkLostFile();
        Log.d(TAG, "missing file: " + s);
        if (s != null && !s.equals("")) {
            if (!replaceFile(context, new File(s))) return;
        }
        faceAuth = new FaceAuth();
        faceAuth.setActiveLog(BDFaceSDKCommon.BDFaceLogInfo.BDFACE_LOG_ERROR_MESSAGE, 0);
        faceAuth.setCoreConfigure(BDFaceSDKCommon.BDFaceCoreRunMode.BDFACE_LITE_POWER_NO_BIND, 2);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getCertificate(context, url, null);
            }
        }, 2000);
    }

    @Override
    public void getCertificate(Context context, String url, AuthCallback callback) {
        String cert = context.getSharedPreferences(spName, Context.MODE_PRIVATE).getString(keyName, null);
        if (cert == null) {
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
                    e.printStackTrace();
                    if (callback != null) callback.authResult(false);
                    return;
                }
                HttpUtil.post(url, AESUtil.encode(jsonObject.toString()), new HttpUtil.HttpCallback() {
                    @Override
                    public void onFailed(String error) {
                        if (callback != null) callback.authResult(false);
                    }

                    @Override
                    public void onSuccess(String bodyString) {
                        JSONObject json;
                        try {
                            json = new JSONObject(bodyString);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            if (callback != null) callback.authResult(false);
                            return;
                        }
                        if (json.optInt("status") == 200) {
                            final String s;
                            try {
                                s = json.optString("result");
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (callback != null) callback.authResult(false);
                                return;
                            }
                            if (!TextUtils.isEmpty(s)) {
                                authOnline(s, context, callback);
                            }
                        }
                    }
                });
            }
        } else {
            authOnline(cert, context, callback);
        }
    }

    private void authOnline(String cert, Context context, AuthCallback callback) {
        faceAuth.initLicenseOnLine(context, AESUtil.decode(cert), (code, response) -> {
            if (code == 0) {
                context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit().putString(keyName, cert).apply();
                if (callback != null) callback.authResult(true);
            } else {
                if (callback != null) callback.authResult(false);
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
        if (Config.getCameraNum() > 1) {
            BDFaceInstance IrBdFaceInstance = new BDFaceInstance();
            IrBdFaceInstance.creatInstance();
            faceDetectNir = new FaceDetect(IrBdFaceInstance);
        }
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
        trackThread = Executors.newSingleThreadExecutor();
        featureThread = Executors.newSingleThreadExecutor();
        init = true;
    }

    /**
     * 活体检测、人脸特征检测、人脸比对
     *
     * @param detectBean 经过快速人脸检测后的数据包，刷人脸时为null，注册人脸时为照片数据
     */
    private void featureFace(final DetectBean detectBean, final boolean regFace) {
        if (!init) return;
        if (featureThreadBusy) {
            if (!regFace) {
                if (detectBean != null && detectBean.rgbImage != null) {
                    detectBean.rgbImage.destory();
                }
                return;
            }
        }
        featureThread.execute(() -> {
            featureThreadBusy = true;
//            boolean regFace = detectBean != null;
//            DetectBean beanCopy = regFace ? detectBean : this.detectBean;
            if (detectBean == null
                    || detectBean.rgbImage == null
                    || detectBean.fastFaceInfos == null
                    || detectBean.fastFaceInfos.length == 0) {
                if (regFace) {
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "没有照片或者照片没有人脸数据");
                        addFaceCallback = null;
                    }
                    MyLog.d(TAG, "add face failed! no photo or no faces");
                }
//                else if (recognizeCallback != null) {
//                    recognizeCallback.recognizeResult(false, "检测失败");
//                }
                if (detectBean != null && detectBean.rgbImage != null) {
                    detectBean.rgbImage.destory();
                }
                featureThreadBusy = false;
                return;
            }
            try {
                BDFaceDetectListConf bdFaceDetectListConfig = new BDFaceDetectListConf();
                bdFaceDetectListConfig.usingQuality = bdFaceDetectListConfig.usingHeadPose
                        = SingleBaseConfig.getBaseConfig().isQualityControl();
                bdFaceDetectListConfig.usingBestImage = SingleBaseConfig.getBaseConfig().isBestImage();
                FaceInfo[] faceInfos = faceDetect.detect(BDFaceSDKCommon.DetectType.DETECT_VIS,
                        BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                        detectBean.rgbImage, detectBean.fastFaceInfos, bdFaceDetectListConfig);
                //rgb活体分数
                float rgbScore = faceLiveness.silentLive(BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
                        detectBean.rgbImage, faceInfos[0].landmarks);
                if (rgbScore < SingleBaseConfig.getBaseConfig().getRgbLiveScore()) {
                    MyLog.d(TAG, "活体检测失败: rgb score=" + rgbScore);
                    detectBean.rgbImage.destory();
                    if (regFace) {
                        if (addFaceCallback != null) {
                            addFaceCallback.addResult(false, "活体检测失败");
                            addFaceCallback = null;
                        }
                        MyLog.d(TAG, "add face failed! liveness detect failed");
                    }
//                    else if (recognizeCallback != null) {
//                        recognizeCallback.recognizeResult(false, "检测失败");
//                    }
                    featureThreadBusy = false;
                    return;
                }
                if (!regFace && detectBean.irData != null) {
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
                        if (irScore * 100 < FaceSDK.Config.getLivenessThreshold()) {
//                            if (recognizeCallback != null) {
//                                recognizeCallback.recognizeResult(false, "检测失败");
//                            }
                            MyLog.d(TAG, "活体检测失败: ir score=" + irScore);
                            featureThreadBusy = false;
                            return;
                        }
                    } else {
//                        if (recognizeCallback != null) {
//                            recognizeCallback.recognizeResult(false, "检测失败");
//                        }
                        MyLog.d(TAG, "活体检测失败: ir detect is null");
                        irImage.destory();
                        featureThreadBusy = false;
                        return;
                    }
                }

                //活体检查通过，开始提取特征及比对搜索
                byte[] feature = new byte[512];
                float featureSize = faceFeature.feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                        detectBean.rgbImage, faceInfos[0].landmarks, feature);
                detectBean.rgbImage.destory();
                if ((int) featureSize == FEATURE_SIZE / 4) {
                    if (regFace) {//注册人脸
                        int id = db.insert(feature, getCurrentGroup());
                        faceFeature.featurePush(db.queryAll());
                        if (addFaceCallback != null) {
                            addFaceCallback.addResult(true, String.valueOf(id));
                            addFaceCallback = null;
                        }
                        MyLog.d(TAG, "add face success!");
                    } else {
                        ArrayList<Feature> featureResult = faceFeature.featureSearch(feature,
                                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
                        if (featureResult != null && featureResult.size() > 0) {
                            Feature topFeature = featureResult.get(0);
                            if (topFeature != null && topFeature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
                                Feature query = db.query(topFeature.getId(), getCurrentGroup());
                                if (query != null) {
                                    if (recognizeCallback != null) {
                                        recognizeCallback.recognizeResult(true, String.valueOf(query.getId()));
                                    }
                                    MyLog.d(TAG, "recognize success! query id=" + query.getId());
                                } else {
                                    if (recognizeCallback != null) {
                                        recognizeCallback.recognizeResult(false, "人脸未注册");
                                    }
                                    MyLog.d(TAG, "recognize success! query failed");
                                }
                            } else {
                                if (recognizeCallback != null) {
                                    recognizeCallback.recognizeResult(false, "人脸未注册");
                                }
                                MyLog.d(TAG, "recognize success! featureSearch score=" +
                                        (topFeature == null ? 0 : topFeature.getScore()));
                            }
                        } else {
                            if (recognizeCallback != null) {
                                recognizeCallback.recognizeResult(false, "人脸未注册");
                            }
                            MyLog.d(TAG, "recognize success! featureSearch failed");
                        }
                    }
                    featureThreadBusy = false;
                    return;
                }
                if (regFace) {
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "图片提取特征值失败");
                        addFaceCallback = null;
                    }
                    MyLog.d(TAG, "add face failed! face feature size error");
                } else {
//                    if (recognizeCallback != null) {
//                        recognizeCallback.recognizeResult(false, "人脸识别失败");
//                    }
                    MyLog.d(TAG, "recognize failed! feature failed");
                }
                featureThreadBusy = false;
            } catch (Exception e) {
                if (regFace) {
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "图片提取发生错误");
                        addFaceCallback = null;
                    }
                }
                MyLog.d(TAG, "Face detect error! " + e.toString());
                featureThreadBusy = false;
            }
        });
    }

    /**
     * 查找人脸
     *
     * @param imageInstance 注册人脸时传入照片对象，否则传入null使用相机回调帧
     */
    private void trackFace(final BDFaceImageInstance imageInstance) {
        if (!init) return;
        if (trackThreadBusy && imageInstance == null) return;
        trackThread.execute(() -> {
            trackThreadBusy = true;
            boolean regFace = imageInstance != null;
            BDFaceImageInstance rgbInstance = null;
            try {
                if (!regFace) {
                    rgbInstance = new BDFaceImageInstance(cameraFrame.rgbData,
                            Config.getCameraHeight(), Config.getCameraWidth(),
                            BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_YUV_NV21,
                            SingleBaseConfig.getBaseConfig().getRgbDetectDirection(),
                            SingleBaseConfig.getBaseConfig().getMirrorDetectRGB());
                    // 判断暗光恢复
                    if (SingleBaseConfig.getBaseConfig().isDarkEnhance()) {
                        rgbInstance = faceDarkEnhance.faceDarkEnhance(rgbInstance);
                    }
                }
                // 快速检测获取人脸信息
                FaceInfo[] faceInfos = regFace ?
                        faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
                                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, imageInstance) :
                        faceDetect.track(BDFaceSDKCommon.DetectType.DETECT_VIS,
                                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST, rgbInstance);
                Log.d(TAG, "trackFace: face num: " + (faceInfos == null ? 0 : faceInfos.length));
                if (faceInfos != null && faceInfos.length > 0) {
                    if (regFace) {
                        DetectBean bean = new DetectBean();
                        bean.rgbImage = imageInstance;
                        bean.fastFaceInfos = faceInfos;
                        featureFace(bean, true);
                    } else {
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
                                if (Config.isMirror() ^ Config.getPreviewCameraId() == 1) {
                                    rectfs[i].left = detectFaceCallback.getMeasuredWidth() - rectfs[i].left;
                                    rectfs[i].right = detectFaceCallback.getMeasuredWidth() - rectfs[i].right;
                                }
                            }
                            detectFaceCallback.onDetectFace(rectfs);
                        }
                        if (faceInfos[0].width > 120) {
                            DetectBean detectBean = new DetectBean();
                            detectBean.rgbImage = rgbInstance;
                            detectBean.fastFaceInfos = faceInfos;
                            detectBean.irData = cameraFrame.irData;
                            featureFace(detectBean, false);
                        } else {
                            rgbInstance.destory();
                            if (recognizeCallback != null) {
                                recognizeCallback.recognizeResult(false, "请再靠近一些");
                            }
                            MyLog.d(TAG, "recognize false! face too small");
                        }
                    }
                } else {
                    if (regFace) {
                        imageInstance.destory();
                        if (addFaceCallback != null) {
                            addFaceCallback.addResult(false, "没有检测到人脸");
                            addFaceCallback = null;
                        }
                        MyLog.d(TAG, "add face failed! no faces");
                    } else {
                        rgbInstance.destory();
                        if (detectFaceCallback != null) {
                            detectFaceCallback.onDetectFace(null);
                        }
                    }
                }
                trackThreadBusy = false;
            } catch (Exception e) {
                MyLog.d(TAG, e.toString());
                if (regFace) {
                    imageInstance.destory();
                    if (addFaceCallback != null) {
                        addFaceCallback.addResult(false, "检测人脸发生错误");
                        addFaceCallback = null;
                    }
                    MyLog.d(TAG, "add face failed! " + e.toString());
                } else {
                    if (rgbInstance != null) {
                        rgbInstance.destory();
                    }
                    MyLog.d(TAG, "track face exception occurred! " + e.toString());
                }
                trackThreadBusy = false;
            }
        });
    }

    private void initDatabases(Context context) {
        db = new FeatureBd(context);
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

        if (Config.getCameraNum() > 1) {
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
    }

    private void initConfig() {
        if (faceDetect != null) {
            SingleBaseConfig.getBaseConfig().setRgbDetectDirection(Config.getPreviewOrientation());
            SingleBaseConfig.getBaseConfig().setNirDetectDirection(Config.getRecognizeOrientation());
            SingleBaseConfig.getBaseConfig().setMirrorNIR(Config.isMirror() ? 1 : 0);

            BDFaceSDKConfig config = new BDFaceSDKConfig();
            //最小人脸个数检查，默认设置为1,根据需求调整
            config.maxDetectNum = 1;

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
    public synchronized void addFace(final String photoPath, final AddFaceCallback callback) {
        if (!init) {
            if (callback != null) {
                callback.addResult(false, "SDK未初始化");
            }
            return;
        }
        if (TextUtils.isEmpty(photoPath)) {
            if (callback != null) {
                callback.addResult(false, "图片路径错误");
            }
            MyLog.d(TAG, "add face failed! the photo path is null");
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        if (bitmap == null) {
            if (callback != null) {
                callback.addResult(false, "图片加载失败");
            }
            MyLog.d(TAG, "add face failed! bitmap decodeFile error");
            return;
        }
        BDFaceImageInstance image = new BDFaceImageInstance(bitmap);
        addFaceCallback = callback;
        trackFace(image);
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

    @Override
    public void clearFace() {
        if (db != null) {
            db.deleteAll();
        }
    }

    @Override
    public void setCurrentGroup(String group) {
        this.currentGroup = group;
    }

    @Override
    public void clearFace(String groupName) {
        db.delete(groupName);
    }

    private String getCurrentGroup() {
        if (currentGroup == null) {
            if (groupList != null && groupList.size() > 0) {
                currentGroup = groupList.get(0);
            } else {
                currentGroup = "defaultGroup";
            }
        }
        return currentGroup;
    }

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
        if (cameraFrame == null || cameraFrame.rgbData == null) {
            MyLog.d(TAG, "capture save failed! frame buff is empty");
            return false;
        }
        YuvImage yuvImage = new YuvImage(cameraFrame.rgbData.clone(), ImageFormat.NV21,
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
        if (faceFeature != null) {
            faceFeature.uninitModel();
        }
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
            trackThread.shutdown();
        }
        if (featureThread != null) {
            featureThread.shutdown();
        }
    }

    @Override
    public boolean isActive() {
        return init;
    }

    @Override
    public String compare(String photoPath) {
        BDFaceImageInstance image = new BDFaceImageInstance(BitmapFactory.decodeFile(photoPath));
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
                float featureSize = faceFeature.feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                        image, faceInfos[0].landmarks, feature);
                image.destory();

                if ((int) featureSize == FEATURE_SIZE / 4) {
                    ArrayList<Feature> featureResult = faceFeature.featureSearch(feature,
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, 1, true);
                    if (featureResult != null && featureResult.size() > 0) {
                        Feature topFeature = featureResult.get(0);
                        if (topFeature != null && topFeature.getScore() > SingleBaseConfig.getBaseConfig().getLiveThreshold()) {
                            Feature query = db.query(topFeature.getId(), getCurrentGroup());
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
        this.detectFaceCallback = detectFaceCallback;
    }

    @Override
    public void onPreviewCallback(byte[] rgbData, byte[] irData) {
        cameraFrame = new CameraFrame(rgbData, irData);
        trackFace(null);
    }

    /**
     * 检查是否有so文件丢失
     */
    private String checkLostFile() {
        if (TextUtils.isEmpty(Config.getCrashPath())) return null;
        File file = new File(Config.getCrashPath());
        if (!file.exists() || file.isFile()) return null;
        File[] files = file.listFiles();
        if (files == null || files.length < 1) return null;
        long t;
        try {
            String[] split = files[files.length - 1].getName().split("\\.")[0].split("-");
            t = Long.parseLong(split[split.length - 1]);
        } catch (Exception e) {
            return null;
        }
        if ((System.currentTimeMillis() - t) < 5000) {
            try (BufferedReader reader = new BufferedReader(new FileReader(files[files.length - 1]))) {
                String s = reader.readLine();
                while (s != null) {
                    if (s.contains("UnsatisfiedLinkError")) {
                        int index1 = s.indexOf("\"");
                        int index2 = s.lastIndexOf("\"");
                        return s.substring(index1 + 1, index2);
                    }
                    s = reader.readLine();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 替换文件
     *
     * @param context 上下文环境
     * @param toFile  被替换的文件
     */
    private boolean replaceFile(Context context, File toFile) {
        final String name = toFile.getName();
        Log.d(TAG, "replaceFile: file name=" + name);
        File temp = new File(context.getExternalCacheDir(), name);
        try (InputStream inputStream = context.getAssets().open(name);
             OutputStream outputStream = new FileOutputStream(temp)) {
            byte[] buff = new byte[10240];
            int len = inputStream.read(buff);
            while (len > 0) {
                outputStream.write(buff, 0, len);
                outputStream.flush();
                len = inputStream.read(buff);
            }
        } catch (Exception e) {
            Log.d(TAG, "copy file to temp Exception: " + e.toString());
            return false;
        }
        Log.d(TAG, "copy file to temp success!");

        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            final String cmd = "cp " + temp.toString() + toFile.toString() + "\n";
            Log.d(TAG, "copy file cmd: " + cmd);
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception ignore) {
            }
        }
        Log.d(TAG, "copy temp file to target success!");
        //noinspection ResultOfMethodCallIgnored
        temp.delete();
        return true;
    }
}
