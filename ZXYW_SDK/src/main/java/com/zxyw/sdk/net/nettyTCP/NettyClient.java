package com.zxyw.sdk.net.nettyTCP;

import android.os.SystemClock;
import android.util.Log;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

public class NettyClient implements _NetListener {
    private final String TAG = this.getClass().getSimpleName();
    private EventLoopGroup group;
    private boolean isConnect = false;
    private boolean reConnect = true;
    private boolean keepAlive = true;
    private int readIdleTime;
    private int writeIdleTime;
    private int allIdleTime;
    private SocketAddress remoteAddress;
    private SocketAddress localAddress;
    private byte[] decodeWord;
    private NettyListener nettyListener;
    private Object heartBeat;
    private SendStrategy sendStrategy;

    public NettyListener getNettyListener() {
        return nettyListener;
    }

    public void bind(SocketAddress localAddress){
        this.localAddress = localAddress;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setNettyListener(NettyListener nettyListener) {
        this.nettyListener = nettyListener;
    }

    public void setHeartBeat(Object heartBeat) {
        this.heartBeat = heartBeat;
    }

    public SendStrategy getSendStrategy() {
        return sendStrategy;
    }

    public void setSendStrategy(SendStrategy sendStrategy) {
        this.sendStrategy = sendStrategy;
    }

    public void setReadIdleTime(int readIdleTime) {
        this.readIdleTime = readIdleTime;
    }

    public void setWriteIdleTime(int writeIdleTime) {
        this.writeIdleTime = writeIdleTime;
    }

    public void setAllIdleTime(int allIdleTime) {
        this.allIdleTime = allIdleTime;
    }

    public NettyClient(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        readIdleTime = 5;
        writeIdleTime = 5;
        allIdleTime = 5;
    }

    public void setReConnect(boolean reConnect) {
        this.reConnect = reConnect;
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setDecodeWord(byte[] decodeWord) {
        this.decodeWord = decodeWord;
    }

    public void closeClient() {
        if (group != null) {
            group.shutdownGracefully();
        }
        if (reConnect) {
            connect();
        }
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void connect() {
        new Thread(() -> {
            try {
                group = new NioEventLoopGroup();
                final Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group);
                bootstrap.option(ChannelOption.TCP_NODELAY, true);
                bootstrap.channel(NioSocketChannel.class);
                if (localAddress != null){
                    bootstrap.localAddress(localAddress);
                }
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleStateHandler(readIdleTime, writeIdleTime, allIdleTime, TimeUnit.SECONDS));
                        if (decodeWord != null) {
                            pipeline.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, true, Unpooled.copiedBuffer(decodeWord)));
                        }
                        pipeline.addLast(new NetHandler(NettyClient.this));
                        pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
                        pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                    }
                });
                bootstrap.connect(remoteAddress).addListener((ChannelFutureListener) future -> {
                    isConnect = future.isSuccess();
                    if (!isConnect && reConnect) {
                        future.channel().close();
                        SystemClock.sleep(1000);
                        closeClient();
                    }
                }).sync();
            } catch (Exception | Error e) {
                try {
                    Log.e(TAG, e.getMessage());
                } catch (Exception ignore) {

                }
            }
        }).start();
    }

    public void sendResponseMessage(Object message) {
        Packet packet = new Packet();
        packet.setNeedResponse(true);
        packet.setTryTimes(10);
        send(packet);
    }

    public void sendNoResponseMessage(Object message) {
        Packet packet = new Packet();
        packet.setNeedResponse(false);
        send(packet);
    }

    public void send(Packet packet) {
        if (sendStrategy != null) sendStrategy.send(packet);
    }

    public void retry() {
        if (sendStrategy != null) sendStrategy.timeOut();
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
        Log.d(TAG, remoteAddress.toString() + "已连接");
        if (sendStrategy == null) sendStrategy = new SendStrategy();
        sendStrategy.setChannel(ctx.channel());
        sendStrategy.setKeepAlive(keepAlive);
        if (heartBeat != null) {
            Packet packet = new Packet();
            packet.setData(heartBeat);
            packet.setNeedResponse(false);
            sendStrategy.setHeartBeat(packet);
        }
        if (nettyListener != null) nettyListener.onConnect(ctx);
    }

    @Override
    public void inActive(ChannelHandlerContext ctx) {
        Log.d(TAG, remoteAddress.toString() + "已断开");
        if (nettyListener != null) nettyListener.onDisconnect(ctx);
        ctx.channel().close();
        sendStrategy.setChannel(null);
        closeClient();
    }

    @Override
    public void receive(ChannelHandlerContext ctx, byte[] message) {
        if (nettyListener != null) nettyListener.onReceive(ctx, message);
    }

    @Override
    public void readIdle(ChannelHandlerContext ctx) {
        if (nettyListener != null) nettyListener.onTimeOut(IdleState.READER_IDLE, ctx);
    }

    @Override
    public void writeIdle(ChannelHandlerContext ctx) {
        if (nettyListener != null) nettyListener.onTimeOut(IdleState.WRITER_IDLE, ctx);
    }

    @Override
    public void allIdle(ChannelHandlerContext ctx) {
        if (nettyListener != null) nettyListener.onTimeOut(IdleState.ALL_IDLE, ctx);
    }
}
