// IPermissionMonitor.aidl
package com.android.server.permissionmonitor;

import com.android.server.permissionmonitor.PermissionRecord;
import com.android.server.permissionmonitor.IPermissionCallback;

/**
 * 权限监控服务接口
 */
interface IPermissionMonitor {
    List<PermissionRecord> getRecords(String packageName, long startTime, long endTime);
    
    List<PermissionRecord> getAllRecords(long startTime, long endTime);
    
    void registerCallback(IPermissionCallback callback);
    
    void unregisterCallback(IPermissionCallback callback);
    
    List<String> getMonitoredPermissions();
    
    void setMonitoredPermissions(in List<String> permissions);
    
    boolean clearRecords(long beforeTime);
    
    String getServiceStatus();
}
