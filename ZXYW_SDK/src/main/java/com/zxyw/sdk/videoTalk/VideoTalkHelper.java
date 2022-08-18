package com.zxyw.sdk.videoTalk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.starrtc.starrtcsdk.api.XHClient;
import com.zxyw.sdk.videoTalk.comm.KeepLiveService;
import com.zxyw.sdk.videoTalk.comm.MLOC;
import com.zxyw.sdk.videoTalk.utils.AEvent;
import com.zxyw.sdk.videoTalk.voip.VoipActivity;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class VideoTalkHelper {
    private static VideoTalkHelper instance;
    private Delegate delegate;
    private Intent serviceIntent;
    private OnTalkingListener talkingListener;

    private VideoTalkHelper() {
    }

    public static VideoTalkHelper getInstance() {
        if (instance == null) instance = new VideoTalkHelper();
        return instance;
    }

    public void init(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String address = null;
                int times = 0;
                while (address == null) {
                    times++;
                    address = getIPAddress();
                    if (times > 1) SystemClock.sleep(500);
                    if (times > 100) break;
                }
                if (address == null) return;
                MLOC.userId = address;
                serviceIntent = new Intent(context, KeepLiveService.class);
                context.startService(serviceIntent);
            }
        }).start();
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public boolean check() {
        return XHClient.getInstance().getIsOnline();
    }

    public void requestTalk(Activity activity, String targetIp) {
        if (activity == null || targetIp == null || !check()) return;
        Intent intent = new Intent(activity, VoipActivity.class);
        intent.putExtra("targetId", targetIp);
        intent.putExtra(VoipActivity.ACTION, VoipActivity.CALLING);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private String getIPAddress() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    public void stop(Context context){
        if (serviceIntent != null && context != null) {
            context.stopService(serviceIntent);
        }
    }

    public void setTalkingListener(OnTalkingListener talkingListener) {
        this.talkingListener = talkingListener;
    }

    public OnTalkingListener getTalkingListener() {
        return talkingListener;
    }

    public void hangup(){
        AEvent.notifyListener(AEvent.AEVENT_VOIP_REV_HANGUP,true,"");
    }
}
