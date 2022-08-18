package com.zxyw.sdk.videoTalk.voip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.starrtc.starrtcsdk.api.XHClient;
import com.starrtc.starrtcsdk.apiInterface.IXHResultCallback;
import com.zxyw.sdk.R;
import com.zxyw.sdk.videoTalk.Delegate;
import com.zxyw.sdk.videoTalk.VideoTalkConfig;
import com.zxyw.sdk.videoTalk.VideoTalkHelper;
import com.zxyw.sdk.videoTalk.comm.MLOC;
import com.zxyw.sdk.videoTalk.utils.AEvent;
import com.zxyw.sdk.videoTalk.utils.IEventListener;

public class VoipRingingActivity extends Activity implements View.OnClickListener, IEventListener {

    private String targetId;
    private final int PICK_UP = 0;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PICK_UP) {
                pickUp.callOnClick();
            }
        }
    };
    private TextView pickUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_voip_ringing);

        Window _window = getWindow();
        WindowManager.LayoutParams params = _window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
        _window.setAttributes(params);

        addListener();

        targetId = getIntent().getStringExtra("targetId");
        findViewById(R.id.ring_hangoff).setOnClickListener(this);
        pickUp = findViewById(R.id.ring_pickup);
        pickUp.setOnClickListener(this);

        if (targetId.contains(VideoTalkConfig.getServerIp())) {
            pickUp.callOnClick();
        } else {
            VideoTalkConfig.ResponseType type = VideoTalkConfig.getResponseType();
            switch (type) {
                case PICKUP_AUTO:
                    pickUp.callOnClick();
                    return;
                case PICKUP_AUTO_DELAY:
                    handler.sendEmptyMessageDelayed(PICK_UP, 30 * 1000);
                    break;
                case PICKUP_HAND:
                    break;
            }
        }

        Delegate delegate = VideoTalkHelper.getInstance().getDelegate();
        if (delegate != null) delegate.bellStart();
    }

    public void addListener() {
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_HANGUP, this);
        AEvent.addListener(AEvent.AEVENT_VOIP_REV_ERROR, this);
    }

    public void removeListener() {
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_HANGUP, this);
        AEvent.removeListener(AEvent.AEVENT_VOIP_REV_ERROR, this);
    }

    @Override
    public void dispatchEvent(final String aEventID, boolean success, final Object eventObj) {
        switch (aEventID) {
            case AEvent.AEVENT_VOIP_REV_HANGUP:
                MLOC.d("", "对方已挂断");
                MLOC.showMsg(VoipRingingActivity.this, "对方已挂断");
                finish();
                break;
            case AEvent.AEVENT_VOIP_REV_ERROR:
                MLOC.showMsg(VoipRingingActivity.this, (String) eventObj);
                finish();
                break;
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        addListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        removeListener();
        handler.removeMessages(PICK_UP);
        Delegate delegate = VideoTalkHelper.getInstance().getDelegate();
        if (delegate != null) delegate.bellStop();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ring_hangoff) {
            refuse();
        } else if (id == R.id.ring_pickup) {
            handler.removeMessages(PICK_UP);
            Intent intent = new Intent(VoipRingingActivity.this, VoipActivity.class);
            intent.putExtra("targetId", targetId);
            intent.putExtra(VoipActivity.ACTION, VoipActivity.RING);
            startActivity(intent);
            finish();
        }
    }

    private void refuse() {
        XHClient.getInstance().getVoipManager().refuse(new IXHResultCallback() {
            @Override
            public void success(Object data) {
                finish();
            }

            @Override
            public void failed(String errMsg) {
                finish();
            }
        });
    }
}
