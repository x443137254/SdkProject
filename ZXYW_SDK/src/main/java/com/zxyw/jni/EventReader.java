package com.zxyw.jni;

public class EventReader {
    static {
        System.loadLibrary("EventReader");
    }

    public native int KeyEventStart();

    public native int KeyEventStop();

    private static final EventReader instance = new EventReader();

    private boolean running;
    public String path = "/dev/input/event6";

    private EventReader() {
    }

    private EventCallback callback;

    public void receiveKeyEvent(int event_code, int event_value) {
        if (callback != null) {
            callback.onEvent(event_code, event_value);
        }
    }

    public static EventReader getInstance() {
        return instance;
    }

    public void setCallback(EventCallback callback) {
        this.callback = callback;
    }

    public void startListen() {
        if (running) return;
        new Thread(() -> {
            if (KeyEventStart() >= 0) {
                running = true;
            }
        }).start();
    }

    public void stopListen() {
        KeyEventStop();
        running = false;
    }
}
