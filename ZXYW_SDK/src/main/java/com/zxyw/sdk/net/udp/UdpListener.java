package com.zxyw.sdk.net.udp;

public interface UdpListener {
    byte[] onReceive(byte[] data);
}
