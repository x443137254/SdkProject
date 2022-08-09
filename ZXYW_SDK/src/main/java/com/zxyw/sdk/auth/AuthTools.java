package com.zxyw.sdk.auth;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class AuthTools {
    private static final String key = "27609AC8DE0716E8";

    static boolean saveFile(String s, String path) {
        if (s == null) return false;
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            File parentFile = file.getParentFile();
            if (parentFile == null) return false;
            if (!parentFile.exists() || parentFile.isFile()) {
                if (!parentFile.mkdirs()) return false;
            }
            try {
                if (!file.createNewFile()) return false;
            } catch (IOException e) {
                return false;
            }
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            String des = DesUtil.encrypt(s, key);
            if (des == null) return false;
            stream.write(des.getBytes());
            stream.flush();
            stream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String readFile(String path) {
        String sn = null;
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                sn = br.readLine();
                br.close();
            } catch (IOException e) {
                return null;
            }
        }
        if (TextUtils.isEmpty(sn)) return null;
        try {
            return DesUtil.decrypt(sn, key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
