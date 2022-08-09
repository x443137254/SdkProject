package com.zxyw.sdk.simpleFingerLib;

@Deprecated
public class SimpleFingerLib {
//    private final String TAG = getClass().getSimpleName();
//    private final boolean debug = true;
//    private OutputStream outputStream;
//    private BufferedInputStream inputStream;
//    private boolean open;
//    private boolean scanning;
//    private static SimpleFingerLib instance;
//    private ExecutorService service;
//    private FingerDetectListener fingerDetectListener;
//    private FingerEnrollListener fingerEnrollListener;
//    private FingerPressListener fingerPressListener;
//    private ErrorListener errorListener;
//    private Type type;
//    private byte[] buff;
//    private int index = 0;
//    private DeviceDelegate device;
//    private DetectStrategy detectStrategy;
//    private final Timer timer = new Timer();
//    private TimerTask closeFingerTask;
//
//    private boolean enable = true;
//    private boolean error = false;
//    private int cmdCount = 0;
//
//    private enum Type {DETECT, ENROLL}
//
//    public void setFingerDetectListener(FingerDetectListener fingerDetectListener) {
//        this.fingerDetectListener = fingerDetectListener;
//    }
//
//    public void setFingerEnrollListener(FingerEnrollListener fingerEnrollListener) {
//        this.fingerEnrollListener = fingerEnrollListener;
//    }
//
//    public void setFingerPressListener(FingerPressListener fingerPressListener) {
//        this.fingerPressListener = fingerPressListener;
//    }
//
//    public static SimpleFingerLib getInstance() {
//        if (instance == null) instance = new SimpleFingerLib();
//        return instance;
//    }
//
//    public void setDetectStrategy(DetectStrategy detectStrategy) {
//        this.detectStrategy = detectStrategy;
//    }
//
//    public void init(DeviceDelegate device) {
//        this.device = device;
//        service = Executors.newCachedThreadPool();
//        buff = new byte[1024];
//        open = false;
////        canSend = true;
//        scanning = false;
//    }
//
//    private SimpleFingerLib() {
//    }
//
//    public void detect() {
//        if (!enable) return;
//        index = 0;
//        service.execute(detectRunnable);
//    }
//
//    public void enroll() {
//        if (!enable) return;
//        index = 0;
//        service.execute(enrollRunnable);
//    }
//
//    public void setErrorListener(ErrorListener errorListener) {
//        this.errorListener = errorListener;
//    }
//
//    private void openFinger() {
//        if (!enable) return;
//        open = true;
//        device.openPower();
//        if (debug) Log.d(TAG, "打开指纹模块电源");
//        FingerDebugUtil.getInstance().debug("turn on finger power");
//        SystemClock.sleep(200);
//        outputStream = device.getOutputStream();
//        inputStream = device.getInputStream();
//        SystemClock.sleep(200);
//        service.execute(readThread);
//        index = 0;
////        if (type == Type.DETECT) send(CmdFactory.getData());
////        else send(CmdFactory.clear());
//        send(CmdFactory.cancel());
//    }
//
//    public void closeFinger() {
//        if (open) service.execute(closeRunnable);
//        stopTimer();
//    }
//
//    public boolean isEnable() {
//        return enable;
//    }
//
//    private void disable() {
//        if (error && errorListener != null) {
//            errorListener.error();
//        }
//        cmdCount = 0;
//        enable = false;
//        error = true;
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                enable = true;
//            }
//        }, 5000);
//    }
//
//    private final Runnable closeRunnable = new Runnable() {
//        @Override
//        public void run() {
////            SystemClock.sleep(100);
////            device.closeSerial();
//            SystemClock.sleep(100);
//            device.closePower();
//            if (debug) Log.d(TAG, "关闭指纹模块电源");
//            FingerDebugUtil.getInstance().debug("turn off finger power");
//            open = false;
////            canSend = true;
//        }
//    };
//
//    public boolean isScanning() {
//        return scanning;
//    }
//
//    private final Thread readThread = new Thread() {
//        @Override
//        public void run() {
//            try {
//                //noinspection ResultOfMethodCallIgnored
//                inputStream.skip(inputStream.available());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            byte[] buff = new byte[1024 * 10];
//            while (open) {
//                int len = 0;
//                if (inputStream == null) break;
//                try {
//                    len = inputStream.read(buff);
//                } catch (IOException ignore) {
//                }
//                if (len > 1000) {
//                    index = 0;
//                } else if (len > 0) {
//                    try {
//                        System.arraycopy(buff, 0, SimpleFingerLib.this.buff, index, len);
//                        index += len;
//                        String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, 0, len));
//                        if (debug) {
//                            Log.d(TAG, "receive：index=" + index + " <<--" + s +
//                                    "\n-----------------------------------------------------------------------------------------------------------------------------------------");
//                        }
//                        FingerDebugUtil.getInstance().debug("receive data(index=" + index + ")：" + s);
//                    } catch (Exception ignore) {
//                        continue;
//                    }
//                    cmdCount = 0;
//                    error = false;
//                    stopTimer();
//                    parse();
//                }
//            }
//        }
//    };
//
//    private final Thread checkFingerThread = new Thread() {
//        @Override
//        public void run() {
//            if (fingerPressListener == null) return;
//            SystemClock.sleep(1000);
//            if (debug) Log.d(TAG, "开始检测手指按压");
//            FingerDebugUtil.getInstance().debug("check finger's press start");
//            while (scanning) {
//                while (!enable) {
//                    SystemClock.sleep(1000);
//                }
//                SystemClock.sleep(300);
//                if (detectStrategy.onPress()) {
//                    if (fingerPressListener.onFingerPress()) {
//                        simulatePress();
//                    }
//                    SystemClock.sleep(1500);
//                }
//            }
//            FingerDebugUtil.getInstance().debug("check finger's press stop");
//        }
//    };
//
//    public void simulatePress() {
//        if (!enable) return;
//        detect();
//    }
//
//    private void startTimer() {
//        if (closeFingerTask == null) {
//            closeFingerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    if (cmdCount > 2) {
//                        disable();
//                    }
//                    closeFinger();
//                }
//            };
//            try {
//                timer.schedule(closeFingerTask, 5000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void stopTimer() {
//        if (closeFingerTask != null) {
//            closeFingerTask.cancel();
//            closeFingerTask = null;
//        }
//    }
//
//    public void startFingerCheck() {
//        FingerDebugUtil.getInstance().debug("try to start finger press check");
//        if (detectStrategy != null && !scanning) {
//            scanning = true;
//            service.execute(checkFingerThread);
//        }
//    }
//
//    public void stopFingerCheck() {
//        FingerDebugUtil.getInstance().debug("try to stop finger press check");
//        scanning = false;
//        checkFingerThread.interrupt();
//        scanning = false;
//    }
//
//    private void parse() {
//        int offset = -1;
//        for (int i = 0; i < index; i++) {
//            if (buff[i] == PacketType.RESPONSE[0] && buff[i + 1] == PacketType.RESPONSE[1]) {
//                offset = i;
//                break;
//            }
//        }
//        if (offset < 0) return;
//        if (index < offset + 24) return;
//        if (buff[offset + 2] == Command.GET_DATA[0] && buff[offset + 3] == Command.GET_DATA[1]) {//获取指纹模板指令
//            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，校验后接口返回模板数据，继续取指纹模板
//                if (index < offset + 604) return;
//                byte[] sum = FingerTools.sum(buff, offset + 24, offset + 602);
//                if (sum[0] == buff[offset + 602] && sum[1] == buff[offset + 603]) {
//                    if (fingerDetectListener != null) {
//                        fingerDetectListener.getFingerTemplate(Arrays.copyOfRange(buff, offset + 32, offset + 602));
//                    }
//                }
//            } else {//失败，重新发送获取指纹模板指令
//                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
//                if (debug) {
//                    Log.e(TAG, "获取按压指纹数据失败：" + s);
//                }
//                FingerDebugUtil.getInstance().debug("get press finger data failed：" + s);
//                if (fingerDetectListener != null) {
//                    fingerDetectListener.getFingerTemplate(null);
//                }
//            }
//            detect();
//        } else if (buff[offset + 2] == Command.CANCEL[0] && buff[offset + 3] == Command.CANCEL[1]) {//取消指令
//            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，根据当前执行的命令发送指令
//                if (type == Type.DETECT) detect();
//                else deleteAll();
//            } else {//失败，无法取消就断电重启
//                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
//                if (debug) {
//                    Log.e(TAG, "取消指令发送失败：" + s);
//                }
//                FingerDebugUtil.getInstance().debug("cancel failed：" + s);
//                reboot();
//            }
//        } else if (buff[offset + 2] == Command.CLEAR[0] && buff[offset + 3] == Command.CLEAR[1]) {//清除记录
//            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，开始录入指纹
//                enroll();
//            } else {//失败，断电重启
//                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
//                if (debug) {
//                    Log.e(TAG, "清除记录指令发送失败：" + s);
//                }
//                FingerDebugUtil.getInstance().debug("clear all failed：" + s);
//                reboot();
//            }
//        } else if (buff[offset + 2] == Command.ENROLL[0] && buff[offset + 3] == Command.ENROLL[1]) {//录入指纹
//            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，解析返回值
//                if (fingerEnrollListener == null) return;
//                if (buff[offset + 8] == (byte) 0xF1 && buff[offset + 9] == (byte) 0xFF) {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP1, null);
//                    index = 0;
//                } else if (buff[offset + 8] == (byte) 0xF2 && buff[offset + 9] == (byte) 0xFF) {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP2, null);
//                    index = 0;
//                } else if (buff[offset + 8] == (byte) 0xF3 && buff[offset + 9] == (byte) 0xFF) {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.STEP3, null);
//                    index = 0;
//                } else if (buff[offset + 8] == (byte) 0xF4 && buff[offset + 9] == (byte) 0xFF) {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.RELEASE, null);
//                    index = 0;
//                } else if (buff[offset + 8] == (byte) 0x01 && buff[offset + 9] == (byte) 0x00) {
//                    getEnrollTemplate();
//                } else {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR_RETRY, null);
//                    closeFinger();
//                }
//            } else {//失败，关闭电源
//                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
//                if (debug) {
//                    Log.e(TAG, "录入失败：" + s);
//                }
//                FingerDebugUtil.getInstance().debug("enroll failed：" + s);
//                fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR, null);
//                closeFinger();
//            }
//        } else if (buff[offset + 2] == Command.READ[0] && buff[offset + 3] == Command.READ[1]) {//读取录入的指纹模板数据
//            if ((buff[offset + 6] + buff[offset + 7]) == 0) {//成功，校验后接口返回模板数据，继续取指纹模板
//                if (index < offset + 604) return;
//                byte[] sum = FingerTools.sum(buff, offset + 24, offset + index - 2);
//                if (sum[0] == buff[offset + index - 2] && sum[1] == buff[offset + index - 1]) {
//                    if (fingerEnrollListener != null) {
//                        fingerEnrollListener.enrollStep(FingerEnrollListener.Step.DONE,
//                                Arrays.copyOfRange(buff, offset + index - 572, offset + index - 2));
//                    }
//                }
//            } else {//失败，重新发送获取指纹模板指令
//                String s = FingerTools.bytes2string(Arrays.copyOfRange(buff, offset + 6, offset + 8));
//                if (debug) {
//                    Log.e(TAG, "读取录入指纹模板失败：" + s);
//                }
//                FingerDebugUtil.getInstance().debug("read template failed：" + s);
//                if (fingerEnrollListener != null) {
//                    fingerEnrollListener.enrollStep(FingerEnrollListener.Step.ERROR, null);
//                }
//            }
//            closeFinger();
//        }
//    }
//
//    private void getEnrollTemplate() {
//        if (debug) Log.d(TAG, "发送获取录入的指纹模板指令");
//        FingerDebugUtil.getInstance().debug("send cmd : read enrolled template");
//        index = 0;
//        service.execute(new Runnable() {
//            @Override
//            public void run() {
//                SimpleFingerLib.this.send(CmdFactory.read());
//            }
//        });
//    }
//
//    private void deleteAll() {
//        if (debug) Log.d(TAG, "发送删除指令");
//        FingerDebugUtil.getInstance().debug("send cmd : clear all");
//        index = 0;
//        service.execute(new Runnable() {
//            @Override
//            public void run() {
//                SimpleFingerLib.this.send(CmdFactory.clear());
//            }
//        });
//    }
//
//    public void reboot() {
//        closeFinger();
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                openFinger();
//            }
//        }, 500);
//    }
//
//    private final Runnable detectRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if (!open) {
//                type = Type.DETECT;
//                SimpleFingerLib.this.openFinger();
//            } else if (type == Type.DETECT) {
//                if (debug) Log.d(TAG, "发送读取按压指纹指令");
//                FingerDebugUtil.getInstance().debug("send cmd : read press finger data");
//                SimpleFingerLib.this.send(CmdFactory.getData());
//            } else {
//                type = Type.DETECT;
//                if (debug) Log.d(TAG, "发送取消指令");
//                FingerDebugUtil.getInstance().debug("send cmd : cancel");
//                SimpleFingerLib.this.send(CmdFactory.cancel());
//            }
//        }
//    };
//
//    private final Runnable enrollRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if (!open) {
//                type = Type.ENROLL;
//                SimpleFingerLib.this.openFinger();
//            } else if (type == Type.ENROLL) {
//                if (debug) Log.d(TAG, "发送录入指令");
//                FingerDebugUtil.getInstance().debug("send cmd : enroll");
//                SimpleFingerLib.this.send(CmdFactory.enroll());
//            } else {
//                type = Type.ENROLL;
//                if (debug) Log.d(TAG, "发送取消指令");
//                FingerDebugUtil.getInstance().debug("send cmd : cancel");
//                SimpleFingerLib.this.send(CmdFactory.cancel());
//            }
//        }
//    };
//
//    private synchronized void send(final byte[] data) {
//        String s = FingerTools.bytes2string(data);
//        if (debug) {
//            Log.d(TAG, "send：-->> " + s +
//                    "\n-----------------------------------------------------------------------------------------------------------------------------------------");
//        }
//        FingerDebugUtil.getInstance().debug("the data really to send：" + s);
//        try {
//            if (outputStream != null) {
//                outputStream.write(data);
//                outputStream.flush();
//            }
//            cmdCount++;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (type == Type.DETECT) {
//            startTimer();
//        }
//    }
}
