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

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AppOps扫描器
 * 负责定期扫描AppOpsManager获取权限使用记录
 */
public class AppOpsScanner {
    private static final String TAG = "AppOpsScanner";
    private static final long SCAN_INTERVAL_MS = 1000;
    
    private final Context context;
    private final AppOpsManager appOpsManager;
    private HandlerThread scannerThread;
    private Handler scannerHandler;
    private boolean isScanning = false;
    
    private static final int[] DEFAULT_MONITORED_OPS = {
            AppOpsManager.OP_CAMERA,
            AppOpsManager.OP_RECORD_AUDIO,
            AppOpsManager.OP_COARSE_LOCATION,
            AppOpsManager.OP_FINE_LOCATION,
            AppOpsManager.OP_READ_CONTACTS,
            AppOpsManager.OP_WRITE_CONTACTS,
            AppOpsManager.OP_READ_CALENDAR,
            AppOpsManager.OP_WRITE_CALENDAR,
            AppOpsManager.OP_READ_SMS,
            AppOpsManager.OP_WRITE_SMS
    };
    
    private final Set<Integer> monitoredOps = new HashSet<>();
    private OnPermissionDetectedListener listener;
    
    private long lastScanTime = 0;
    
    public interface OnPermissionDetectedListener {
        void onPermissionDetected(List<PermissionRecord> records);
    }
    
    public AppOpsScanner(Context context) {
        this.context = context;
        this.appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        
        for (int op : DEFAULT_MONITORED_OPS) {
            monitoredOps.add(op);
        }
    }
    
    public void start() {
        if (isScanning) {
            return;
        }
        
        scannerThread = new HandlerThread("AppOpsScanner");
        scannerThread.start();
        scannerHandler = new Handler(scannerThread.getLooper());
        
        isScanning = true;
        lastScanTime = System.currentTimeMillis();
        
        scannerHandler.post(scanRunnable);
    }
    
    public void stop() {
        if (!isScanning) {
            return;
        }
        
        isScanning = false;
        if (scannerHandler != null) {
            scannerHandler.removeCallbacks(scanRunnable);
        }
        
        if (scannerThread != null) {
            scannerThread.quitSafely();
            scannerThread = null;
            scannerHandler = null;
        }
    }
    
    public void setOnPermissionDetectedListener(OnPermissionDetectedListener listener) {
        this.listener = listener;
    }
    
    public void addMonitoredOp(int op) {
        monitoredOps.add(op);
    }
    
    public void removeMonitoredOp(int op) {
        monitoredOps.remove(op);
    }
    
    public List<Integer> getMonitoredOps() {
        return new ArrayList<>(monitoredOps);
    }
    
    public void scanOnce() {
        if (scannerHandler != null) {
            scannerHandler.post(scanRunnable);
        }
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                performScan();
            } catch (Exception e) {
                Log.e(TAG, "Error during scan", e);
            }
            
            if (isScanning) {
                scannerHandler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        }
    };
    
    private long getLastAccessTime(AppOpsManager.OpEntry opEntry) {
        return opEntry.getTime();
    }

    private long invokeLongMethod(AppOpsManager.OpEntry opEntry, String methodName,
            Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = AppOpsManager.OpEntry.class.getMethod(methodName, parameterTypes);
            Object result = method.invoke(opEntry, args);
            if (result instanceof Long) {
                return (Long) result;
            }
        } catch (Exception e) {
            Log.d(TAG, "invokeLongMethod failed: " + methodName + ", error=" + e.getMessage());
        }
        return Long.MIN_VALUE;
    }

    private String buildTimeDebugInfo(AppOpsManager.OpEntry opEntry) {
        long lastAccessTimeFlags = invokeLongMethod(opEntry, "getLastAccessTime",
                new Class<?>[] {int.class}, new Object[] {0});
        long lastAccessTimeNoArgs = invokeLongMethod(opEntry, "getLastAccessTime",
                new Class<?>[0], new Object[0]);
        long timeNoArgs = invokeLongMethod(opEntry, "getTime",
                new Class<?>[0], new Object[0]);
        long rejectTimeNoArgs = invokeLongMethod(opEntry, "getRejectTime",
                new Class<?>[0], new Object[0]);
        long lastRejectTimeFlags = invokeLongMethod(opEntry, "getLastRejectTime",
                new Class<?>[] {int.class}, new Object[] {0});
        long lastRejectTimeNoArgs = invokeLongMethod(opEntry, "getLastRejectTime",
                new Class<?>[0], new Object[0]);
        long durationNoArgs = invokeLongMethod(opEntry, "getDuration",
                new Class<?>[0], new Object[0]);
        long lastDurationFlags = invokeLongMethod(opEntry, "getLastDuration",
                new Class<?>[] {int.class}, new Object[] {0});
        long lastDurationNoArgs = invokeLongMethod(opEntry, "getLastDuration",
                new Class<?>[0], new Object[0]);

        return "times{"
                + "getLastAccessTime(0)=" + lastAccessTimeFlags
                + ", getLastAccessTime()=" + lastAccessTimeNoArgs
                + ", getTime()=" + timeNoArgs
                + ", getRejectTime()=" + rejectTimeNoArgs
                + ", getLastRejectTime(0)=" + lastRejectTimeFlags
                + ", getLastRejectTime()=" + lastRejectTimeNoArgs
                + ", getDuration()=" + durationNoArgs
                + ", getLastDuration(0)=" + lastDurationFlags
                + ", getLastDuration()=" + lastDurationNoArgs
                + "}";
    }

    private void performScan() {
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "performScan start: currentTime=" + currentTime
                + ", lastScanTime=" + lastScanTime
                + ", monitoredOps=" + monitoredOps);
        
        try {
            int[] opsArray = new int[monitoredOps.size()];
            int i = 0;
            for (Integer op : monitoredOps) {
                opsArray[i++] = op;
            }
            
            List<AppOpsManager.PackageOps> packageOpsList = 
                    appOpsManager.getPackagesForOps(opsArray);
            
            if (packageOpsList == null || packageOpsList.isEmpty()) {
                Log.d(TAG, "performScan: getPackagesForOps returned no data");
                return;
            }

            Log.d(TAG, "performScan: packageOpsList size=" + packageOpsList.size());
            
            List<PermissionRecord> newRecords = new ArrayList<>();
            
            for (AppOpsManager.PackageOps packageOps : packageOpsList) {
                String packageName = packageOps.getPackageName();
                int uid = packageOps.getUid();
                Log.d(TAG, "performScan: package=" + packageName + ", uid=" + uid);
                
                if (uid < Process.FIRST_APPLICATION_UID) {
                    Log.d(TAG, "performScan: skip non-application uid for package=" + packageName
                            + ", uid=" + uid);
                    continue;
                }
                
                List<AppOpsManager.OpEntry> opEntries = packageOps.getOps();
                if (opEntries == null || opEntries.isEmpty()) {
                    Log.d(TAG, "performScan: package=" + packageName + " has no op entries");
                    continue;
                }

                Log.d(TAG, "performScan: package=" + packageName + " opEntries size="
                        + opEntries.size());
                for (AppOpsManager.OpEntry opEntry : opEntries) {
                    int op = opEntry.getOp();
                    long lastAccessTime = getLastAccessTime(opEntry);
                    boolean passesWindow = lastAccessTime > lastScanTime
                            && lastAccessTime <= currentTime;
                    Log.d(TAG, "performScan: package=" + packageName
                            + ", op=" + op
                            + ", permission=" + getPermissionNameForOp(op)
                            + ", lastAccessTime=" + lastAccessTime
                            + ", lastScanTime=" + lastScanTime
                            + ", currentTime=" + currentTime
                            + ", passesWindow=" + passesWindow
                            + ", " + buildTimeDebugInfo(opEntry));
                    
                    if (passesWindow) {
                        String permissionName = getPermissionNameForOp(op);
                        PermissionRecord record = new PermissionRecord(
                                packageName, 
                                permissionName, 
                                lastAccessTime
                        );
                        newRecords.add(record);
                        Log.d(TAG, "performScan: accepted record package=" + packageName
                                + ", permission=" + permissionName
                                + ", timestamp=" + lastAccessTime);
                    } else {
                        Log.d(TAG, "performScan: filtered out package=" + packageName
                                + ", op=" + op
                                + ", lastAccessTime=" + lastAccessTime);
                    }
                }
            }
            
            lastScanTime = currentTime;
            Log.d(TAG, "performScan end: newRecords size=" + newRecords.size()
                    + ", updated lastScanTime=" + lastScanTime);
            
            if (listener != null && !newRecords.isEmpty()) {
                Log.d(TAG, "performScan: dispatching " + newRecords.size()
                        + " records to listener");
                listener.onPermissionDetected(newRecords);
            } else if (listener == null) {
                Log.d(TAG, "performScan: listener is null, records not dispatched");
            } else {
                Log.d(TAG, "performScan: no new records to dispatch");
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during scan", e);
            stop();
        }
    }
    
    private String getPermissionNameForOp(int op) {
        switch (op) {
            case AppOpsManager.OP_CAMERA:
                return "android.permission.CAMERA";
            case AppOpsManager.OP_RECORD_AUDIO:
                return "android.permission.RECORD_AUDIO";
            case AppOpsManager.OP_COARSE_LOCATION:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case AppOpsManager.OP_FINE_LOCATION:
                return "android.permission.ACCESS_FINE_LOCATION";
            case AppOpsManager.OP_READ_CONTACTS:
                return "android.permission.READ_CONTACTS";
            case AppOpsManager.OP_WRITE_CONTACTS:
                return "android.permission.WRITE_CONTACTS";
            case AppOpsManager.OP_READ_CALENDAR:
                return "android.permission.READ_CALENDAR";
            case AppOpsManager.OP_WRITE_CALENDAR:
                return "android.permission.WRITE_CALENDAR";
            case AppOpsManager.OP_READ_SMS:
                return "android.permission.READ_SMS";
            case AppOpsManager.OP_WRITE_SMS:
                return "android.permission.WRITE_SMS";
            default:
                return "android.permission.UNKNOWN_" + op;
        }
    }
}
