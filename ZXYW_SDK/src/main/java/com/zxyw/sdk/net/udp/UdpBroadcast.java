package com.zxyw.sdk.net.udp;

import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UdpBroadcast {
    private final Thread receiveThread;
    private DatagramSocket socket;
    private boolean running;
    private final Handler handler;

    public UdpBroadcast(UdpListener listener) {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        running = true;
        receiveThread = new Thread(() -> {
            if (listener != null) {
                final byte[] buff = new byte[1024];
                DatagramPacket recPacket;
                while (running) {
                    recPacket = new DatagramPacket(buff, buff.length);
                    try {
                        socket.receive(recPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    listener.onReceive(Arrays.copyOfRange(recPacket.getData(), 0, recPacket.getLength()));
                }
            }
        });
        receiveThread.start();
        final HandlerThread thread = new HandlerThread("");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public void send(int port, byte[] data) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    try {
                        socket.send(new DatagramPacket(data, 0, data.length, InetAddress.getByName("255.255.255.255"), port));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(this, 5000);
            }
        });
    }

    public void stopBroadcast() {
        handler.removeCallbacksAndMessages(null);
        running = false;
        receiveThread.interrupt();
    }
}
