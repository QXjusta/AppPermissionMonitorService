package com.example.apppermissionmonitorservice.utils;

import android.util.Log;

/**
 * 日志工具类
 */
public class LogUtils {
    private static final String TAG = "PermissionMonitor";
    private static boolean DEBUG = true;
    
    /**
     * 设置调试模式
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }
    
    /**
     * 记录调试日志
     */
    public static void d(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * 记录调试日志（带标签）
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(TAG + "/" + tag, message);
        }
    }
    
    /**
     * 记录信息日志
     */
    public static void i(String message) {
        Log.i(TAG, message);
    }
    
    /**
     * 记录信息日志（带标签）
     */
    public static void i(String tag, String message) {
        Log.i(TAG + "/" + tag, message);
    }
    
    /**
     * 记录警告日志
     */
    public static void w(String message) {
        Log.w(TAG, message);
    }
    
    /**
     * 记录警告日志（带标签）
     */
    public static void w(String tag, String message) {
        Log.w(TAG + "/" + tag, message);
    }
    
    /**
     * 记录错误日志
     */
    public static void e(String message) {
        Log.e(TAG, message);
    }
    
    /**
     * 记录错误日志（带标签）
     */
    public static void e(String tag, String message) {
        Log.e(TAG + "/" + tag, message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
    
    /**
     * 记录错误日志（带标签和异常）
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(TAG + "/" + tag, message, throwable);
    }
    
    /**
     * 记录详细日志
     */
    public static void v(String message) {
        if (DEBUG) {
            Log.v(TAG, message);
        }
    }
    
    /**
     * 记录详细日志（带标签）
     */
    public static void v(String tag, String message) {
        if (DEBUG) {
            Log.v(TAG + "/" + tag, message);
        }
    }
}