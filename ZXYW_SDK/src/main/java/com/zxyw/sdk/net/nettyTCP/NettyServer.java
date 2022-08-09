package com.zxyw.sdk.net.nettyTCP;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

public class NettyServer implements _NetListener {
    private final String TAG = "NettyServer";
    private final int port;
    private EventLoopGroup workGroup;
    private byte[] unpackBytes;
    private int readIdleTime;
    private int writeIdleTime;
    private int allIdleTime;
    private final List<Channel> channelList;
    final private List<SendStrategy> senderList;
    private Object heartBeat;
    private NettyListener nettyListener;
    private SocketAddress localAddress;
    private final List<BlackClient> blacklist;
    private Class<? extends SendStrategy> strategyClass;

    public NettyListener getNettyListener() {
        return nettyListener;
    }

    public List<Channel> getChannelList() {
        return channelList;
    }

    public void setSendStrategy(Class<? extends SendStrategy> strategyClass) {
        this.strategyClass = strategyClass;
    }

    public void setNettyListener(NettyListener nettyListener) {
        this.nettyListener = nettyListener;
    }

    public void setHeartBeat(Object heartBeat) {
        this.heartBeat = heartBeat;
    }

    public void setUnpackBytes(byte[] unpackBytes) {
        this.unpackBytes = unpackBytes;
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

    public void bind(SocketAddress localAddress){
        this.localAddress = localAddress;
    }

    public void setUnpackString(String unpackString) {
        if (unpackString == null) this.unpackBytes = null;
        else this.unpackBytes = unpackString.getBytes();
    }

    public NettyServer(int port) {
        this.port = port;
        readIdleTime = 5;
        writeIdleTime = 0;
        allIdleTime = 0;
        channelList = new ArrayList<>();
        senderList = new ArrayList<>();
        blacklist = new ArrayList<>();
    }

    public void start() {
        new Thread(() -> {
            workGroup = new NioEventLoopGroup();
            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            if (localAddress != null) {
                bootstrap.localAddress(localAddress);
            }
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("timeout", new IdleStateHandler(readIdleTime, writeIdleTime, allIdleTime, TimeUnit.SECONDS));
                    if (unpackBytes != null) {
                        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Unpooled.copiedBuffer(unpackBytes)));
                    }
                    pipeline.addLast("handler", new NetHandler(NettyServer.this));
                    pipeline.addLast("decoder", new StringEncoder(CharsetUtil.UTF_8));
                    pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
                    pipeline.addLast("firewall", new RuleBasedIpFilter(new IpFilterRule() {
                        @Override
                        public boolean matches(InetSocketAddress address) {
                            for (int i = 0; i < blacklist.size(); i++) {
                                BlackClient client = blacklist.get(i);
                                if (client.getAddress().equals(address.getAddress())) {
                                    if ((System.currentTimeMillis() - client.getFirstWarmTime()) > client.getReleaseTime()) {
                                        blacklist.remove(client);
                                        return false;
                                    } else
                                        return client.getWarmTimes() >= client.getDeadLine();
                                }
                            }
                            return false;
                        }

                        @Override
                        public IpFilterRuleType ruleType() {
                            return IpFilterRuleType.REJECT;
                        }
                    }));
                }
            });
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            try {
                ChannelFuture future = bootstrap.bind(port).sync();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void shotDown() {
        for (Channel channel : channelList) {
            if (channel != null) channel.close();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
    }

    public void authClient(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        channelList.add(channel);
        SendStrategy strategy;
        if (strategyClass != null) {
            try {
                strategy = strategyClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                strategy = new SendStrategy();
            }
        } else {
            strategy = new SendStrategy();
        }
        strategy.setChannel(channel);
        if (heartBeat != null) {
            Packet packet = new Packet();
            packet.setData(heartBeat);
            packet.setNeedResponse(false);
            strategy.setHeartBeat(packet);
        }
        synchronized (senderList) {
            senderList.add(strategy);
        }
    }

    @Deprecated
    public void doTimeOut() {
        try {
            synchronized (senderList) {
                for (SendStrategy sendStrategy : senderList) {
                    sendStrategy.timeOut();
                }
            }
        } catch (Exception ignore) {

        }
    }

    public void warnClient(Channel channel) {
        if (channel == null) return;
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        InetAddress inetAddress = address.getAddress();
        int size = blacklist.size();
        boolean contain = false;
        BlackClient client;
        for (int i = 0; i < size; i++) {
            client = blacklist.get(i);
            if (client.getAddress().equals(inetAddress)) {
                client.addWarmTimes();
                contain = true;
                break;
            }
        }
        if (!contain) {
            client = new BlackClient(System.currentTimeMillis(), inetAddress);
            blacklist.add(client);
        }
        channel.close();
    }

    public void sendResponseMessage(Object message) {
        Packet packet = new Packet();
        packet.setNeedResponse(true);
        packet.setTryTimes(10);
        sendToAllClient(packet);
    }

    public void sendNoResponseMessage(Object message) {
        Packet packet = new Packet();
        packet.setNeedResponse(false);
        sendToAllClient(packet);
    }

    public void sendToAllClient(Packet packet) {
        try {
            synchronized (senderList) {
                for (SendStrategy sendStrategy : senderList) {
                    if (sendStrategy != null) {
                        sendStrategy.send(packet);
                    }
                }
            }
        } catch (Exception ignore) {

        }
    }

    @Override
    public void active(ChannelHandlerContext ctx) {
        if (nettyListener != null) nettyListener.onConnect(ctx);
    }

    @Override
    public void inActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        channelList.remove(channel);
        synchronized (senderList) {
            for (SendStrategy ss : senderList) {
                if (ss.getChannel().equals(channel)) {
                    senderList.remove(ss);
                    break;
                }
            }
        }
        if (nettyListener != null) nettyListener.onDisconnect(ctx);
        ctx.close();
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

    public class BlackClient {
        private final long firstWarmTime;
        private final long releaseTime = 600000;
        private final InetAddress address;
        private int warmTimes;
        private final int deadLine = 5;

        public BlackClient(long firstWarmTime, InetAddress address) {
            this.firstWarmTime = firstWarmTime;
            this.address = address;
            this.warmTimes = 1;
        }

        public void addWarmTimes() {
            this.warmTimes++;
        }

        public long getFirstWarmTime() {
            return firstWarmTime;
        }

        public long getReleaseTime() {
            return releaseTime;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getWarmTimes() {
            return warmTimes;
        }

        public int getDeadLine() {
            return deadLine;
        }
    }
}
