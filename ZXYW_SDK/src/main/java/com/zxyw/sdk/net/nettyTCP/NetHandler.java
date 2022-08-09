package com.zxyw.sdk.net.nettyTCP;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class NetHandler extends SimpleChannelInboundHandler {
    private _NetListener listener;

    NetHandler(_NetListener listener) {
        this.listener = listener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (listener != null) listener.active(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (listener != null) listener.inActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (listener != null) {
            ByteBuf data = (ByteBuf) msg;
            int len = data.readableBytes();
            if (len > 0) {
                byte[] bytes = new byte[len];
                data.readBytes(bytes);
                listener.receive(ctx, bytes);
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent && listener != null) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) listener.allIdle(ctx);
            else if (event.state().equals(IdleState.READER_IDLE)) listener.readIdle(ctx);
            else if (event.state().equals(IdleState.WRITER_IDLE)) listener.writeIdle(ctx);
        }
    }
}
