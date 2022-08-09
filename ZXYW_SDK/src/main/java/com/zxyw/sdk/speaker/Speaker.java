package com.zxyw.sdk.speaker;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.LinkedList;
import java.util.Queue;

public class Speaker {
    private static Speaker instance;
    private SpeechSynthesizer mTts;
    private final Queue<String> queue;
    private String path;
    volatile private boolean talking;
    final private Handler handler;
    private String speakContent;
    final private Runnable speakRunnable;
    private SpeakStatusChangeListener listener;

    private Speaker() {
        queue = new LinkedList<>();
        talking = false;
        HandlerThread thread = new HandlerThread(getClass().getSimpleName());
        thread.start();
        handler = new Handler(thread.getLooper());
        speakRunnable = new Runnable() {
            @Override
            public void run() {
                if (mTts != null) {
                    mTts.startSpeaking(speakContent, synthesizerListener);
                }
            }
        };
    }

    public void setListener(SpeakStatusChangeListener listener) {
        this.listener = listener;
    }

    public static Speaker getInstance() {
        if (instance == null) instance = new Speaker();
        return instance;
    }

    public void init(final Context context, String appId) {
        String param = "appid=" + appId + "," + SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC;
        SpeechUtility.createUtility(context.getApplicationContext(), param);
        mTts = SpeechSynthesizer.createSynthesizer(context.getApplicationContext(), new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    mTts = null;
                } else {
                    path = getResourcePath(context);
                    setParam();
                }
            }
        });
    }

    public void speakNow(String content) {
        queue.clear();
        speakContent = content;
        handler.post(speakRunnable);
    }

    public void speak(String content) {
        if (mTts == null) return;
        if (talking){
            queue.clear();
            queue.add(content);
        }else {
            talking = true;
            speakContent = content;
            handler.post(speakRunnable);
            if (listener != null) {
                listener.speakerStart();
            }
        }
    }

    final private SynthesizerListener synthesizerListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {
            if (mTts != null) {
                mTts.resumeSpeaking();
            }
        }

        @Override
        public void onSpeakResumed() {
        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            String s = queue.poll();
            if (s == null) {
                talking = false;
                if (listener != null) {
                    listener.speakerStop();
                }
            } else {
                speakContent = s;
                handler.post(speakRunnable);
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
        }
    };

    public boolean isTalking() {
        return talking;
    }

    public boolean auth() {
        return mTts != null;
    }

    private String getResourcePath(Context context) {
        return ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "xtts/common.jet")
                + ";" +
                ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "xtts/xiaoyan.jet");
    }

    private void setParam() {
        Setting.setShowLog(false);
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS);
        //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, path);
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY,"1");//支持实时音频流抛出，仅在synthesizeToUri条件下支持
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "60");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "60");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        //	mTts.setParameter(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC+"");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");

        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }
}
