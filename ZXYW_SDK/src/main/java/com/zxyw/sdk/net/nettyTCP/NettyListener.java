package com.zxyw.sdk.net.nettyTCP;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

public class NettyListener {
    public void onTimeOut(IdleState state, ChannelHandlerContext ctx) {

    }

    public void onConnect(ChannelHandlerContext ctx) {

    }

    public void onDisconnect(ChannelHandlerContext ctx) {

    }

    public void onReceive(ChannelHandlerContext ctx, byte[] data) {

    }
}
