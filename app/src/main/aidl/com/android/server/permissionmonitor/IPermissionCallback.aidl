// IPermissionCallback.aidl
package com.android.server.permissionmonitor;

import com.android.server.permissionmonitor.PermissionRecord;

interface IPermissionCallback {
    void onPermissionUsed(in PermissionRecord record);
    
    void onServiceStatusChanged(String status);
}
