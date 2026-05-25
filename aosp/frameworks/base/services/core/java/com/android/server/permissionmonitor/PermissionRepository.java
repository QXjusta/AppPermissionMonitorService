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
 * 权限记录数据仓库
 * 提供统一的数据访问接口
 */
public class PermissionRepository {
    private final PermissionDao permissionDao;
    private final Context context;

    public PermissionRepository(Context context) {
        this.context = context;
        this.permissionDao = new PermissionDao(context);
    }

    public void initialize() {
        permissionDao.open();
    }

    public void shutdown() {
        permissionDao.close();
    }

    public long saveRecord(PermissionRecord record) {
        return permissionDao.insertRecord(record);
    }

    public void saveRecords(List<PermissionRecord> records) {
        permissionDao.insertRecords(records);
    }

    public List<PermissionRecord> getRecordsByPackage(String packageName, long startTime, long endTime) {
        return permissionDao.getRecordsByPackage(packageName, startTime, endTime);
    }

    public List<PermissionRecord> getAllRecords(long startTime, long endTime) {
        return permissionDao.getAllRecords(startTime, endTime);
    }

    public List<PermissionRecord> getRecordsByPermission(String permissionName, long startTime, long endTime) {
        return permissionDao.getRecordsByPermission(permissionName, startTime, endTime);
    }

    public int deleteOldRecords(long beforeTime) {
        return permissionDao.deleteRecordsBefore(beforeTime);
    }

    public String getStatistics() {
        long totalCount = permissionDao.getRecordCount();
        long earliestTime = permissionDao.getEarliestRecordTime();
        long latestTime = permissionDao.getLatestRecordTime();
        
        return String.format("总记录数: %d, 最早记录: %d, 最新记录: %d", 
                totalCount, earliestTime, latestTime);
    }

    public int clearAllRecords() {
        return permissionDao.deleteRecordsBefore(System.currentTimeMillis() + 1);
    }
}
