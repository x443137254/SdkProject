package com.zxyw.sdk.videoTalk;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogHelper {
    private final static boolean enable = false;
    private final static String path = Environment.getExternalStorageDirectory()
            + "/Fiacs/video_talk_log/"
            + new SimpleDateFormat("MM.dd", Locale.CHINA).format(new Date())
            + ".log";
    private final static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);

    public static void log(String s) {
        if (!enable) return;
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            File parentFile = file.getParentFile();
            if (!parentFile.exists() || parentFile.isFile()) {
                if (!parentFile.mkdirs()) return;
            }
            try {
                if (!file.createNewFile()) return;
            } catch (IOException e) {
                return;
            }
        }
        String log = "\n" + format.format(new Date()) + " " + s;
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(log.getBytes());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
