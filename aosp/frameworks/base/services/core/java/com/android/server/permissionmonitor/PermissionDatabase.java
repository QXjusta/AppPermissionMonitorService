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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限记录数据库帮助类
 */
public class PermissionDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "permission_monitor.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PERMISSION_RECORDS = "permission_records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PACKAGE_NAME = "package_name";
    public static final String COLUMN_PERMISSION_NAME = "permission_name";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_BLOCKED = "is_blocked";
    public static final String COLUMN_EXTRA_INFO = "extra_info";

    private static final String CREATE_TABLE_PERMISSION_RECORDS =
            "CREATE TABLE " + TABLE_PERMISSION_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PACKAGE_NAME + " TEXT NOT NULL, " +
                    COLUMN_PERMISSION_NAME + " TEXT NOT NULL, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                    COLUMN_IS_BLOCKED + " INTEGER DEFAULT 0, " +
                    COLUMN_EXTRA_INFO + " TEXT, " +
                    "UNIQUE(" + COLUMN_PACKAGE_NAME + ", " + COLUMN_PERMISSION_NAME + ", " + COLUMN_TIMESTAMP + ")" +
                    ");";

    private static final String CREATE_INDEX_PACKAGE_NAME =
            "CREATE INDEX idx_package_name ON " + TABLE_PERMISSION_RECORDS + "(" + COLUMN_PACKAGE_NAME + ");";
    
    private static final String CREATE_INDEX_TIMESTAMP =
            "CREATE INDEX idx_timestamp ON " + TABLE_PERMISSION_RECORDS + "(" + COLUMN_TIMESTAMP + ");";
    
    private static final String CREATE_INDEX_PERMISSION_NAME =
            "CREATE INDEX idx_permission_name ON " + TABLE_PERMISSION_RECORDS + "(" + COLUMN_PERMISSION_NAME + ");";

    public PermissionDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PERMISSION_RECORDS);
        db.execSQL(CREATE_INDEX_PACKAGE_NAME);
        db.execSQL(CREATE_INDEX_TIMESTAMP);
        db.execSQL(CREATE_INDEX_PERMISSION_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERMISSION_RECORDS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

/**
 * 权限记录数据访问对象
 */
class PermissionDao {
    private final PermissionDatabase dbHelper;
    private SQLiteDatabase database;

    public PermissionDao(Context context) {
        dbHelper = new PermissionDatabase(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    public long insertRecord(PermissionRecord record) {
        ContentValues values = new ContentValues();
        values.put(PermissionDatabase.COLUMN_PACKAGE_NAME, record.getPackageName());
        values.put(PermissionDatabase.COLUMN_PERMISSION_NAME, record.getPermissionName());
        values.put(PermissionDatabase.COLUMN_TIMESTAMP, record.getTimestamp());
        values.put(PermissionDatabase.COLUMN_IS_BLOCKED, record.isBlocked() ? 1 : 0);
        values.put(PermissionDatabase.COLUMN_EXTRA_INFO, record.getExtraInfo());

        return database.insert(PermissionDatabase.TABLE_PERMISSION_RECORDS, null, values);
    }

    public void insertRecords(List<PermissionRecord> records) {
        database.beginTransaction();
        try {
            for (PermissionRecord record : records) {
                insertRecord(record);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public List<PermissionRecord> getRecordsByPackage(String packageName, long startTime, long endTime) {
        List<PermissionRecord> records = new ArrayList<>();
        
        String selection = PermissionDatabase.COLUMN_PACKAGE_NAME + " = ? AND " +
                          PermissionDatabase.COLUMN_TIMESTAMP + " >= ? AND " +
                          PermissionDatabase.COLUMN_TIMESTAMP + " <= ?";
        String[] selectionArgs = {packageName, String.valueOf(startTime), String.valueOf(endTime)};
        
        String orderBy = PermissionDatabase.COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = database.query(
                PermissionDatabase.TABLE_PERMISSION_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        
        return records;
    }

    public List<PermissionRecord> getAllRecords(long startTime, long endTime) {
        List<PermissionRecord> records = new ArrayList<>();
        
        String selection = PermissionDatabase.COLUMN_TIMESTAMP + " >= ? AND " +
                          PermissionDatabase.COLUMN_TIMESTAMP + " <= ?";
        String[] selectionArgs = {String.valueOf(startTime), String.valueOf(endTime)};
        
        String orderBy = PermissionDatabase.COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = database.query(
                PermissionDatabase.TABLE_PERMISSION_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        
        return records;
    }

    public List<PermissionRecord> getRecordsByPermission(String permissionName, long startTime, long endTime) {
        List<PermissionRecord> records = new ArrayList<>();
        
        String selection = PermissionDatabase.COLUMN_PERMISSION_NAME + " = ? AND " +
                          PermissionDatabase.COLUMN_TIMESTAMP + " >= ? AND " +
                          PermissionDatabase.COLUMN_TIMESTAMP + " <= ?";
        String[] selectionArgs = {permissionName, String.valueOf(startTime), String.valueOf(endTime)};
        
        String orderBy = PermissionDatabase.COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = database.query(
                PermissionDatabase.TABLE_PERMISSION_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        
        return records;
    }

    public int deleteRecordsBefore(long beforeTime) {
        String whereClause = PermissionDatabase.COLUMN_TIMESTAMP + " < ?";
        String[] whereArgs = {String.valueOf(beforeTime)};
        
        return database.delete(PermissionDatabase.TABLE_PERMISSION_RECORDS, whereClause, whereArgs);
    }

    public long getRecordCount() {
        String query = "SELECT COUNT(*) FROM " + PermissionDatabase.TABLE_PERMISSION_RECORDS;
        Cursor cursor = database.rawQuery(query, null);
        
        long count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
            cursor.close();
        }
        
        return count;
    }

    public long getEarliestRecordTime() {
        String query = "SELECT MIN(" + PermissionDatabase.COLUMN_TIMESTAMP + ") FROM " + 
                       PermissionDatabase.TABLE_PERMISSION_RECORDS;
        Cursor cursor = database.rawQuery(query, null);
        
        long time = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                time = cursor.getLong(0);
            }
            cursor.close();
        }
        
        return time;
    }

    public long getLatestRecordTime() {
        String query = "SELECT MAX(" + PermissionDatabase.COLUMN_TIMESTAMP + ") FROM " + 
                       PermissionDatabase.TABLE_PERMISSION_RECORDS;
        Cursor cursor = database.rawQuery(query, null);
        
        long time = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                time = cursor.getLong(0);
            }
            cursor.close();
        }
        
        return time;
    }

    private PermissionRecord cursorToRecord(Cursor cursor) {
        PermissionRecord record = new PermissionRecord();
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_ID)));
        record.setPackageName(cursor.getString(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_PACKAGE_NAME)));
        record.setPermissionName(cursor.getString(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_PERMISSION_NAME)));
        record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_TIMESTAMP)));
        record.setBlocked(cursor.getInt(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_IS_BLOCKED)) == 1);
        record.setExtraInfo(cursor.getString(cursor.getColumnIndexOrThrow(PermissionDatabase.COLUMN_EXTRA_INFO)));
        return record;
    }
}
