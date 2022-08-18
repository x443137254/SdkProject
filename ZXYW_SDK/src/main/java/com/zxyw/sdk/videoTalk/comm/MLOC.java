package com.zxyw.sdk.videoTalk.comm;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.zxyw.sdk.videoTalk.VideoTalkConfig;
import com.zxyw.sdk.videoTalk.database.CoreDB;
import com.zxyw.sdk.videoTalk.database.HistoryBean;
import com.zxyw.sdk.videoTalk.database.MessageBean;

import java.util.List;

/**
 * Created by zhangjt on 2017/8/17.
 */
@SuppressWarnings("all")
public class MLOC {
    public static Context appContext;
    public static String userId = "";

    //    public static String SERVER_HOST = "demo.starrtc.com";
    private static String SERVER_HOST = "";
    private static String VOIP_SERVER_URL;
    private static String IM_SERVER_URL;
    private static String CHATROOM_SERVER_URL;
    private static String LIVE_VDN_SERVER_URL;
    private static String LIVE_SRC_SERVER_URL;
    private static String LIVE_PROXY_SERVER_URL;

    public static Boolean AEventCenterEnable = false;

    public static Boolean hasLogout = false;

    public static boolean hasNewC2CMsg = false;
    public static boolean hasNewGroupMsg = false;
    public static boolean hasNewVoipMsg = false;
    public static boolean canPickupVoip = true;

    public static boolean deleteGroup = false;

    private static CoreDB coreDB;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
//        userId = loadSharedData(context, "userId", userId);
//        if (userId.equals("")) {
//            userId = "" + (new Random().nextInt(900000) + 100000);
//            saveUserId(MLOC.userId);
//        }
        if (coreDB == null) {
            coreDB = new CoreDB(context);
        }

//        VOIP_SERVER_URL = loadSharedData(context, "VOIP_SERVER_URL", VOIP_SERVER_URL);
//        IM_SERVER_URL = loadSharedData(context, "IM_SERVER_URL", IM_SERVER_URL);
//        LIVE_SRC_SERVER_URL = loadSharedData(context, "LIVE_SRC_SERVER_URL", LIVE_SRC_SERVER_URL);
//        LIVE_PROXY_SERVER_URL = loadSharedData(context, "LIVE_PROXY_SERVER_URL", LIVE_PROXY_SERVER_URL);
//        LIVE_VDN_SERVER_URL = loadSharedData(context, "LIVE_VDN_SERVER_URL", LIVE_VDN_SERVER_URL);
//        CHATROOM_SERVER_URL = loadSharedData(context, "CHATROOM_SERVER_URL", CHATROOM_SERVER_URL);
        SERVER_HOST = VideoTalkConfig.getServerIp();

        if (loadSharedData(context, "AEC_ENABLE", "0").equals("0")) {
            AEventCenterEnable = false;
        } else {
            AEventCenterEnable = true;
        }
    }

    public static String getVoipServerUrl() {
        return SERVER_HOST + ":10086";
    }

    public static String getImServerUrl() {
        return SERVER_HOST + ":19903";
    }

    public static String getChatroomServerUrl() {
        return SERVER_HOST + ":19906";
    }

    public static String getLiveVdnServerUrl() {
        return SERVER_HOST + ":19928";
    }

    public static String getLiveSrcServerUrl() {
        return SERVER_HOST + ":19931";
    }

    public static String getLiveProxyServerUrl() {
        return SERVER_HOST + ":19932";
    }

    private static Boolean debug = true;

    public static void setDebug(Boolean b) {
        debug = b;
    }

    public static void d(String tag, String msg) {
        if (debug) {
            Log.d("starSDK_demo_" + tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        Log.e("starSDK_demo_" + tag, msg);
    }

    private static Toast mToast;

    public static void showMsg(String str) {
        try {
            if (mToast != null) {
                mToast.setText(str);
                mToast.setDuration(Toast.LENGTH_SHORT);
            } else {
                mToast = Toast.makeText(appContext.getApplicationContext(), str, Toast.LENGTH_SHORT);
            }
            mToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showMsg(Context context, String str) {
        try {
            if (mToast != null) {
                mToast.setText(str);
                mToast.setDuration(Toast.LENGTH_SHORT);
            } else {
                mToast = Toast.makeText(context.getApplicationContext(), str, Toast.LENGTH_SHORT);
            }
            mToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<HistoryBean> getHistoryList(String type) {
        if (coreDB != null) {
            return coreDB.getHistory(type);
        } else {
            return null;
        }
    }

    public static void addHistory(HistoryBean history, Boolean hasRead) {
        if (coreDB != null) {
            coreDB.addHistory(history, hasRead);
        }
    }

    public static void updateHistory(HistoryBean history) {
        if (coreDB != null) {
            coreDB.updateHistory(history);
        }
    }

    public static void removeHistory(HistoryBean history) {
        if (coreDB != null) {
            coreDB.removeHistory(history);
        }
    }

    public static List<MessageBean> getMessageList(String conversationId) {
        if (coreDB != null) {
            return coreDB.getMessageList(conversationId);
        } else {
            return null;
        }
    }

    public static void saveMessage(MessageBean messageBean) {
        if (coreDB != null) {
            coreDB.setMessage(messageBean);
        }
    }

    public static void saveSharedData(Context context, String key, String value) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences("stardemo", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String loadSharedData(Context context, String key) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences("stardemo", Activity.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    public static String loadSharedData(Context context, String key, String defValue) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences("stardemo", Activity.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

//    public static void saveUserId(String id) {
//        MLOC.userId = id;
//        MLOC.saveSharedData(appContext, "userId", MLOC.userId);
//    }
}
