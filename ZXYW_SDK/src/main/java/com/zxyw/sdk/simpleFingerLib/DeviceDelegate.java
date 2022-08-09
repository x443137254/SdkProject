package com.zxyw.sdk.simpleFingerLib;

import android_serialport_api.SerialPort;

public interface DeviceDelegate {
    void openPower();
    void closePower();
    SerialPort getSerialPort();
}
