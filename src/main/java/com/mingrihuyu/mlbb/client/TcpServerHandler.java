package com.mingrihuyu.mlbb.client;

import com.mingrihuyu.mlbb.server.WebsocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = Logger.getLogger(TcpServerHandler.class);
    private ExecutorService pool = Executors.newFixedThreadPool(32);

    private Map<String, WebsocketClient> websocketClientMap = new ConcurrentHashMap<String, WebsocketClient>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        logger.info("tcp server active ....." + channelId);
        logger.debug("tcp 客户端开始和 websocket 服务端建立连接, " + channelId);
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        WebsocketClient websocketClient = new WebsocketClient(socketAddress.getPort(), ctx.channel());
        websocketClientMap.put(channelId, websocketClient);
        pool.execute(websocketClient);
        logger.info("tcp 客户端开始和 websocket 服务端建立连接 结束");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        logger.info("客户端请求到了..." + channelId);
        ByteBuf buf = (ByteBuf) msg;
        logger.info("TCP server 收到的数据是:" + ByteBufUtil.hexDump(buf));
        logger.debug("开始转发tcp消息到websocket");
        WebsocketClient websocketClient = websocketClientMap.get(channelId);
        if(websocketClient != null){
            logger.info("发送给 websocket client");
            ByteBuf buff = Unpooled.copiedBuffer(buf);
            websocketClient.writeAndFlush(buff);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        websocketClientMap.remove(channelId);
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("tcp server 遇到异常: " + cause);
        ctx.close();
    }
}
