package com.example.apppermissionmonitorservice.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;

/**
 * 权限工具类
 */
public class PermissionUtils {
    
    /**
     * 检查是否拥有权限
     */
    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 检查是否拥有访问权限监控服务的权限
     */
    public static boolean hasAccessPermissionMonitorPermission(Context context) {
        return hasPermission(context, "com.example.ACCESS_PERMISSION_MONITOR");
    }
    
    /**
     * 将AppOps操作码转换为权限名称
     */
    public static String getPermissionNameForOp(int op) {
        if (op == getAppOpsValue("OP_CAMERA")) {
            return "android.permission.CAMERA";
        }
        if (op == getAppOpsValue("OP_RECORD_AUDIO")) {
            return "android.permission.RECORD_AUDIO";
        }
        if (op == getAppOpsValue("OP_COARSE_LOCATION")) {
            return "android.permission.ACCESS_COARSE_LOCATION";
        }
        if (op == getAppOpsValue("OP_FINE_LOCATION")) {
            return "android.permission.ACCESS_FINE_LOCATION";
        }
        if (op == getAppOpsValue("OP_READ_CONTACTS")) {
            return "android.permission.READ_CONTACTS";
        }
        if (op == getAppOpsValue("OP_WRITE_CONTACTS")) {
            return "android.permission.WRITE_CONTACTS";
        }
        if (op == getAppOpsValue("OP_READ_CALENDAR")) {
            return "android.permission.READ_CALENDAR";
        }
        if (op == getAppOpsValue("OP_WRITE_CALENDAR")) {
            return "android.permission.WRITE_CALENDAR";
        }
        if (op == getAppOpsValue("OP_READ_SMS")) {
            return "android.permission.READ_SMS";
        }
        if (op == getAppOpsValue("OP_WRITE_SMS")) {
            return "android.permission.WRITE_SMS";
        }
        if (op == getAppOpsValue("OP_READ_EXTERNAL_STORAGE")) {
            return "android.permission.READ_EXTERNAL_STORAGE";
        }
        if (op == getAppOpsValue("OP_WRITE_EXTERNAL_STORAGE")) {
            return "android.permission.WRITE_EXTERNAL_STORAGE";
        }
        if (op == getAppOpsValue("OP_READ_PHONE_STATE")) {
            return "android.permission.READ_PHONE_STATE";
        }
        if (op == getAppOpsValue("OP_CALL_PHONE")) {
            return "android.permission.CALL_PHONE";
        }
        return "android.permission.UNKNOWN_" + op;
    }

    private static int getAppOpsValue(String fieldName) {
        try {
            Field field = AppOpsManager.class.getField(fieldName);
            return field.getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * 将权限名称转换为友好的显示名称
     */
    public static String getFriendlyPermissionName(String permission) {
        if (permission == null) {
            return "未知权限";
        }
        
        switch (permission) {
            case "android.permission.CAMERA":
                return "相机";
            case "android.permission.RECORD_AUDIO":
                return "录音";
            case "android.permission.ACCESS_COARSE_LOCATION":
                return "粗略位置";
            case "android.permission.ACCESS_FINE_LOCATION":
                return "精确位置";
            case "android.permission.READ_CONTACTS":
                return "读取联系人";
            case "android.permission.WRITE_CONTACTS":
                return "写入联系人";
            case "android.permission.READ_CALENDAR":
                return "读取日历";
            case "android.permission.WRITE_CALENDAR":
                return "写入日历";
            case "android.permission.READ_SMS":
                return "读取短信";
            case "android.permission.WRITE_SMS":
                return "写入短信";
            case "android.permission.READ_EXTERNAL_STORAGE":
                return "读取外部存储";
            case "android.permission.WRITE_EXTERNAL_STORAGE":
                return "写入外部存储";
            case "android.permission.READ_PHONE_STATE":
                return "读取手机状态";
            case "android.permission.CALL_PHONE":
                return "拨打电话";
            default:
                // 去掉android.permission.前缀
                if (permission.startsWith("android.permission.")) {
                    return permission.substring("android.permission.".length());
                }
                return permission;
        }
    }
    
    /**
     * 获取权限分类
     */
    public static String getPermissionCategory(String permission) {
        if (permission == null) {
            return "其他";
        }
        
        if (permission.contains("LOCATION")) {
            return "位置";
        } else if (permission.contains("CAMERA") || permission.contains("RECORD_AUDIO")) {
            return "媒体";
        } else if (permission.contains("CONTACTS")) {
            return "联系人";
        } else if (permission.contains("CALENDAR")) {
            return "日历";
        } else if (permission.contains("SMS")) {
            return "短信";
        } else if (permission.contains("STORAGE")) {
            return "存储";
        } else if (permission.contains("PHONE")) {
            return "电话";
        } else {
            return "其他";
        }
    }
    
    /**
     * 检查权限是否危险
     */
    public static boolean isDangerousPermission(String permission) {
        // 根据Android权限分类，这些是危险权限
        String[] dangerousPermissions = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_SMS",
            "android.permission.WRITE_SMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE"
        };
        
        for (String dangerousPermission : dangerousPermissions) {
            if (dangerousPermission.equals(permission)) {
                return true;
            }
        }
        
        return false;
    }
}