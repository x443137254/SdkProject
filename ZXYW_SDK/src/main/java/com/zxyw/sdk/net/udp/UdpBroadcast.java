package com.zxyw.sdk.net.udp;

import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * udp广播类
 * 5s发送一次广播消息，循环监听，使用发送端口或者指定端口循环接收数据
 */
public class UdpBroadcast {
    private UdpListener listener;
    private DatagramPacket datagramPacket;
    private int receivePort;//接收端口，不指定的话就使用发送的端口接收
    private final ExecutorService threadPool;
    private final Handler handler;
    private Runnable sendRunnable;//发送广播线程，定时5s循环发送
    private Runnable receiveRunnable;//接收广播线程
    private DatagramSocket receiveSocket;

    public UdpBroadcast() {
        threadPool = Executors.newCachedThreadPool();
        HandlerThread thread = new HandlerThread("UdpBroadcast");
        thread.start();
        handler = new Handler(thread.getLooper());

        //初始化接收线程
        receiveRunnable = () -> {
            if (receiveSocket == null || receiveSocket.isClosed()) {
                return;
            }
            DatagramPacket recPacket;
            byte[] buff = new byte[1000];
            while (true) {
                try {
                    recPacket = new DatagramPacket(buff, buff.length);
                    receiveSocket.receive(recPacket);
                    if (listener != null) {
                        byte[] response = listener.onReceive(Arrays.copyOfRange(recPacket.getData(), 0, recPacket.getLength()));
                        if (response != null && response.length > 0) {
                            receiveSocket.send(new DatagramPacket(response, response.length,
                                    new InetSocketAddress(recPacket.getAddress(), recPacket.getPort())));
                        }
                    }
                } catch (Exception ignore) {
                    if (receiveSocket != null) {
                        receiveSocket.close();
                    }
                    receiveSocket = null;
                    return;
                }
            }
        };

        //初始化循环发送广播线程
        sendRunnable = () -> {
            if (datagramPacket == null) return;
            if (receiveSocket != null) {
                receiveSocket.close();
            }
            try {
                DatagramSocket socket = new DatagramSocket();
                if (receivePort == 0) {//没有指定接收端口，则使用发送端口接受数据
                    receiveSocket = socket;
                }
                socket.send(datagramPacket);
                startReceive();
            } catch (IOException ignore) {
            }
            handler.postDelayed(sendRunnable, 10000);
        };
    }

    /**
     * 开始接收UDP广播
     */
    public void startReceive() {
        threadPool.execute(receiveRunnable);
    }

    /**
     * 指定接收广播端口号
     *
     * @param receivePort 端口号
     */
    public void setReceivePort(int receivePort) {
        this.receivePort = receivePort;
        try {
            receiveSocket = new DatagramSocket(receivePort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * 循环发送广播
     *
     * @param port 目标端口
     * @param data 广播数据
     */
    public void send(int port, byte[] data) {
        try {
            datagramPacket = new DatagramPacket(data, 0, data.length, InetAddress.getByName("255.255.255.255"), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        handler.post(sendRunnable);
    }

    public void setListener(UdpListener listener) {
        this.listener = listener;
    }

    public void exit() {
        if (receiveSocket != null) {
            receiveSocket.close();
        }
        handler.removeCallbacksAndMessages(null);
        threadPool.shutdown();
    }
}
