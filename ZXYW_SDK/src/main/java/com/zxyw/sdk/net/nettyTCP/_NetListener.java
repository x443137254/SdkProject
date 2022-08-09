package com.zxyw.sdk.net.nettyTCP;

import io.netty.channel.ChannelHandlerContext;

interface _NetListener {
    void active(ChannelHandlerContext ctx);

    void inActive(ChannelHandlerContext ctx);

    void receive(ChannelHandlerContext ctx, byte[] message);

    void readIdle(ChannelHandlerContext ctx);

    void writeIdle(ChannelHandlerContext ctx);

    void allIdle(ChannelHandlerContext ctx);
}
