package com.mingrihuyu.mlbb.server;

import com.mingrihuyu.mlbb.utils.OsInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class WebsocketClient implements Runnable {

    private static final Map<Integer, Integer> portMap = new HashMap<Integer, Integer>(){
        {put(8000, 7000);}
        {put(8001, 7001);}
    };

    private Integer targetPort;
    private EventLoopGroup workGroup;
    private Bootstrap bootstrap = new Bootstrap();

    private Channel channel;

    private Logger logger = Logger.getLogger(WebsocketClient.class);

    public WebsocketClient(Integer targetPort,final Channel tcpChannel) {
        this.targetPort = portMap.get(targetPort);
        logger.info("websocket client conn start: " + this.targetPort);
        if(OsInfo.isWin()){
            workGroup = new NioEventLoopGroup();
        }else{
            workGroup = new EpollEventLoopGroup();
        }
        if(OsInfo.isWin()){
            bootstrap.channel(NioSocketChannel.class);
        }else{
            bootstrap.channel(EpollSocketChannel.class);
        }
        URI uri = null;
        try {
            uri = new URI("ws://10.100.4.14:" + this.targetPort);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        final WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, (String) null, true, httpHeaders);
        final WebsocketClientHandler wch = new WebsocketClientHandler(tcpChannel);
        bootstrap.group(workGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new HttpObjectAggregator(65536))
                                .addLast(wch);
                    }
                });
        try {
            this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
            handshaker.handshake(this.channel);
            wch.setHandshaker(handshaker);
            wch.handshakeFuture().sync();
            logger.info("websocket 握手成功");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeAndFlush(ByteBuf msg){
        BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(msg);
        this.channel.writeAndFlush(binaryWebSocketFrame).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.isSuccess()){
                    logger.info("写入消息成功");
                }else{
                    logger.info("写入消息失败, " + channelFuture.cause().getMessage());
                }
            }
        });
    }

    public void run() {
        try {
            this.channel.closeFuture().sync();
        }catch (Exception e){
            logger.error("websocket client 建立连接失败, 错误信息: ", e);
        }
        logger.info("websocket client over");
    }
}
