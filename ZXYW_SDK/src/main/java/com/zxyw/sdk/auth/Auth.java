package com.zxyw.sdk.auth;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class Auth {
    private final String SN_PATH = "/sdcard/.zxyw/sn";
    private final String AUTH_PATH = "/sdcard/.zxyw/auth";

    private boolean auth = false;

    public boolean isAuth() {
        return auth;
    }

    public boolean backupSN(String sn) {
        return AuthTools.saveFile(sn, SN_PATH);
    }

    public String readSN() {
        return AuthTools.readFile(SN_PATH);
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public String readAuth() {
        return AuthTools.readFile(AUTH_PATH);
    }

    public boolean authBackup(String s) {
        return AuthTools.saveFile(s, AUTH_PATH);
    }

    public boolean checkAuth(String sn) {
        String s = AuthTools.readFile(AUTH_PATH);
        if (s == null || s.length() == 0) {
            auth = false;
            return false;
        }
        String a = a();
        if (a == null) {
            auth = false;
            return false;
        }
        String b = a.substring(0,8);
        b = b + a.substring(a.length() - 8);
        String des;
        try {
            des = DesUtil.encrypt(sn, b);
        } catch (Exception e) {
            e.printStackTrace();
            des = "";
        }
        auth = s.equals(des);
        return auth;
    }

    private String a() {
        String s1, s2, s3 = "0000000000000000";
        try {
            Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                s1 = input.readLine();
                if (s1 != null) {
                    if (s1.contains("Serial")) {
                        s2 = s1.substring(s1.indexOf(":") + 1);
                        s3 = s2.trim();
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return s3;
    }
}
