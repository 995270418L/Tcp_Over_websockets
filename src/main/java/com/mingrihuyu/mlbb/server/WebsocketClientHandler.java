package com.mingrihuyu.mlbb.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.Data;
import org.apache.log4j.Logger;

import javax.xml.soap.Text;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

public class WebsocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private Logger logger = Logger.getLogger(WebsocketClientHandler.class);

    private Channel tcpChannel;
    private WebSocketClientHandshaker handshaker;
    private ChannelPromise channelPromise;

    public WebSocketClientHandshaker getHandshaker() {
        return handshaker;
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }


    public WebsocketClientHandler(Channel channel){
        this.tcpChannel = channel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.channelPromise = ctx.newPromise();
    }

    public ChannelFuture handshakeFuture() {
        return this.channelPromise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("成功建立 websocket 连接, 准备发送数据");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        logger.debug("断开连接: " + ctx.channel().id().asLongText());
        tcpChannel.close();
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("收到消息, 判断 websocket 是否握手完毕: " + this.handshaker.isHandshakeComplete());
        if(msg instanceof ByteBuf){
            ByteBuf buf = (ByteBuf) msg;
            logger.info("websocket is bytebuf msg: " + ByteBufUtil.hexDump(buf));
        }
        Channel channel = ctx.channel();
        FullHttpResponse response;
        if(!this.handshaker.isHandshakeComplete()){
            try{
                response = (FullHttpResponse) msg;
                this.handshaker.finishHandshake(ctx.channel(), response.retain());
                this.channelPromise.setSuccess();
                logger.info("websocket client handshark complete .....");
            }catch (WebSocketHandshakeException e){
                logger.error("websocket 握手失败, 错误信息: ", e);
                this.channelPromise.setFailure(e.getCause());
            }
        }else if(msg instanceof FullHttpResponse){
            logger.debug("websocket 握手响应");
            response = (FullHttpResponse) msg;
            logger.error("Unexpetcd FullHttpResponse ( getStatus = " + response.status());
        }else{
            WebSocketFrame frame = (WebSocketFrame) msg;
            logger.info("websocket server 响应数据: " + ByteBufUtil.hexDump(frame.content()));
            if(frame instanceof CloseWebSocketFrame){
                logger.info("websocket 关闭请求");
                channel.close();
            }else if(frame instanceof TextWebSocketFrame){
                logger.info("TextWebSocketFrame msg");
                this.tcpChannel.writeAndFlush(frame.content());
            }else if(frame instanceof BinaryWebSocketFrame){
                logger.info("BinaryWebSocketFrame msg");
                ByteBuf buff = Unpooled.copiedBuffer(frame.content());
                this.tcpChannel.writeAndFlush(buff);
            }else if (frame instanceof PingWebSocketFrame) {
                logger.info("心跳请求");
                ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            }
        }
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame){
        if(webSocketFrame instanceof CloseWebSocketFrame){
            logger.debug("关闭请求");
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) webSocketFrame).retain());
            return;
        }
        if (webSocketFrame instanceof PingWebSocketFrame) {
            logger.debug("心跳请求");
            ctx.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }
        if(webSocketFrame instanceof TextWebSocketFrame){
            // 字符串类型消息处理
            logger.debug("文本消息");
            String responseMsg = ((TextWebSocketFrame) webSocketFrame).text();
            ctx.writeAndFlush(new TextWebSocketFrame(responseMsg));
        }
        if(webSocketFrame instanceof BinaryWebSocketFrame){
            logger.info("二进制消息: " );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("exceptionCaught: ", cause);
        ctx.close();
    }

}
