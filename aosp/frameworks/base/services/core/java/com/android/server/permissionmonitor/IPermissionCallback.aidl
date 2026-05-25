// IPermissionCallback.aidl
package com.android.server.permissionmonitor;

import com.android.server.permissionmonitor.PermissionRecord;

/**
 * 权限使用回调接口
 */
interface IPermissionCallback {
    void onPermissionUsed(in PermissionRecord record);
    
    void onServiceStatusChanged(String status);
}
