package com.mingrihuyu.mlbb;

import com.mingrihuyu.mlbb.client.TcpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpClientMain {

    private static ExecutorService pool = Executors.newFixedThreadPool(32);

    public static void main(String[] args) {
        TcpClient tcpClient = new TcpClient(9000, null);
    }
}
