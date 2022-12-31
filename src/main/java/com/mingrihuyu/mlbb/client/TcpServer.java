package com.mingrihuyu.mlbb.client;

import com.mingrihuyu.mlbb.utils.OsInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class TcpServer {

    private Logger logger = Logger.getLogger(TcpServer.class);

    private static final Map<Integer, Integer> portMap = new HashMap<Integer, Integer>(){
        {put(7000, 8000);}
        {put(7001, 8001);}
    };

    private int[] portArr;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private ServerBootstrap bootstrap = new ServerBootstrap();

    public TcpServer(String portStr) {
        String[] portStrArr = portStr.split(",");
        this.portArr = new int[portStrArr.length];
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
        logger.info("Tcp Server start......");
        try {
            bootstrap.group(bossGroup, workGroup)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new TcpServerHandler())
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
                        logger.info("tcp 成功绑定端口, " + port);
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
