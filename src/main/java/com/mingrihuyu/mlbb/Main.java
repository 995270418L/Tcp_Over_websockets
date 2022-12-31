package com.mingrihuyu.mlbb;

import com.mingrihuyu.mlbb.client.TcpClient;
import com.mingrihuyu.mlbb.client.TcpServer;
import com.mingrihuyu.mlbb.server.WebsocketServer;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if(args.length < 2){
            logger.error("usage: java -jar xxxx.jar mode port(seperate by ,)");
        }
        String mode = args[0];
        String portStr = args[1];
        if(mode.equals("websocket")){
            new WebsocketServer(portStr).start();
        }else if(mode.equals("tcp")){
            new TcpServer(portStr).start();
        }else{
            logger.error("mode not right, must be server or client");
        }
    }

}
