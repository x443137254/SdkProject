package com.zxyw.sdk.net.nettyTCP;

import io.netty.channel.Channel;

public class SendStrategy {
    private Channel channel;
    private Packet heartBeat;
    private Packet lastMessage;
    private boolean keepAlive = true;

    public Packet getHeartBeat() {
        return heartBeat;
    }

    public void setLastMessage(Packet lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Packet getLastMessage() {
        return lastMessage;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setHeartBeat(Packet heartBeat) {
        this.heartBeat = heartBeat;
    }

    public void timeOut() {
        if (lastMessage != null &&
                lastMessage.isNeedResponse() &&
                lastMessage.getMaxTryTimes() > lastMessage.getTryTimes()) {
            lastMessage.setTryTimes(lastMessage.getTryTimes() + 1);
            send(lastMessage);
        } else sendHeartBeat();
    }

    private void sendHeartBeat() {
        send(heartBeat);
    }

    public void send(Packet packet) {
        if (channel == null || !channel.isActive() || packet == null) return;
        Object data = packet.getData();
        if (data != null) {
            lastMessage = packet;
            data = transform(data);
            channel.writeAndFlush(data);
        }
    }

    public Object transform(Object o) {
        return o;
    }
}
