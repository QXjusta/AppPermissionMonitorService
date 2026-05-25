/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.permissionmonitor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限监控系统服务
 * 继承SystemService，作为系统级服务运行
 */
public class PermissionMonitorService extends SystemService {
    private static final String TAG = "PermissionMonitorService";
    public static final String SERVICE_NAME = "permission_monitor";
    
    private final Context mContext;
    private IBinder mBinder;
    private PermissionMonitorManager mPermissionMonitor;
    private final RemoteCallbackList<IPermissionCallback> mCallbacks = new RemoteCallbackList<>();
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private int mRegisteredCallbackCount = 0;
    private boolean mBootCompleted = false;
    private boolean mMonitoringInitialized = false;
    private String mMonitoringInitError;
    
    private final List<String> mMonitoredPermissions = new ArrayList<>();
    
    public PermissionMonitorService(Context context) {
        super(context);
        mContext = context;
        initDefaultPermissions();
    }
    
    @Override
    public void onStart() {
        Log.i(TAG, "Starting PermissionMonitorService");

        mHandlerThread = new HandlerThread("PermissionMonitorService");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mPermissionMonitor = new PermissionMonitorManager(mContext);

        mBinder = new PermissionMonitorStub();
        publishBinderService(SERVICE_NAME, mBinder);

        Log.i(TAG, "PermissionMonitorService binder published");
    }
    
    @Override
    public void onBootPhase(int phase) {
        Log.d(TAG, "Boot phase: " + phase);
        
        if (phase == SystemService.PHASE_BOOT_COMPLETED && !mBootCompleted) {
            mBootCompleted = true;
            initializeMonitoring();
        }
    }

    private void initializeMonitoring() {
        if (mMonitoringInitialized) {
            return;
        }

        if (mPermissionMonitor == null) {
            mMonitoringInitError = "PermissionMonitorManager is not ready";
            Log.e(TAG, mMonitoringInitError);
            return;
        }

        try {
            mPermissionMonitor.initialize();

            mPermissionMonitor.setOnPermissionMonitorListener(
                    new PermissionMonitorManager.OnPermissionMonitorListener() {
                @Override
                public void onNewPermissionRecords(List<PermissionRecord> records) {
                    handleNewPermissionRecords(records);
                }
                
                @Override
                public void onMonitorStarted() {
                    Log.i(TAG, "Permission monitoring started");
                }
                
                @Override
                public void onMonitorStopped() {
                    Log.i(TAG, "Permission monitoring stopped");
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Permission monitor error: " + error);
                }
            });

            mPermissionMonitor.startMonitoring();
            mMonitoringInitialized = true;
            mMonitoringInitError = null;

            Log.i(TAG, "PermissionMonitorService monitoring initialized after boot completed");
        } catch (RuntimeException e) {
            mMonitoringInitError = e.getMessage();
            Log.e(TAG, "Failed to initialize permission monitoring", e);
        }
    }
    
    private void initDefaultPermissions() {
        mMonitoredPermissions.add("android.permission.CAMERA");
        mMonitoredPermissions.add("android.permission.RECORD_AUDIO");
        mMonitoredPermissions.add("android.permission.ACCESS_COARSE_LOCATION");
        mMonitoredPermissions.add("android.permission.ACCESS_FINE_LOCATION");
        mMonitoredPermissions.add("android.permission.READ_CONTACTS");
        mMonitoredPermissions.add("android.permission.WRITE_CONTACTS");
        mMonitoredPermissions.add("android.permission.READ_SMS");
        mMonitoredPermissions.add("android.permission.WRITE_SMS");
        mMonitoredPermissions.add("android.permission.READ_CALENDAR");
        mMonitoredPermissions.add("android.permission.WRITE_CALENDAR");
    }
    
    private void handleNewPermissionRecords(List<PermissionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        final int N = mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IPermissionCallback callback = mCallbacks.getBroadcastItem(i);
            try {
                for (PermissionRecord record : records) {
                    callback.onPermissionUsed(record);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Callback failed: " + e.getMessage());
            }
        }
        mCallbacks.finishBroadcast();
    }
    
    private boolean checkCallerPermission() {
        return mContext.checkCallingOrSelfPermission(
                "com.android.permission.ACCESS_PERMISSION_MONITOR")
                == PackageManager.PERMISSION_GRANTED;
    }

    private void dumpInternal(PrintWriter pw) {
        pw.println("PermissionMonitorService Status:");
        pw.println("================================");

        if (mPermissionMonitor == null) {
            pw.println("Service not initialized");
            return;
        }

        pw.println("Boot completed: " + mBootCompleted);
        pw.println("Monitoring initialized: " + mMonitoringInitialized);
        if (mMonitoringInitError != null) {
            pw.println("Monitoring init error: " + mMonitoringInitError);
        }

        if (mMonitoringInitialized) {
            pw.println(mPermissionMonitor.getMonitoringStats());
        }
        pw.println("Registered callbacks: " + mRegisteredCallbackCount);
        pw.println("Monitored permissions: " + mMonitoredPermissions.size());

        for (String permission : mMonitoredPermissions) {
            pw.println("  - " + permission);
        }
    }
    
    private class PermissionMonitorStub extends IPermissionMonitor.Stub {
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                    != PackageManager.PERMISSION_GRANTED) {
                pw.println("Permission Denial: can't dump PermissionMonitorService");
                return;
            }
            dumpInternal(pw);
        }

        @Override
        public List<PermissionRecord> getRecords(String packageName, long startTime, 
                long endTime) throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            if (mPermissionMonitor == null || mPermissionMonitor.getRepository() == null) {
                return new ArrayList<>();
            }
            
            if (packageName == null || packageName.isEmpty()) {
                return mPermissionMonitor.getRepository().getAllRecords(startTime, endTime);
            } else {
                return mPermissionMonitor.getRepository()
                        .getRecordsByPackage(packageName, startTime, endTime);
            }
        }
        
        @Override
        public List<PermissionRecord> getAllRecords(long startTime, 
                long endTime) throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            if (mPermissionMonitor == null || mPermissionMonitor.getRepository() == null) {
                return new ArrayList<>();
            }
            
            return mPermissionMonitor.getRepository().getAllRecords(startTime, endTime);
        }
        
        @Override
        public void registerCallback(IPermissionCallback callback) throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            if (callback != null) {
                mCallbacks.register(callback);
                mRegisteredCallbackCount++;
                Log.d(TAG, "Callback registered");
            }
        }
        
        @Override
        public void unregisterCallback(IPermissionCallback callback) throws RemoteException {
            if (callback != null) {
                mCallbacks.unregister(callback);
                if (mRegisteredCallbackCount > 0) {
                    mRegisteredCallbackCount--;
                }
                Log.d(TAG, "Callback unregistered");
            }
        }
        
        @Override
        public List<String> getMonitoredPermissions() throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            return new ArrayList<>(mMonitoredPermissions);
        }
        
        @Override
        public void setMonitoredPermissions(List<String> permissions) throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            mMonitoredPermissions.clear();
            if (permissions != null) {
                mMonitoredPermissions.addAll(permissions);
            }
            
            Log.i(TAG, "Monitored permissions updated: " 
                    + mMonitoredPermissions.size() + " permissions");
        }
        
        @Override
        public boolean clearRecords(long beforeTime) throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            if (mPermissionMonitor == null || mPermissionMonitor.getRepository() == null) {
                return false;
            }
            
            int deletedCount = mPermissionMonitor.getRepository().deleteOldRecords(beforeTime);
            Log.i(TAG, "Cleared " + deletedCount + " records before " + beforeTime);
            return deletedCount > 0;
        }
        
        @Override
        public String getServiceStatus() throws RemoteException {
            if (!checkCallerPermission()) {
                throw new SecurityException(
                        "Caller does not have permission to access PermissionMonitorService");
            }
            
            if (mPermissionMonitor == null) {
                return "Service not initialized";
            }

            if (!mMonitoringInitialized) {
                if (mMonitoringInitError != null) {
                    return "Monitoring initialization failed: " + mMonitoringInitError;
                }
                return mBootCompleted ? "Monitoring not initialized" : "Waiting for boot completion";
            }
            
            return mPermissionMonitor.getMonitoringStats();
        }
    }
    
    protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter pw, String[] args) {
        dumpInternal(pw);
    }
}
