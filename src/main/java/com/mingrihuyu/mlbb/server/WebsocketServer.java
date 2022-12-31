package com.mingrihuyu.mlbb.server;

import com.mingrihuyu.mlbb.utils.OsInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.Logger;


public class WebsocketServer {

    private int[] portArr;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private ServerBootstrap bootstrap = new ServerBootstrap();
    private Logger logger = Logger.getLogger(WebsocketServer.class);
    private ChannelFuture[] channelFutures = null;

    public WebsocketServer(String portStr) {
        if(portStr == null || portStr.trim().equals("")){
            logger.error("参数错误，启动失败");
            return;
        }
        String[] portStrArr = portStr.split(",");
        this.portArr = new int[portStrArr.length];
        this.channelFutures = new ChannelFuture[portStrArr.length];
        for(int i=0; i<portStrArr.length; i++){
            portArr[i] = Integer.parseInt(portStrArr[i]);
        }
        if(OsInfo.isWin()){
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
        }else{
            bossGroup = new EpollEventLoopGroup();
            workGroup = new EpollEventLoopGroup();
        }
    }

    public void start(){
        logger.info("Websocket Server start......");
        try {
            bootstrap.group(bossGroup, workGroup)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new LoggingHandler(LogLevel.DEBUG))
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new WebsocketServerHandler())
                            ;
                        }
                    });
            if(OsInfo.isWin()){
                bootstrap.channel(NioServerSocketChannel.class);
            }else{
                bootstrap.channel(EpollServerSocketChannel.class);
            }
            for(int i = 0; i<portArr.length; i++){
                final int port = portArr[i];
                ChannelFuture channelFuture = bootstrap.bind(port);
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.info("websocket 成功绑定端口, " + port);
                    }
                });
                final Channel channel = channelFuture.channel();
                channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        channel.close();
                    }
                });
            }
            for(;;);
        }catch (Exception e){
            logger.error("error: ", e);
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

}
