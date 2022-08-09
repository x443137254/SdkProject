package com.zxyw.sdk.tools;

import android.os.Environment;

import java.io.File;

/**
 * 存放所有的路径，目录在app初始化时创建
 */
public final class Path {
    public static final String FTP_ROOT = Environment.getExternalStorageDirectory() + File.separator + "ZXYW";
    public static final String LOG = FTP_ROOT + File.separator + "log";
    public static final String APK = FTP_ROOT + File.separator + "apk";
    public static final String BANNER = FTP_ROOT + File.separator + "banner";
    public static final String CRASH = FTP_ROOT + File.separator + "crash";
    public static final String TEMP = FTP_ROOT + File.separator + "temp";
    public static final String WALLPAPER = FTP_ROOT + File.separator + "wallpaper";
    public static final String PHOTO = FTP_ROOT + File.separator + "photo";
    public static final String CAPTURE = FTP_ROOT + File.separator + "capture";

    /**
     * 创建各个目录
     */
    public static void init() {
        mkdirs();
    }

    private static void mkdirs() {
        mkDir(new File(FTP_ROOT));
        mkDir(new File(LOG));
        mkDir(new File(APK));
        mkDir(new File(PHOTO));
        mkDir(new File(BANNER));
        mkDir(new File(CRASH));
        mkDir(new File(CAPTURE));
        mkDir(new File(TEMP));
        mkDir(new File(WALLPAPER));
    }

    private static void mkDir(File file) {
//        if (file == null) return;
        if (file.exists() && file.isFile()) {
            if (!file.delete()) {
                MyLog.e("mkdir", "delete file error! " + file.getName());
            }
        }
        if (!file.exists()) {
            if (!file.mkdirs()) {
                MyLog.e("mkdir", "create dir error! " + file.getName());
            }
        }
    }
}
