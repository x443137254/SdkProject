package com.zxyw.sdk.videoTalk.comm;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.starrtc.starrtcsdk.api.XHClient;
import com.starrtc.starrtcsdk.api.XHConstants;
import com.starrtc.starrtcsdk.api.XHCustomConfig;
import com.starrtc.starrtcsdk.apiInterface.IXHErrorCallback;
import com.starrtc.starrtcsdk.apiInterface.IXHResultCallback;
import com.starrtc.starrtcsdk.core.videosrc.XHVideoSourceManager;
import com.zxyw.sdk.videoTalk.VideoTalkConfig;
import com.zxyw.sdk.videoTalk.listener.XHLoginManagerListener;
import com.zxyw.sdk.videoTalk.listener.XHVoipManagerListener;
import com.zxyw.sdk.videoTalk.utils.AEvent;
import com.zxyw.sdk.videoTalk.utils.IEventListener;
import com.zxyw.sdk.videoTalk.voip.VoipRingingActivity;

/**
 * Created by zhangjt on 2017/8/6.
 */

public class KeepLiveService extends Service implements IEventListener {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initSDK();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initSDK() {
        AEvent.setHandler(new Handler(getMainLooper()));
        MLOC.init(this);
        initFree();
    }

    private boolean isLogin = false;

    //开放版SDK初始化
    private void initFree() {
        isLogin = XHClient.getInstance().getIsOnline();
        if (!isLogin) {
//            if(MLOC.userId.equals("")){
//                MLOC.userId = ""+(new Random().nextInt(900000)+100000);
//                MLOC.saveUserId(MLOC.userId);
//            }
            addListener();
            XHCustomConfig customConfig = XHCustomConfig.getInstance(this);
            customConfig.setChatroomServerUrl(MLOC.getChatroomServerUrl());
            customConfig.setLiveSrcServerUrl(MLOC.getLiveSrcServerUrl());
            customConfig.setLiveVdnServerUrl(MLOC.getLiveVdnServerUrl());
            customConfig.setImServerUrl(MLOC.getImServerUrl());
            customConfig.setVoipServerUrl(MLOC.getVoipServerUrl());
            customConfig.setDefConfigCameraMirror(VideoTalkConfig.getCameraMirror());
            customConfig.setLogEnable(true); //关闭SDK调试日志
            customConfig.setDefConfigBigVideoConfig(VideoTalkConfig.getFPS(), VideoTalkConfig.getBitrate());
//            customConfig.setDefConfigSmallVideoConfig(3,50);
//            customConfig.setDefConfigOpenGLESEnable(false);

            customConfig.setDefConfigCameraId(VideoTalkConfig.getCameraId());//设置默认摄像头方向  0后置  1前置
            customConfig.setDefConfigVideoSize(XHConstants.XHCropTypeEnum.STAR_VIDEO_CONFIG_640BW_640BH_SMALL_NONE);
//            customConfig.setDefConfigHardwareEnable(true);
//            customConfig.setDefConfigOpenGLESEnable(true);
//            customConfig.setLogDirPath(Environment.getExternalStorageDirectory().getPath()+"/starrtcLog");
//            customConfig.setDefConfigCamera2Enable(false);
//            StarCamera.setFrameBufferEnable(false);
            customConfig.initSDKForFree(MLOC.userId, new IXHErrorCallback() {
                @Override
                public void error(final String errMsg, Object data) {
                }
            }, new Handler(getMainLooper()));

            XHClient.getInstance().getVoipManager().addListener(new XHVoipManagerListener());
            XHClient.getInstance().getLoginManager().addListener(new XHLoginManagerListener());
            XHVideoSourceManager.getInstance().setVideoSourceCallback(new VideoSourceCallback());
            XHClient.getInstance().getLoginManager().loginFree(new IXHResultCallback() {
                @Override
                public void success(Object data) {
                    isLogin = true;
                }

                @Override
                public void failed(final String errMsg) {
                }
            });
        }
    }

    @Override
    public void dispatchEvent(String aEventID, boolean success, Object eventObj) {
        switch (aEventID) {
            case AEvent.AEVENT_VOIP_REV_CALLING: {
                Intent intent = new Intent(this, VoipRingingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("targetId", eventObj.toString());
                startActivity(intent);
            }
            break;
            case AEvent.AEVENT_C2C_REV_MSG:
            case AEvent.AEVENT_REV_SYSTEM_MSG:
                MLOC.hasNewC2CMsg = true;
                break;
            case AEvent.AEVENT_GROUP_REV_MSG:
                MLOC.hasNewGroupMsg = true;
                break;
            case AEvent.AEVENT_LOGOUT:
                removeListener();
                this.stopSelf();
                break;
            case AEvent.AEVENT_USER_KICKED:
            case AEvent.AEVENT_CONN_DEATH:
                XHClient.getInstance().getLoginManager().loginFree(new IXHResultCallback() {
                    @Override
                    public void success(Object data) {
                        isLogin = true;
                    }

                    @Override
                    public void failed(final String errMsg) {
                    }
                });
                break;
        }
    }

    private void addListener() {
        AEvent.addListener(AEvent.AEVENT_LOGOUT, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_CALLING, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_CALLING_AUDIO, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_P2P_REV_CALLING, this);
        AEvent.addListener(AEvent.AEVENT_C2C_REV_MSG, this);
        AEvent.addListener(AEvent.AEVENT_REV_SYSTEM_MSG, this);
        AEvent.addListener(AEvent.AEVENT_GROUP_REV_MSG, this);
        AEvent.addListener(AEvent.AEVENT_USER_KICKED, this);
        AEvent.addListener(AEvent.AEVENT_CONN_DEATH, this);
    }

    private void removeListener() {
        AEvent.removeListener(AEvent.AEVENT_LOGOUT, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_CALLING, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_CALLING_AUDIO, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_P2P_REV_CALLING, this);
        AEvent.removeListener(AEvent.AEVENT_C2C_REV_MSG, this);
        AEvent.removeListener(AEvent.AEVENT_REV_SYSTEM_MSG, this);
        AEvent.removeListener(AEvent.AEVENT_GROUP_REV_MSG, this);
        AEvent.removeListener(AEvent.AEVENT_USER_KICKED, this);
        AEvent.removeListener(AEvent.AEVENT_CONN_DEATH, this);
    }

}
