package com.mingrihuyu.mlbb.client;

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
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class TcpClient implements Runnable {

    private Logger logger = Logger.getLogger(TcpClient.class);

    private static final Map<Integer, Integer> portMap = new HashMap<Integer, Integer>(){
        {put(7000, 8000);}
        {put(7001, 8001);}
        {put(9000, 9090);}
    };
    private Integer targetPort;
    private EventLoopGroup workGroup;
    private Bootstrap bootstrap = new Bootstrap();
    private ChannelFuture channelFuture;

    private Channel websocketChannel;

    public Channel getWebsocketChannel() {
        return websocketChannel;
    }

    public void setWebsocketChannel(Channel websocketChannel) {
        this.websocketChannel = websocketChannel;
    }

    public TcpClient(Integer port, final Channel channel) {
        logger.info("Tcp Client connect start......");
        this.targetPort = portMap.get(port);
        setWebsocketChannel(channel);
        if(this.targetPort == null){
            logger.error("tcp client 初始化失败,端口未找到, 源端口: " + port);
            return ;
        }
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
        bootstrap.group(workGroup)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    logger.info("初始化 channel .... ");
                    ch.pipeline()
                            .addLast(new TcpClientHandler(channel))
                    ;
                }
            });
    }

    public void writeAndFlush(ByteBuf msg){
        channelFuture.channel().writeAndFlush(msg);
    }

    public boolean isClose(){
        return channelFuture == null ||  !channelFuture.channel().isOpen();
    }

    public void close(){
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            channelFuture = bootstrap.connect("127.0.0.1", this.targetPort).sync();
            if(channelFuture.isSuccess()){
                logger.info("建立tcp连接成功, target ip: 127.0.0.1, port " + this.targetPort);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("error: ", e);
        }finally {
            workGroup.shutdownGracefully();
        }
        logger.info("建立连接结束.... ");
    }
}
