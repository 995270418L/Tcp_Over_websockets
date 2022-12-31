package com.mingrihuyu.mlbb.server;

import com.mingrihuyu.mlbb.client.TcpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

public class WebsocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private Logger logger = Logger.getLogger(WebsocketServerHandler.class);

    private ExecutorService pool = Executors.newFixedThreadPool(32);
    private WebSocketServerHandshaker handshaker;

    private Map<String, TcpClient> tcpClientMap = new ConcurrentHashMap<String, TcpClient>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("成功建立 websocket 连接，开始建立 tcp 连接开始");
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        TcpClient tcpClient = new TcpClient(socketAddress.getPort(), ctx.channel());
        tcpClientMap.put(ctx.channel().id().asLongText(), tcpClient);
        pool.execute(tcpClient);
        logger.info("成功建立 websocket 连接，开始建立 tcp 连接 结束");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        logger.info("websocket 断开连接: " + channelId);
        TcpClient tcpClient = tcpClientMap.get(channelId);
        if(tcpClient != null && !tcpClient.isClose()){
            logger.info("websocket 断开连接，移除指定的 client");
            tcpClient.close();
        }
        tcpClientMap.remove(channelId);
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof FullHttpMessage){
            logger.info("收到 FullHttpMessage");
            handleHttpMsg(ctx, (FullHttpRequest) msg);
        }else if(msg instanceof WebSocketFrame){
            logger.info("收到简单的客户端消息 channel id: " + ctx.channel().id().asLongText());
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame){
        logger.info("websocket 收到消息：" + webSocketFrame.toString());
        if(webSocketFrame instanceof CloseWebSocketFrame){
            logger.info("关闭请求");
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) webSocketFrame).retain());
            return;
        }
        if (webSocketFrame instanceof PingWebSocketFrame) {
            logger.info("心跳请求");
            ctx.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }
        if(webSocketFrame instanceof TextWebSocketFrame){
            // 字符串类型消息处理
            String responseMsg = ((TextWebSocketFrame) webSocketFrame).text();
            logger.info("文本消息 " + responseMsg);
            String channelId = ctx.channel().id().asLongText();
            TcpClient tcpClient = tcpClientMap.get(channelId);
            if(tcpClient != null){
                ByteBuf buff = Unpooled.copiedBuffer(responseMsg, Charset.forName("UTF8"));
                tcpClient.writeAndFlush(buff);
            }
        }
        if(webSocketFrame instanceof BinaryWebSocketFrame){
            logger.info("收到 二进制消息, 开始转发" );
            String channelId = ctx.channel().id().asLongText();
            TcpClient tcpClient = tcpClientMap.get(channelId);
            if(tcpClient != null){
                ByteBuf buff = Unpooled.copiedBuffer(webSocketFrame.content());
                tcpClient.writeAndFlush(buff);
            }
        }
    }

    private void handleHttpMsg(ChannelHandlerContext ctx, FullHttpRequest req){

        //如果http解码失败，返回http异常
        //判断是否是WebSocket握手请求
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sentHttpResponse(ctx,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        String host = req.headers().get("Host");
        //构造握手响应返回
        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory("", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private void sentHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(byteBuf);
            byteBuf.release();
            setContentLength(res,res.content().readableBytes());
        }

        //如果是非keep-alive连接，关闭连接
        ChannelFuture future = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code()!=200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("exceptionCaught: ", cause);
        ctx.close();
    }

}
