package com.mingrihuyu.mlbb;

import com.mingrihuyu.mlbb.client.TcpServer;

public class TcpServerMain {

    public static void main(String[] args) {
        new TcpServer("9090").start();
    }
}
