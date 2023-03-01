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
        //序列号加密
        final String encodeSN = AESUtil.encode(sn);
        //加密后的值存入sp文件
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit().putString(KEY_SN, encodeSN).apply();
        //加密后的值更新外部文件
        final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(context.getPackageName()));
        String cache = null;
        if (file.exists() && file.isFile()) {
            try (final FileReader reader = new FileReader(file)) {
                final char[] buff = new char[10240];
                final int len = reader.read(buff);
                cache = new String(buff, 0, len);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject;
        try {
            if (cache == null) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = new JSONObject(cache);
            }
            jsonObject.put(KEY_LICENCE, encodeSN);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        try (final FileWriter writer = new FileWriter(file)) {
            if (!file.exists() || !file.isFile()) {
                if (!file.createNewFile()) return;
            }
            writer.write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readSN(final Context context) {
        //先读取sp缓存
        String encodeSN = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(KEY_SN, null);
        //读取外部空间文件缓存
        final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(context.getPackageName()));
        String cache = null;
        if (file.exists() && file.isFile()) {
            try (final FileReader reader = new FileReader(file)) {
                final char[] buff = new char[10240];
                final int len = reader.read(buff);
                cache = new String(buff, 0, len);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //将文件缓存内容置换为json对象
        JSONObject jsonObject = null;
        if (cache != null && !cache.equals("")) {
            try {
                jsonObject = new JSONObject(cache);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }

        if (TextUtils.isEmpty(encodeSN)) {//如果sp缓存没有，则序列号从文件缓存json中取
            encodeSN = jsonObject.optString(KEY_SN, "");
        } else {//如果sp缓存存在，则更新文件缓存内容
            try {
                jsonObject.put(KEY_SN, encodeSN);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SN, jsonObject.optString(KEY_SN, ""))
                    .apply();
            try (final FileWriter writer = new FileWriter(file)) {
                if (!file.exists() || !file.isFile()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
                writer.write(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //如果缓存值取到了，解码返回
        if (!TextUtils.isEmpty(encodeSN)) {
            return AESUtil.decode(encodeSN);
        }
        return null;
    }

    public static boolean checkAuth(final Context context, final String sn, final String url, final String key) {
        if (context == null || sn == null) return false;
        final String a = a();
        if (a == null) {
            return false;
        }
        //先从sp文件读取
        String encodeLicence = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).getString(KEY_LICENCE, null);
        //读取外部空间文件
        final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(context.getPackageName()));
        String cache = null;
        if (file.exists() && file.isFile()) {
            try (final FileReader reader = new FileReader(file)) {
                final char[] buff = new char[10240];
                final int len = reader.read(buff);
                cache = new String(buff, 0, len);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = null;
        if (cache != null && !cache.equals("")) {
            try {
                jsonObject = new JSONObject(cache);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }

        if (encodeLicence == null || encodeLicence.equals("")) {//如果sp缓存没有值，从外部空间文件读取
            encodeLicence = jsonObject.optString(KEY_LICENCE, "");
        }else {//如果sp缓存有值，则更新外部空间文件内容
            try {
                jsonObject.put(KEY_LICENCE, encodeLicence);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try (final FileWriter writer = new FileWriter(file)) {
                if (!file.exists() || !file.isFile()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
                writer.write(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final String licence = AESUtil.decode(encodeLicence);
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
        getAuthCode(context, sn, url, key, a);
        return false;
    }

    public static void getAuthCode(Context context, String sn, String url, String key) {
        getAuthCode(context, sn, url, key, a());
    }

    private static void getAuthCode(Context context, String sn, String url, String key, String a) {
        if (!TextUtils.isEmpty(url)) {
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
                                //激活码先存入sp文件
                                final String encodeLicence = AESUtil.encode(s);
                                context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(KEY_LICENCE, encodeLicence)
                                        .apply();
                                //激活码继续存入公共空间缓存文件
                                final File file = new File(Path.FTP_ROOT + "/" + Utils.string2MD5(context.getPackageName()));
                                String cache = null;
                                if (file.exists() && file.isFile()) {
                                    try (final FileReader reader = new FileReader(file)) {
                                        final char[] buff = new char[10240];
                                        final int len = reader.read(buff);
                                        cache = new String(buff, 0, len);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                JSONObject jsonObject = null;
                                if (cache != null && !cache.equals("")) {
                                    try {
                                        jsonObject = new JSONObject(cache);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (jsonObject == null) {
                                    jsonObject = new JSONObject();
                                }
                                try {
                                    jsonObject.put(KEY_LICENCE, encodeLicence);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try (final FileWriter writer = new FileWriter(file)) {
                                    if (!file.exists() || !file.isFile()) {
                                        //noinspection ResultOfMethodCallIgnored
                                        file.createNewFile();
                                    }
                                    writer.write(jsonObject.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                Speaker.getInstance().speak("系统激活成功");
                                Utils.reboot(3000);
                            } else {
                                Speaker.getInstance().speak("系统激活失败");
                            }
                        }
                    }
                }
            });
        }
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
