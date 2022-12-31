package com.mingrihuyu.mlbb.utils;

public class OsInfo {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWin(){
        return OS.indexOf("windows") >= 0;
    }

}
