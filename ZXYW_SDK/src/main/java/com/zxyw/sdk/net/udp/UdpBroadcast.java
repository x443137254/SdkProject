package com.zxyw.sdk.net.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UdpBroadcast {

    public void send(int port, byte[] data, UdpListener listener) {
        new Thread(() -> {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(data, 0, data.length, InetAddress.getByName("255.255.255.255"), port);
                DatagramSocket socket = new DatagramSocket();
                socket.send(datagramPacket);
                if (listener != null) {
                    byte[] buff = new byte[1024];
                    final DatagramPacket recPacket = new DatagramPacket(buff, buff.length);
                    socket.receive(recPacket);
                    listener.onReceive(Arrays.copyOfRange(recPacket.getData(), 0, recPacket.getLength()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
