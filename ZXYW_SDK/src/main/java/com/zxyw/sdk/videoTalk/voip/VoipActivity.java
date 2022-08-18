package com.zxyw.sdk.videoTalk.voip;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.starrtc.starrtcsdk.api.XHClient;
import com.starrtc.starrtcsdk.api.XHConstants;
import com.starrtc.starrtcsdk.api.XHSDKHelper;
import com.starrtc.starrtcsdk.api.XHVoipManager;
import com.starrtc.starrtcsdk.apiInterface.IXHResultCallback;
import com.starrtc.starrtcsdk.core.audio.StarRTCAudioManager;
import com.starrtc.starrtcsdk.core.player.StarPlayer;
import com.starrtc.starrtcsdk.core.player.StarPlayerScaleType;
import com.starrtc.starrtcsdk.core.pusher.XHCameraRecorder;
import com.starrtc.starrtcsdk.core.pusher.XHScreenRecorder;
import com.zxyw.sdk.R;
import com.zxyw.sdk.videoTalk.Delegate;
import com.zxyw.sdk.videoTalk.OnTalkingListener;
import com.zxyw.sdk.videoTalk.VideoTalkConfig;
import com.zxyw.sdk.videoTalk.VideoTalkHelper;
import com.zxyw.sdk.videoTalk.comm.MLOC;
import com.zxyw.sdk.videoTalk.database.CoreDB;
import com.zxyw.sdk.videoTalk.database.HistoryBean;
import com.zxyw.sdk.videoTalk.utils.AEvent;
import com.zxyw.sdk.videoTalk.utils.IEventListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

public class VoipActivity extends AppCompatActivity implements View.OnClickListener, IEventListener {

    private XHVoipManager voipManager;
    private StarPlayer targetPlayer;
//    private StarPlayer selfPlayer;
    private Chronometer timer;
    public static String ACTION = "ACTION";
    public static String RING = "RING";
    public static String CALLING = "CALLING";
    private String targetId;
    private Boolean isTalking = false;
    private StarRTCAudioManager starRTCAudioManager;
    private XHSDKHelper xhsdkHelper;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (VideoTalkHelper.getInstance().getType() == VideoTalkHelper.DeviceType.MAIN){
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
        starRTCAudioManager = StarRTCAudioManager.create(this.getApplicationContext());
        starRTCAudioManager.start(new StarRTCAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StarRTCAudioManager.AudioDevice selectedAudioDevice, Set availableAudioDevices) {
                MLOC.d("onAudioDeviceChanged ", selectedAudioDevice.name());
            }
        });
        starRTCAudioManager.setDefaultAudioDevice(StarRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        starRTCAudioManager.setSpeakerphoneOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            Window _window = getWindow();
            WindowManager.LayoutParams params = _window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE;
            _window.setAttributes(params);
        }

        setContentView(R.layout.activity_voip);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
        voipManager = XHClient.getInstance().getVoipManager();
        voipManager.setRecorder(new XHCameraRecorder());
        voipManager.setRtcMediaType(XHConstants.XHRtcMediaTypeEnum.STAR_RTC_MEDIA_TYPE_VIDEO_AND_AUDIO);
        addListener();
        targetId = getIntent().getStringExtra("targetId");
        String action = getIntent().getStringExtra(ACTION);
        targetPlayer = findViewById(R.id.voip_surface_target);
        targetPlayer.setScalType(StarPlayerScaleType.DRAW_TYPE_CENTER);
//        selfPlayer = findViewById(R.id.voip_surface_self);
//        selfPlayer.setZOrderMediaOverlay(true);

        timer = findViewById(R.id.timer);
        targetPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTalking) {
                    findViewById(R.id.talking_view).setVisibility(findViewById(R.id.talking_view).getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                }
            }
        });

        findViewById(R.id.calling_hangup).setOnClickListener(this);
        findViewById(R.id.talking_hangup).setOnClickListener(this);

        xhsdkHelper = new XHSDKHelper();
        xhsdkHelper.setDefaultCameraId(VideoTalkConfig.getCameraId());
//        if (VideoTalkHelper.getInstance().getType() == VideoTalkHelper.DeviceType.FRONT){
//            xhsdkHelper.setVideoRotation(90);
//        }
        xhsdkHelper.setVideoRotation(VideoTalkConfig.getRotation());
        if (action != null && action.equals(CALLING)) {
            showCallingView();
            MLOC.d("newVoip", "call");
            xhsdkHelper.startPerview(this, ((StarPlayer) findViewById(R.id.voip_surface_target)));
            voipManager.call(this, targetId, new IXHResultCallback() {
                @Override
                public void success(Object data) {
                    xhsdkHelper.stopPerview();
                    xhsdkHelper = null;
                    MLOC.d("newVoip", "call success! RecSessionId:" + data);
                }

                @Override
                public void failed(String errMsg) {
                    MLOC.d("newVoip", "call failed");
                    stopAndFinish();
                }
            });
        } else {
            MLOC.d("newVoip", "onPickup");
            onPickup();
        }

//        findViewById(R.id.calling_hangup).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                voipManager.switchCamera(0);
//                int i = XHCustomConfig.getInstance(getApplicationContext()).getCameraIdConfig();
//                Log.d("111111","camera_id："+i);
//            }
//        },2000);

    }

    private void setupViews() {
//        voipManager.setupView(selfPlayer, targetPlayer, new IXHResultCallback() {
        voipManager.setupView(null, targetPlayer, new IXHResultCallback() {
            @Override
            public void success(Object data) {
                MLOC.d("newVoip", "setupView success");
            }

            @Override
            public void failed(String errMsg) {
                MLOC.d("newVoip", "setupView failed");
                stopAndFinish();
            }
        });
    }

    public void addListener() {
        AEvent.addListener(AEvent.AEVENT_VOIP_INIT_COMPLETE, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_BUSY, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_REFUSED, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_HANGUP, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_CONNECT, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_ERROR, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_TRANS_STATE_CHANGED, this);
    }

    public void removeListener() {
        MLOC.canPickupVoip = true;
        AEvent.removeListener(AEvent.AEVENT_VOIP_INIT_COMPLETE, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_BUSY, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_REFUSED, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_HANGUP, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_CONNECT, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_ERROR, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_TRANS_STATE_CHANGED, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        MLOC.canPickupVoip = false;
        HistoryBean historyBean = new HistoryBean();
        historyBean.setType(CoreDB.HISTORY_TYPE_VOIP);
        historyBean.setLastTime(new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date()));
        historyBean.setConversationId(targetId);
        historyBean.setNewMsgCount(1);
        MLOC.addHistory(historyBean, true);
        Delegate delegate = VideoTalkHelper.getInstance().getDelegate();
        if (delegate != null) delegate.openLed();
    }

    @Override
    public void onPause() {
        super.onPause();
        Delegate delegate = VideoTalkHelper.getInstance().getDelegate();
        if (delegate != null) delegate.closeLed();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        addListener();
    }

    @Override
    public void onDestroy() {
        removeListener();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(VoipActivity.this).setCancelable(true)
                .setTitle("是否挂断?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        hangup();
                    }
                }
        ).show();
    }

    private void hangup() {
        isTalking = false;
        timer.stop();
        voipManager.hangup(new IXHResultCallback() {
            @Override
            public void success(Object data) {
                removeListener();
                stopAndFinish();
            }

            @Override
            public void failed(final String errMsg) {
                stopAndFinish();
            }
        });
    }

    @Override
    public void dispatchEvent(String aEventID, boolean success, final Object eventObj) {
        switch (aEventID) {
            case AEvent.AEVENT_VOIP_REV_BUSY:
                MLOC.d("", "对方线路忙");
                MLOC.showMsg(VoipActivity.this, "对方线路忙");
                if (xhsdkHelper != null) {
                    xhsdkHelper.stopPerview();
                    xhsdkHelper = null;
                }
                stopAndFinish();
                break;
            case AEvent.AEVENT_VOIP_REV_REFUSED:
                MLOC.d("", "对方拒绝通话");
                MLOC.showMsg(VoipActivity.this, "对方拒绝通话");
                if (xhsdkHelper != null) {
                    xhsdkHelper.stopPerview();
                    xhsdkHelper = null;
                }
                stopAndFinish();
                break;
            case AEvent.AEVENT_VOIP_REV_HANGUP:
                MLOC.d("", "对方已挂断");
                MLOC.showMsg(VoipActivity.this, "对方已挂断");
                timer.stop();
                stopAndFinish();
                break;
            case AEvent.AEVENT_VOIP_REV_CONNECT:
                MLOC.d("", "对方允许通话");
                showTalkingView();
                break;
            case AEvent.AEVENT_VOIP_REV_ERROR:
                MLOC.d("", (String) eventObj);
                if (xhsdkHelper != null) {
                    xhsdkHelper.stopPerview();
                    xhsdkHelper = null;
                }
                stopAndFinish();
                break;
        }
    }


    private void showCallingView() {
        findViewById(R.id.calling_view).setVisibility(View.VISIBLE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
    }

    private void showTalkingView() {
        final OnTalkingListener listener = VideoTalkHelper.getInstance().getTalkingListener();
        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onTalking();
                    handler.postDelayed(this, 1000);
                }
            }
        });

        isTalking = true;
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.talking_view).setVisibility(View.VISIBLE);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
        setupViews();
    }

    private void onPickup() {
        voipManager.accept(this, targetId, new IXHResultCallback() {
            @Override
            public void success(Object data) {
                MLOC.d("newVoip", "onPickup OK! RecSessionId:" + data);
            }

            @Override
            public void failed(String errMsg) {
                MLOC.d("newVoip", "onPickup failed ");
                stopAndFinish();
            }
        });
        showTalkingView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.calling_hangup) {
            voipManager.cancel(new IXHResultCallback() {
                @Override
                public void success(Object data) {
                    stopAndFinish();
                }

                @Override
                public void failed(String errMsg) {
                    stopAndFinish();
                }
            });
            if (xhsdkHelper != null) {
                xhsdkHelper.stopPerview();
                xhsdkHelper = null;
            }
        } else if (id == R.id.talking_hangup) {
            hangup();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        XHScreenRecorder mRecorder = new XHScreenRecorder(this, resultCode, data);
        voipManager.resetRecorder(mRecorder);
    }

    private void stopAndFinish() {
        if (starRTCAudioManager != null) {
            starRTCAudioManager.stop();
        }
        VoipActivity.this.finish();
    }

}
