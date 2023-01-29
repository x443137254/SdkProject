package com.zxyw.sdk.tools;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MyLog {
    private static final Format format = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);
    private final static BlockingQueue<LogContent> queue = new LinkedBlockingQueue<>();
    private static int maxSize = 100;

    static {
        new Thread(() -> {
            while (true) {
                try {
                    writeToFile(queue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * @param maxSize 日志文件夹最大使用空间（单位M）
     */
    public static void setMaxSize(int maxSize) {
        MyLog.maxSize = maxSize;
    }

    public static void w(String tag, String msg) { // 警告信息
        if (tag == null || tag.equals("")) tag = "null";
        if (msg == null) msg = "null";
        addQueue(tag, msg);
        Log.w(tag, msg);
    }

    private static void addQueue(String tag, String msg) {
        LogContent log = new LogContent();
        log.tag = tag;
        log.time = format.format(new Date());
        log.content = msg;
        queue.offer(log);
    }

    public static void e(String tag, String msg) {
        if (tag == null || tag.equals("")) tag = "null";
        if (msg == null) msg = "null";
        addQueue(tag, msg);
        Log.e(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (tag == null || tag.equals("")) tag = "null";
        if (msg == null) msg = "null";
        addQueue(tag, msg);
        Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (tag == null || tag.equals("")) tag = "null";
        if (msg == null) msg = "null";
        addQueue(tag, msg);
        Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (tag == null || tag.equals("")) tag = "null";
        if (msg == null) msg = "null";
        addQueue(tag, msg);
        Log.v(tag, msg);
    }

    private static void writeToFile(LogContent log) {
        if (log == null) return;
        checkDiskFree();
        final String fileName = Path.LOG + File.separator +
                new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date()) + ".log";
        File file = new File(fileName);
        if (!file.exists() || file.isDirectory()) {
            File parent = file.getParentFile();
            if (parent != null && (!parent.exists() || parent.isFile())) {
                if (!parent.mkdirs()) {
                    return;
                }
            }
        }
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            fileWriter.append(log.time)
                    .append(" ")
                    .append(log.tag)
                    .append(" ")
                    .append(log.content)
                    .append("\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkDiskFree() {
        final File file = new File(Path.LOG);
        if (file.exists()) {
            final File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                long totalSize = 0;
                File deleteFile = files[0];
                for (File f : files) {
                    totalSize += f.length();
                    if (f.lastModified() < deleteFile.lastModified()) {
                        deleteFile = f;
                    }
                }
                if (totalSize > maxSize * 1024 * 1024) {
                    //noinspection ResultOfMethodCallIgnored
                    deleteFile.delete();
                }
            }
        }
    }

    private static class LogContent {
        String tag;
        String time;
        String content;
    }
}