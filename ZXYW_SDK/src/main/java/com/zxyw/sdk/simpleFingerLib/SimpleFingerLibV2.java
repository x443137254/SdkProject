package com.zxyw.sdk.simpleFingerLib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.zxyw.sdk.tools.MyLog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 除指纹录入外不再管理指纹模块电源，通过监听F12事件发送读取指纹模板指令
 */
public class SimpleFingerLibV2 {
    private final String TAG = "SimpleFingerLibV2";
    private static SimpleFingerLibV2 instance;
    private FingerDetectListener fingerDetectListener;
    private FingerEnrollListener fingerEnrollListener;
    private ActionType type = ActionType.DETECT;
    private final byte[] buff;
    private int index = 0;
    private DeviceDelegate device;
    private final Handler handler;

    private enum ActionType {DETECT, ENROLL, CLOSE}

    public void setFingerDetectListener(FingerDetectListener fingerDetectListener) {
        this.fingerDetectListener = fingerDetectListener;
    }

    public void setFingerEnrollListener(FingerEnrollListener fingerEnrollListener) {
        this.fingerEnrollListener = fingerEnrollListener;
    }

    public static SimpleFingerLibV2 getInstance() {
        if (instance == null) instance = new SimpleFingerLibV2();
        return instance;
    }

    public void init(DeviceDelegate device) {
        this.device = device;
    }

    private SimpleFingerLibV2() {
        HandlerThread thread = new HandlerThread("SimpleFingerLib");
        thread.start();
        handler = new Handler(thread.getLooper());
        buff = new byte[1024];
        new Thread(() -> {
            while (device == null || device.getSerialPort() == null) {
                SystemClock.sleep(1000);
            }
            byte[] buff = new byte[1024 * 10];
            while (true) {
                int len = 0;
                try {
                    len = device.getSerialPort().getInputStream().read(buff);
                } catch (IOException ignore) {
                }
                if ((len == 1 && buff[0] == 0) || len > 1000) {
                    index = 0;
                } else if (len > 0) {
                    try {
                        System.arraycopy(buff, 0, SimpleFingerLibV2.this.buff, index, len);
                        index += len;
                    } catch (Exception ignore) {
                        index = 0;
                    }
                }
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, 0, len));
                MyLog.d(TAG, "receive data(index=" + index + "): " + s);
                if (index > 0) {
                    parse();
                }
            }
        }).start();
    }

    public void detect() {
        index = 0;
        type = ActionType.DETECT;
        openFingerAfterCancel();
    }

    public void enroll() {
        index = 0;
        type = ActionType.ENROLL;
        device.closePower();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::openFingerAfterCancel, 100);
    }

    private void openFingerAfterCancel() {
        device.openPower();
        MyLog.d(TAG, "turn on finger power");
        SystemClock.sleep(200);
        index = 0;
        send(CmdFactory.cancel());
    }


    public void closeFingerAfterCancel() {
        index = 0;
        type = ActionType.CLOSE;
        send(CmdFactory.cancel());
    }

    public void closeFingerDirectly() {
        device.closePower();
    }

    private void parse() {
        int offset = -1;
        for (int i = 0; i < index; i++) {
            if (buff[i] == PacketType.RESPONSE[0] && buff[i + 1] == PacketType.RESPONSE[1]) {
                offset = i;
                break;
            }
        }
        if (offset < 0) return;
        if (index < offset + 24) return;
        if (buff[offset + 2] == Command.GET_DATA[0] && buff[offset + 3] == Command.GET_DATA[1]) {//获取指纹模板指令
            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，校验后接口返回模板数据，继续取指纹模板
                if (index < offset + 604) return;
                byte[] sum = FingerTools.sum(buff, offset + 24, offset + 602);
                if (sum[0] == buff[offset + 602] && sum[1] == buff[offset + 603]) {
                    if (fingerDetectListener != null) {
                        fingerDetectListener.getFingerTemplate(Arrays.copyOfRange(buff, offset + 32, offset + 602));
                    }
                }
            } else {//失败，重新发送获取指纹模板指令
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
                MyLog.d(TAG, "get press finger data failed：" + s);
                if (fingerDetectListener != null) {
                    fingerDetectListener.getFingerTemplate(null);
                }
            }
            detect();
        } else if (buff[offset + 2] == Command.CANCEL[0] && buff[offset + 3] == Command.CANCEL[1]) {//取消指令
            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，根据当前执行的命令发送指令
                if (type == ActionType.DETECT) {
                    index = 0;
                    send(CmdFactory.getData());
                } else if (type == ActionType.ENROLL) {
                    deleteAll();
                } else if (device != null) {
                    device.closePower();
                    MyLog.d(TAG, "turn off finger power");
                }
            } else {//失败，无法取消就断电
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
                MyLog.d(TAG, "cancel failed：" + s);
                closeFingerAfterCancel();
            }
        } else if (buff[offset + 2] == Command.CLEAR[0] && buff[offset + 3] == Command.CLEAR[1]) {//清除记录
            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，开始录入指纹
                index = 0;
                send(CmdFactory.enroll());
            } else {//失败，断电
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
                MyLog.d(TAG, "clear all failed：" + s);
                closeFingerAfterCancel();
            }
        } else if (buff[offset + 2] == Command.ENROLL[0] && buff[offset + 3] == Command.ENROLL[1]) {//录入指纹
            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，解析返回值
                if (fingerEnrollListener == null) return;
                if (buff[offset + 8] == (byte) 0xF1 && buff[offset + 9] == (byte) 0xFF) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP1, null);
                    index = 0;
                } else if (buff[offset + 8] == (byte) 0xF2 && buff[offset + 9] == (byte) 0xFF) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP2, null);
                    index = 0;
                } else if (buff[offset + 8] == (byte) 0xF3 && buff[offset + 9] == (byte) 0xFF) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP3, null);
                    index = 0;
                } else if (buff[offset + 8] == (byte) 0xF4 && buff[offset + 9] == (byte) 0xFF) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.RELEASE, null);
                    index = 0;
                } else if (buff[offset + 8] == (byte) 0x01 && buff[offset + 9] == (byte) 0x00) {
                    getEnrollTemplate();
                } else {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR_RETRY, null);
                    closeFingerAfterCancel();
                }
            } else {//失败，关闭电源
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
                MyLog.d(TAG, "enroll failed：" + s);
                if (fingerEnrollListener != null) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR, null);
                }
                closeFingerAfterCancel();
            }
        } else if (buff[offset + 2] == Command.READ[0] && buff[offset + 3] == Command.READ[1]) {//读取录入的指纹模板数据
            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，校验后接口返回模板数据，继续取指纹模板
                if (index < offset + 604) return;
                byte[] sum = FingerTools.sum(buff, offset + 24, offset + index - 2);
                if (sum[0] == buff[offset + index - 2] && sum[1] == buff[offset + index - 1]) {
                    if (fingerEnrollListener != null) {
                        fingerEnrollListener.enrollStep(FingerEnrollListener.Step.DONE,
                                Arrays.copyOfRange(buff, offset + index - 572, offset + index - 2));
                    }
                }
            } else {//失败，重新发送获取指纹模板指令
                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
                MyLog.d(TAG, "read template failed：" + s);
                if (fingerEnrollListener != null) {
                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR, null);
                }
            }
            closeFingerAfterCancel();
        }
    }

    private void getEnrollTemplate() {
        MyLog.d(TAG, "send cmd : read enrolled template");
        index = 0;
        send(CmdFactory.read());
    }

    private void deleteAll() {
        MyLog.d(TAG, "send cmd : clear all");
        index = 0;
        send(CmdFactory.clear());
    }

    private synchronized void send(final byte[] data) {
        if (device == null || device.getSerialPort() == null) return;
        handler.post(() -> {
            String s = FingerTools.bytes2string(data);
            MyLog.d(TAG, "the data really to send：" + s);
            try {
                final OutputStream outputStream = device.getSerialPort().getOutputStream();
                if (outputStream != null) {
                    outputStream.write(data);
                    outputStream.flush();
                } else {
                    MyLog.d(TAG, "outputStream is null");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
