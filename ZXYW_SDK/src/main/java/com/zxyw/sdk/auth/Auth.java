package com.zxyw.sdk.auth;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.zxyw.sdk.net.http.HttpUtil;
import com.zxyw.sdk.speaker.Speaker;
import com.zxyw.sdk.tools.Path;
import com.zxyw.sdk.tools.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Auth {

    private static boolean auth = false;
    private final static String SP_NAME = "auth_cache";
    private final static String KEY_SN = "SN";
    private final static String KEY_LICENCE = "LICENCE";
    private final static String KEY_UUID = "uuid";

    public static boolean isAuth() {
        return auth;
    }

    public static void backupSN(final Context context, final String sn) {
        final String encodeSN = AESUtil.encode(sn);
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SN, encodeSN).apply();
        final String packageName = context.getPackageName();
        final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(packageName));
        try (final FileWriter writer = new FileWriter(file)){
            if (!file.exists() || file.isFile()){
                if(!file.createNewFile()) return;
            }
            writer.write(encodeSN);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String readSN(final Context context) {
        String cache = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(KEY_SN, null);
        String sn = null;
        final String packageName = context.getPackageName();
        final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(packageName));
        if (TextUtils.isEmpty(cache)) {
            if (file.exists() && file.isFile()) {
                try (final FileReader reader = new FileReader(file)) {
                    final char[] buff = new char[64];
                    final int len = reader.read(buff);
                    cache = new String(buff, 0, len);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!TextUtils.isEmpty(cache)){
                context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SN, cache).apply();
            }
        }else {
            if (!file.exists() || file.isFile()){
                try {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (file.exists() && file.isFile()){
                try (final FileWriter writer = new FileWriter(file)){
                    writer.write(cache);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        if (!TextUtils.isEmpty(cache)){
            sn = AESUtil.decode(cache);
        }
        return sn;
    }

    public static boolean checkAuth(final Context context, final String sn, final String url, final String key) {
        if (context == null || sn == null) return false;
        final String a = a();
        if (a == null) {
            return false;
        }
        final String licence = AESUtil.decode(context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(KEY_LICENCE, null));
        if (licence != null) {
            String b = a.substring(0, 8);
            b = b + a.substring(a.length() - 8);
            String des;
            try {
                des = DesUtil.encrypt(sn, b);
            } catch (Exception e) {
                e.printStackTrace();
                des = "";
            }
            auth = licence.equals(des);
        }
        if (auth) return true;
        else if (!TextUtils.isEmpty(url)) {
            String uuid = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(KEY_UUID, null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putString(KEY_UUID, uuid).apply();
            }
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("android_id", Utils.string2MD5(Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)));
                jsonObject.put("cpu_sn", a);
                jsonObject.put("uuid", Utils.string2MD5(uuid));
                jsonObject.put("bd_device_id", new FaceAuth().getDeviceId(context));
                jsonObject.put("sn", sn);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    HttpUtil.post(url, AESUtil.encode(jsonObject.toString()), new HttpUtil.HttpCallback() {
                        @Override
                        public void onFailed(String error) {

                        }

                        @Override
                        public void onSuccess(String bodyString) {
                            JSONObject json;
                            try {
                                json = new JSONObject(bodyString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return;
                            }
                            if (json.optInt("status") == 200) {
                                final String s;
                                try {
                                    s = DesUtil.decrypt(json.optString("result"), key);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return;
                                }
                                if (!TextUtils.isEmpty(s)) {
                                    String b = a.substring(0, 8);
                                    b = b + a.substring(a.length() - 8);
                                    String des;
                                    try {
                                        des = DesUtil.encrypt(sn, b);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        des = "";
                                    }
                                    if (s.equals(des)) {
                                        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putString(KEY_LICENCE, AESUtil.encode(s)).apply();
                                        Speaker.getInstance().speak("系统激活成功");
                                        Utils.reboot(5000);
                                    } else {
                                        Speaker.getInstance().speak("系统激活失败");
                                    }
                                }
                            }
                        }
                    });
                }
            }, 2000);
        }
        return false;
    }

    private static String a() {
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

    public static String getAuthCode(String sn, long SeqNum, String key) {
        String devKey;
        String authCode;
        try {
            String headSq = Long.toHexString(SeqNum);
            String tailSq = Long.toHexString(~SeqNum);
            StringBuffer sb = null;
            int strLen = headSq.length();
            if (strLen < 8) {
                while (strLen < 8) {
                    sb = new StringBuffer();
                    sb.append("0").append(headSq);
                    headSq = sb.toString();
                    strLen = headSq.length();
                }
                headSq = sb.toString();
            }
            String strSeq = headSq + tailSq.substring(8);
            devKey = DesUtil.encrypt(sn, key);
            String sqDesResult = DesUtil.encrypt(strSeq, devKey);
            authCode = ByteUtil.getXor(sqDesResult, "FB0819B8CE0926F9");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return authCode;
    }
}
