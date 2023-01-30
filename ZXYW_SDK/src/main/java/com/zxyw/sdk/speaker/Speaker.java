package com.zxyw.sdk.speaker;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.zxyw.sdk.tools.MyLog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class Speaker {
    private final String TAG = "Speaker";
    private final String appid = "5ab22c72";
    private static Speaker instance;
    private SpeechSynthesizer mTts;
    private VoiceWakeuper mIvw;
    private SpeechRecognizer mAsr;
    private final Queue<String> queue;
    volatile private boolean talking;
    final private Handler handler;
//    private String speakContent;
    final private Runnable speakRunnable;
    private SpeakStatusChangeListener statusChangeListener;
    private WakeuperListener mWakeuperListener;
    private RecognizerListener recognizerListener;
    private WakeupListener wakeupListener;
    private CommendListener commendListener;
    private int wakeupScore = 1400;

    private Speaker() {
        queue = new LinkedList<>();
        talking = false;
        HandlerThread thread = new HandlerThread(getClass().getSimpleName());
        thread.start();
        handler = new Handler(thread.getLooper());
        speakRunnable = () -> {
            if (mTts != null && !queue.isEmpty()) {
                talking = true;
                mTts.startSpeaking(queue.poll(), synthesizerListener);
            }
        };
    }

    public void setWakeupScore(int wakeupScore) {
        this.wakeupScore = wakeupScore;
    }

    public void setStatusChangeListener(SpeakStatusChangeListener statusChangeListener) {
        this.statusChangeListener = statusChangeListener;
    }

    public void setCommendListener(CommendListener commendListener) {
        this.commendListener = commendListener;
    }

    public static Speaker getInstance() {
        if (instance == null) instance = new Speaker();
        return instance;
    }

    public void init(final Context context, boolean b1, boolean b2, boolean b3) {
        String param = "appid=" + appid + "," + SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC;
        SpeechUtility.createUtility(context.getApplicationContext(), param);
        if (b1) {
            mTts = SpeechSynthesizer.createSynthesizer(context.getApplicationContext(), code -> {
                if (code != ErrorCode.SUCCESS) {
                    mTts = null;
                    MyLog.e(TAG, "语音合成初始化失败！错误码：" + code);
                } else {
                    setTtsParam(context);
                }
            });
        }
        if (b2) {
            mIvw = VoiceWakeuper.createWakeuper(context.getApplicationContext(), code -> {
                if (code != ErrorCode.SUCCESS) {
                    mIvw = null;
                    MyLog.e(TAG, "语音唤醒初始化失败！错误码：" + code);
                }
                //            else {
                //                setIvwParam(context);
                //            }
            });
        }
        if (b3) {
            mAsr = SpeechRecognizer.createRecognizer(context.getApplicationContext(), code -> {
                if (code != ErrorCode.SUCCESS) {
                    mAsr = null;
                    MyLog.e(TAG, "语音识别初始化失败！错误码：" + code);
                } else {
                    setAsrParam(context);
                }
            });
        }
    }

    /**
     * 设置命令识别参数
     */
    private void setAsrParam(Context context) {
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        // 设置本地识别资源
        final String s1 = ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet");
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, s1);
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, context.getExternalFilesDir("msc").getAbsolutePath() + "/Grammar");
        // 设置返回结果格式
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置本地识别使用语法id
        mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "call");
        // 设置识别的门限值
        mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
        // 使用8k音频的时候请解开注释
//        mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, context.getExternalFilesDir("msc").getAbsolutePath() + "/asr.wav");
        mAsr.buildGrammar("bnf", readFile(context), (grammarId, speechError) -> {
            if (speechError == null) {
                MyLog.d(TAG, "语法构建成功：" + grammarId);
            } else {
                MyLog.e(TAG, "语法构建失败：" + speechError.toString());
            }
        });
        recognizerListener = new RecognizerListener() {

            @Override
            public void onVolumeChanged(int volume, byte[] data) {
            }

            @Override
            public void onResult(final RecognizerResult result, boolean isLast) {
                if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                    parseGrammarResult(result.getResultString());
                } else {
                    MyLog.d(TAG, "recognizer result : null");
                }
            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onBeginOfSpeech() {

            }

            @Override
            public void onError(SpeechError error) {
                if (commendListener != null) {
                    commendListener.onRecognize(null, 0, error.getErrorCode());
                }
                MyLog.e(TAG, "Recognize error: " + error.getErrorCode());
            }

            @Override
            public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {//离线功能用不到

            }

        };
    }

    private void parseGrammarResult(String json) {
        try {
            JSONObject joResult = new JSONObject(new JSONTokener(json));
            JSONArray words = joResult.getJSONArray("ws");
            if (words.length() > 0) {
                JSONObject wsItem = words.getJSONObject(0);
                JSONArray items = wsItem.getJSONArray("cw");
                if (!"<contact>".equals(wsItem.getString("slot"))) {
                    //本地多候选按照置信度高低排序，一般选取第一个结果即可
                    final String w = items.getJSONObject(0).optString("w");
                    final int score = joResult.optInt("sc");
                    if (!w.contains("nomatch") && commendListener != null) {
                        commendListener.onRecognize(w, score, ErrorCode.SUCCESS);
                    }
                    MyLog.d(TAG, "recognize success! word: " + w + "  score: " + score);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (commendListener != null) {
            commendListener.onRecognize(null, 0, ErrorCode.ERROR_NO_MATCH);
        }
        MyLog.d(TAG, "recognize error: " + ErrorCode.ERROR_NO_MATCH);
    }

    private String readFile(Context mContext) {
        try (InputStream in = mContext.getAssets().open("call.bnf")) {
            int len = in.available();
            byte[] buf = new byte[len];
            //noinspection ResultOfMethodCallIgnored
            in.read(buf, 0, len);
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 设置语音唤醒的参数
     */
    private void setIvwParam(Context context) {
        // 清空参数
        mIvw.setParameter(SpeechConstant.PARAMS, null);
        // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
        mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + wakeupScore);
        // 设置唤醒模式
        mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
        // 设置持续进行唤醒
        mIvw.setParameter(SpeechConstant.KEEP_ALIVE, "1");
        // 设置闭环优化网络模式
        mIvw.setParameter(SpeechConstant.IVW_NET_MODE, "0");
        // 设置唤醒资源路径
        final String s = ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + appid + ".jet");
        mIvw.setParameter(SpeechConstant.IVW_RES_PATH, s);
        // 设置唤醒录音保存路径，保存最近一分钟的音频
        mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH,
                context.getExternalFilesDir("msc").getAbsolutePath() + "/ivw.wav");
        mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
        //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );

        mWakeuperListener = new WakeuperListener() {

            @Override
            public void onResult(WakeuperResult result) {
                try {
                    final String resultString = result.getResultString();
                    final String score = new JSONObject(resultString).optString("score");
                    final int i = Integer.parseInt(score);
                    MyLog.d(TAG, "wakeup result: " + resultString);
                    if (i > wakeupScore && wakeupListener != null) {
                        wakeupListener.onWakeup();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(SpeechError error) {
                MyLog.e(TAG, error.toString());
            }

            @Override
            public void onBeginOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {

            }

            @Override
            public void onVolumeChanged(int volume) {
            }
        };
    }

    public void startRecognize() {
        if (mAsr != null) {
            mAsr.startListening(recognizerListener);
        }
    }

    public void stopRecognize() {
        if (mAsr != null) {
            mAsr.stopListening();
        }
    }

    public void startWakeupListen(Context context) {
        if (mIvw != null) {
            setIvwParam(context);
            mIvw.startListening(mWakeuperListener);
        }
    }

    public void stopWakeupListen() {
        if (mIvw != null) {
            mIvw.stopListening();
        }
    }

    public void setWakeupListener(WakeupListener wakeupListener) {
        this.wakeupListener = wakeupListener;
    }

    public void speakNow(String content) {
        queue.clear();
        queue.add(content);
        handler.post(speakRunnable);
    }

    public void speak(String content) {
        if (mTts == null) return;
        if (talking) {
            queue.clear();
            queue.add(content);
        } else {
            queue.add(content);
            handler.post(speakRunnable);
            if (statusChangeListener != null) {
                statusChangeListener.speakerStart();
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
            if (queue.isEmpty()) {
                talking = false;
                if (statusChangeListener != null) {
                    statusChangeListener.speakerStop();
                }
            } else {
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

    private void setTtsParam(Context context) {
        Setting.setShowLog(false);
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS);
        //设置发音人资源路径
        final String s = ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "xtts/common.jet")
                + ";"
                + ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "xtts/xiaoyan.jet");
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, s);
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
