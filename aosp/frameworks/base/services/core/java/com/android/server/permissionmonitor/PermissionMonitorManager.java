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

import java.util.List;

/**
 * 权限监控管理器
 * 协调数据采集、存储和回调通知
 */
public class PermissionMonitorManager {
    private final PermissionRepository repository;
    private final AppOpsScanner scanner;
    
    private boolean isMonitoring = false;
    private OnPermissionMonitorListener listener;
    
    public interface OnPermissionMonitorListener {
        void onNewPermissionRecords(List<PermissionRecord> records);
        void onMonitorStarted();
        void onMonitorStopped();
        void onError(String error);
    }
    
    public PermissionMonitorManager(Context context) {
        this.repository = new PermissionRepository(context);
        this.scanner = new AppOpsScanner(context);
        
        scanner.setOnPermissionDetectedListener(records -> {
            handleNewPermissionRecords(records);
        });
    }
    
    public void initialize() {
        repository.initialize();
    }
    
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        scanner.start();
        isMonitoring = true;
        
        if (listener != null) {
            listener.onMonitorStarted();
        }
    }
    
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        scanner.stop();
        isMonitoring = false;
        
        if (listener != null) {
            listener.onMonitorStopped();
        }
    }
    
    public void shutdown() {
        stopMonitoring();
        repository.shutdown();
    }
    
    private void handleNewPermissionRecords(List<PermissionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        repository.saveRecords(records);
        
        if (listener != null) {
            listener.onNewPermissionRecords(records);
        }
    }
    
    public void setOnPermissionMonitorListener(OnPermissionMonitorListener listener) {
        this.listener = listener;
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    public PermissionRepository getRepository() {
        return repository;
    }
    
    public AppOpsScanner getScanner() {
        return scanner;
    }
    
    public String getMonitoringStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("监控状态: ").append(isMonitoring ? "运行中" : "已停止").append("\n");
        stats.append("扫描器状态: ").append(scanner.isScanning() ? "运行中" : "已停止").append("\n");
        stats.append("监控的权限数量: ").append(scanner.getMonitoredOps().size()).append("\n");
        stats.append("数据库统计: ").append(repository.getStatistics());
        
        return stats.toString();
    }
    
    public void addMonitoredPermission(int op) {
        scanner.addMonitoredOp(op);
    }
    
    public void removeMonitoredPermission(int op) {
        scanner.removeMonitoredOp(op);
    }
    
    public void scanNow() {
        scanner.scanOnce();
    }
    
    public int cleanupOldRecords(long retentionDays) {
        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L);
        return repository.deleteOldRecords(cutoffTime);
    }
}
