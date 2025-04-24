package com.thunder.NovaAPI.server.network;

import io.netty.channel.ChannelHandlerContext;

public interface NovaAPINetworkHandlerr {
    void channelRead0(ChannelHandlerContext ctx, String msg);
}
