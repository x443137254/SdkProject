package com.zxyw.sdk.simplevein;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import androidx.annotation.RequiresApi;

import com.xgzx.veinmanager.VeinApi;
import com.zxyw.sdk.tools.MyLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;

import android_serialport_api.SerialPort;

public class SimpleVeinApi {
    private final String TAG = "VeinApi";
    private static final SimpleVeinApi instance = new SimpleVeinApi();
    private SerialPort serialPort;
    private Thread receiveThread;
    private boolean running;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            retryCount++;
            if (retryCount < 3) {
                send(lastSend);
            }
        }
    };
    private final Handler handler;//用于发送失败重试的线程
    private byte[] lastSend;//缓存最后一次发送的指令
    private int retryCount;//发送失败重试次数

    private byte[] template;//读取到的指静脉特征
    private int offset;//特征模板储存长度位置
    private int currentCount;//当前已读取长度
    private int totalCount;//单次读取数据长度
    private byte readCmd;
    //    private String enrollString;
    private EnrollCallback enrollCallback;
    private ReadCallback readCallback;
    private boolean readLoop;
    private String serialPath = "/dev/tty-finger";
    private int baudRate = 57600;
    private boolean showLog;

    private SimpleVeinApi() {
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public static SimpleVeinApi getInstance() {
        return instance;
    }

    /**
     * 修改串口地址，需要在初始化之前调用
     *
     * @param serialPath 串口地址
     */
    public void setSerialPath(String serialPath) {
        this.serialPath = serialPath;
    }

    /**
     * 修改串口波特率，需要在初始化之前调用
     *
     * @param baudRate 串口波特率
     */
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void init() {
        running = true;
        receiveThread = new Thread(() -> {
            while (running && serialPort == null) {
                try {
                    serialPort = new SerialPort(new File(serialPath), baudRate, 0);
                } catch (IOException e) {
                    SystemClock.sleep(1000);
                    serialPort = null;
                }
            }
            if (!running) return;
            log("init: serial port open success");
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            handler.postDelayed(this::connect, 1000);
            final byte[] buff = new byte[32];
            while (running) {
                final int len;
                try {
                    len = inputStream.read(buff);
                } catch (IOException e) {
                    break;
                }
                if (len > 1) {//有时候会收到单个0字节需要跳过
                    final byte[] receiveData = Arrays.copyOf(buff, len);
                    log("receive: " + bytes2string(receiveData));
                    handler.removeCallbacks(retryRunnable);
                    retryCount = 0;
                    try {
                        pars(buff, len);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }
        });
        receiveThread.start();
    }

    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pars(byte[] data, int len) {
        if (lastSend == null) return;
        switch (lastSend[3]) {
            case VeinApi.XG_CMD_CONNECTION://连接指静脉模块
                if (len == 24 && data[0] == Packet.head[0] && data[1] == Packet.head[1]) {
                    try {
                        final Packet packet = new Packet(data);
                        if (packet.sumCheck()) {
                            log("pars: connect success");
                        } else {
                            log("pars: sum check failed");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case VeinApi.XG_CMD_GET_CHARA://读取指静脉特征
                if (len == 24 && data[0] == Packet.head[0] && data[1] == Packet.head[1]) {
                    final Packet packet;
                    try {
                        packet = new Packet(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (readLoop) {
                            getTemplate(true);
                        }
                        break;
                    }
                    if (packet.sumCheck()) {
                        if (packet.data != null && packet.data.length > 0) {
                            switch (packet.data[0]) {
                                case VeinApi.XG_ERR_SUCCESS://读取成功
                                    log("pars: read success, get data");
                                    template = new byte[(packet.data[1] & 0xff) + ((packet.data[2] & 0xff) << 8)];
                                    offset = 0;
                                    readCmd = (byte) VeinApi.XG_CMD_GET_CHARA;
                                    readTemplate();
                                    break;
                                case VeinApi.XG_ERR_FAIL://读取失败
                                    if (readCallback != null) {
                                        readCallback.readTemplate(null);
                                    }
                                    if (readLoop) {
                                        log("pars: read timeout! try again");
                                        getTemplate(true);
                                    }
                                    break;
                                case VeinApi.XG_INPUT_FINGER://请放入手指
                                    log("pars: please put finger in");
                                    break;
                                case VeinApi.XG_RELEASE_FINGER://请移开手指
                                    log("pars: please remove finger");
                                    break;
                            }
                        }
                    } else {
                        log("pars: sum check failed");
                    }
                }
                break;
            case VeinApi.XG_CMD_READ_DATA://读取数据
                currentCount += len;
                if (currentCount <= totalCount) {
                    System.arraycopy(data, 0, template, offset, len);
                    offset += len;
                } else {
                    System.arraycopy(data, 0, template, offset, len - 2);
                    offset += len - 2;
                    //校验和
                    int sum = 0;
                    for (int i = offset - totalCount; i < offset; i++) {
                        sum += template[i] & 0xFF;
                    }
                    if ((((data[len - 1] & 0xFF) << 8) + (data[len - 2] & 0xFF)) == (sum & 0xFFFF)) {
                        if (offset < template.length) {
                            readTemplate();
                        } else {
                            if (readCmd == VeinApi.XG_CMD_READ_ENROLL) {
                                if (enrollCallback != null) {
//                                    enrollCallback.enrollFinish(true, VeinApi.FVEncodeBase64(template, template.length));
                                    enrollCallback.enrollFinish(true, Base64.getEncoder().encodeToString(template));
                                }
//                                enrollString = VeinApi.FVEncodeBase64(template, template.length);
                            } else {
                                if (readCallback != null) {
                                    final long t = System.currentTimeMillis();
//                                    readCallback.readTemplate(VeinApi.FVEncodeBase64(template, template.length));
                                    readCallback.readTemplate(Base64.getEncoder().encodeToString(template));
                                }
                                if (readLoop) {
                                    getTemplate(true);
                                }
//                                final String tempString = VeinApi.FVEncodeBase64(template, template.length);
//                                final String result = VeinApi.FVVerifyUser(enrollString, tempString, 80);
//                                log("Verify Result: " + result);
                            }
                        }
                    } else {
                        log("pars: sum check failed, resend");
                        currentCount = 0;
                        offset -= totalCount;
                        handler.post(retryRunnable);
                    }
                }
                break;
            case VeinApi.XG_CMD_CLEAR_ENROLL:
                _enroll();
                break;
            case VeinApi.XG_CMD_ENROLL:
                if (len == 24 && data[0] == Packet.head[0] && data[1] == Packet.head[1]) {
                    final Packet packet;
                    try {
                        packet = new Packet(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (enrollCallback != null) {
                            enrollCallback.enrollFinish(false, "数据通信异常");
                        }
                        break;
                    }
                    if (packet.sumCheck()) {
                        if (packet.data != null && packet.data.length > 0) {
                            switch (packet.data[0]) {
                                case VeinApi.XG_ERR_SUCCESS://录入成功
                                    log("pars: enroll success, get data");
                                    readEnroll();
                                    break;
                                case VeinApi.XG_ERR_FAIL://录入失败
                                    log("pars: enroll failed");
                                    if (enrollCallback != null) {
                                        enrollCallback.enrollFinish(false, getErrMsg(packet.data[1]));
                                    }
                                    break;
                                case VeinApi.XG_INPUT_FINGER://请放入手指
                                    log("pars: please put finger in");
                                    if (enrollCallback != null) {
                                        enrollCallback.enrollStep(packet.data[1] + 1, false);
                                    }
                                    break;
                                case VeinApi.XG_RELEASE_FINGER://请移开手指
                                    log("pars: please remove finger");
                                    if (enrollCallback != null) {
                                        enrollCallback.enrollStep(packet.data[1] + 1, true);
                                    }
                                    break;
                            }
                        }
                    } else {
                        log("pars: sum check failed");
                    }
                }
                break;
            case VeinApi.XG_CMD_READ_ENROLL:
                if (len == 24 && data[0] == Packet.head[0] && data[1] == Packet.head[1]) {
                    final Packet packet;
                    try {
                        packet = new Packet(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (enrollCallback != null) {
                            enrollCallback.enrollFinish(false, "数据通信异常");
                        }
                        break;
                    }
                    if (packet.sumCheck()) {
                        if (packet.data != null && packet.data.length > 0 && packet.data[0] == VeinApi.XG_ERR_SUCCESS) {
                            log("pars: read success, get data");
                            final int size = (packet.data[1] & 0xff) +
                                    ((packet.data[2] & 0xff) << 8) +
                                    ((packet.data[3] & 0xff) << 16) +
                                    ((packet.data[4] & 0xff) << 24);
                            template = new byte[size];
                            offset = 0;
                            readCmd = (byte) VeinApi.XG_CMD_READ_ENROLL;
                            readTemplate();
                        }
                    } else {
                        log("pars: sum check failed");
                    }
                }
                break;
        }
    }

    private void readTemplate() {
        totalCount = Math.min(template.length - offset, 510);
        final byte[] data = new byte[9];
        data[0] = readCmd;
        data[1] = (byte) (offset & 0xff);
        data[2] = (byte) ((offset >> 8) & 0xff);
        data[3] = (byte) ((offset >> 16) & 0xff);
        data[4] = (byte) ((offset >> 24) & 0xff);
        data[5] = (byte) (totalCount & 0xff);
        data[6] = (byte) ((totalCount >> 8) & 0xff);
        data[7] = (byte) ((totalCount >> 16) & 0xff);
        data[8] = (byte) ((totalCount >> 24) & 0xff);
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_READ_DATA, data).generateBytes();
        currentCount = 0;
        send(bytes);
    }

    private void send(byte[] data) {
        log("start to send: " + bytes2string(data));
        if (outputStream != null) {
            try {
                handler.postDelayed(retryRunnable, 1500);
                outputStream.write(data);
                lastSend = data;
                log("send finish");
            } catch (Exception e) {
                log(e.toString());
            }
        }
    }

    private void connect() {
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_CONNECTION, "00000000".getBytes()).generateBytes();
        send(bytes);
    }

    /**
     * 判断指静脉模板与读取的特征是否匹配
     *
     * @return 若识别未同一手指，返回更新后的模板，否则返回null或-1等长度小于10的值
     */
    public String charMatch(String template, String charData) {
        return VeinApi.FVVerifyUser(template, charData, 80);
    }

    /**
     * 判断2个指静脉模板是否相似
     */
    public boolean templateMatch(String template1, String template2) {
        return VeinApi.FVTempMatch(template1, template2, 3) > 0;
    }

    public void getTemplate() {
        getTemplate(false);
    }

    private void getTemplate(boolean loop) {
        readLoop = loop;
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_GET_CHARA, null).generateBytes();
        send(bytes);
    }

    public void getTemplateLoop() {
        getTemplate(true);
    }

    public void cancelCmd() {
        readLoop = false;
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_CANCEL, null).generateBytes();
        send(bytes);
    }

    public void enroll() {
        if (readLoop) {
            cancelCmd();
            handler.postDelayed(this::enroll, 200);
        } else {
            final byte[] data = new byte[4];
            data[0] = 1;
            final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_CLEAR_ENROLL, data).generateBytes();
            send(bytes);
        }
    }

    private void _enroll() {
        final byte[] data = new byte[16];
        data[0] = 1;
        data[5] = 3;
        data[10] = 10;
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_ENROLL, data).generateBytes();
        send(bytes);
    }

    private void readEnroll() {
        final byte[] data = new byte[4];
        data[0] = 1;
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_READ_ENROLL, data).generateBytes();
        send(bytes);
    }

    public void release() {
        final byte[] bytes = new Packet((byte) VeinApi.XG_CMD_CLOSE_CONNECTION, null).generateBytes();
        send(bytes);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            running = false;
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            if (serialPort != null) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignore) {
                    }
                }
                log("release: serial close");
            }
        }, 200);
    }

    public void setEnrollCallback(EnrollCallback enrollCallback) {
        this.enrollCallback = enrollCallback;
    }

    public void setReadCallback(ReadCallback readCallback) {
        this.readCallback = readCallback;
    }

    private String bytes2string(byte[] bytes) {
        if (bytes == null) return "";
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] temp = new char[2];
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            temp[0] = hexArray[v >>> 4];
            temp[1] = hexArray[v & 0x0F];
            builder.append(temp);
            builder.append(" ");
        }
        return builder.toString();
    }

    private void log(String s) {
        if (showLog) {
            MyLog.d(TAG, s);
        }
    }

    private static class Packet {
        private static final byte[] head = {(byte) 0xBB, (byte) 0xAA};
        private byte address;
        private final byte cmd;
        private byte encode;
        private byte len;
        private final byte[] data;
        private byte[] sum;

        Packet(byte cmd, byte[] data) {
            this.cmd = cmd;
            this.data = data;
            if (data != null) {
                len = (byte) data.length;
            }
        }

        Packet(byte[] data) {
            address = data[2];
            cmd = data[3];
            encode = data[4];
            len = data[5];
            this.data = new byte[16];
            System.arraycopy(data, 6, this.data, 0, 16);
            sum = new byte[2];
            sum[0] = data[23];
            sum[1] = data[22];
        }

        byte[] generateBytes() {
            final byte[] bytes = new byte[24];
            bytes[0] = head[0];
            bytes[1] = head[1];
            bytes[2] = address;
            bytes[3] = cmd;
            bytes[4] = encode;
            int sum = (head[0] & 0x00FF) +
                    (head[1] & 0x00FF) +
                    (address & 0x00FF) +
                    (cmd & 0x00FF) +
                    (encode & 0x00FF);
            if (data != null) {
                bytes[5] = (byte) data.length;
                sum += data.length;
                System.arraycopy(data, 0, bytes, 6, data.length);
                for (byte b : data) {
                    sum += (b & 0x00FF);
                }
            }
            bytes[22] = (byte) (sum & 0xFF);
            bytes[23] = (byte) ((sum & 0xFF00) >> 8);
            return bytes;
        }

        boolean sumCheck() {
            int sum = (head[0] & 0x00FF) +
                    (head[1] & 0x00FF) +
                    (address & 0x00FF) +
                    (cmd & 0x00FF) +
                    (encode & 0x00FF) +
                    (len & 0x00FF);
            if (data != null) {
                for (byte b : data) {
                    sum += (b & 0x00FF);
                }
            }
            return (byte) ((sum & 0xFF00) >> 8) == this.sum[0] && (byte) (sum & 0xFF) == this.sum[1];
        }
    }

    private String getErrMsg(int errCode) {
        switch (errCode) {
            case VeinApi.XG_ERR_SUCCESS:
                return "操作成功";
            case VeinApi.XG_ERR_FAIL:
                return "操作失败";
            case VeinApi.XG_ERR_COM:
                return "通讯错误";
            case VeinApi.XG_ERR_DATA:
                return "数据校验错误";
            case VeinApi.XG_ERR_INVALID_PWD:
                return "密码错误";
            case VeinApi.XG_ERR_INVALID_PARAM:
                return "参数错误";
            case VeinApi.XG_ERR_INVALID_ID:
                return "ID错误";
            case VeinApi.XG_ERR_EMPTY_ID:
                return "指定ID为空";
            case VeinApi.XG_ERR_NOT_ENOUGH:
                return "空间不足";
            case VeinApi.XG_ERR_NO_SAME_FINGER:
                return "不是同一根手指";
            case VeinApi.XG_ERR_DUPLICATION_ID:
                return "ID已存在";
            case VeinApi.XG_ERR_TIME_OUT:
                return "等待超时";
            case VeinApi.XG_ERR_VERIFY:
                return "认证失败";
            case VeinApi.XG_ERR_NO_NULL_ID:
                return "无可用ID";
            case VeinApi.XG_ERR_BREAK_OFF:
                return "操作中断";
            case VeinApi.XG_ERR_NO_CONNECT:
                return "未连接";
            case VeinApi.XG_ERR_NO_SUPPORT:
                return "不支持此操作";
            case VeinApi.XG_ERR_NO_VEIN:
                return "无静脉数据";
            case VeinApi.XG_ERR_PACKET_PREFIX:
                return "格式错误";
            case VeinApi.XG_ERR_PACKET_CHECK:
                return "校验和错误";
            case VeinApi.XG_ERR_PACKET_LACK:
                return "数据不足";
            default:
                return "";
        }
    }
}
