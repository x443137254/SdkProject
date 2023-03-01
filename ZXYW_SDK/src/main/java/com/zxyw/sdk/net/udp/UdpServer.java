package com.zxyw.sdk.net.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpServer {
    private UdpListener listener;
    private ExecutorService service;
    private DatagramSocket socket;
    private boolean start;

    public UdpServer(int port) {
        service = Executors.newFixedThreadPool(2);
        start = false;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public boolean isStart() {
        return start;
    }

    public UdpServer setListener(UdpListener listener) {
        this.listener = listener;
        return this;
    }

    public void start() {
        if (start) return;
        service.execute(() -> {
            start = true;
            byte[] buff = new byte[1024 * 100];
            while (start) {
                try {
                    if (socket == null) break;
                    DatagramPacket recPacket = new DatagramPacket(buff, buff.length);
                    socket.receive(recPacket);
                    if (listener != null) {
                        byte[] response = listener.onReceive(Arrays.copyOfRange(recPacket.getData(), 0, recPacket.getLength()));
                        if (response != null) {
                            socket.send(new DatagramPacket(response, response.length,
                                    new InetSocketAddress(recPacket.getAddress(), recPacket.getPort())));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            start = false;
        });
    }

    public void stop() {
        start = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }
}
