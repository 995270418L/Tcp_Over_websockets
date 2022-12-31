package com.mingrihuyu.mlbb.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

public class TcpClientHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = Logger.getLogger(TcpClientHandler.class);

    private Channel websocketChannel;

    public TcpClientHandler(Channel channel) {
        this.websocketChannel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("tcp client channel active " + ctx.channel().id().asLongText());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("TCP client 收到服务器响应..." + ctx.channel().id().asLongText());
        ByteBuf buf = (ByteBuf) msg;
        logger.info("TCP client 服务端响应的数据是:" + ByteBufUtil.hexDump(buf));
        if(websocketChannel != null){
            ByteBuf tcpData = Unpooled.copiedBuffer(buf);
            logger.info("tcp client 开始发送数据 " + ByteBufUtil.hexDump(tcpData));
            BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(tcpData);
            websocketChannel.writeAndFlush(binaryWebSocketFrame);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("TCP client 断开连接:" + ctx.channel().id().asLongText());
        ctx.channel().close();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("tcp server 遇到异常: " + cause);
        ctx.channel().close();
        ctx.close();
    }
}
