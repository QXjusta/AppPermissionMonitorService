package com.example.apppermissionmonitorservice.client.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.server.permissionmonitor.IPermissionMonitor;
import com.android.server.permissionmonitor.PermissionRecord;
import com.example.apppermissionmonitorservice.R;
import com.example.apppermissionmonitorservice.client.adapter.PermissionAdapter;
import com.example.apppermissionmonitorservice.utils.LogUtils;
import com.example.apppermissionmonitorservice.utils.PermissionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PermissionDetailActivity extends AppCompatActivity {
    private static final String TAG = "PermissionDetailActivity";
    private static final String SERVICE_NAME = "permission_monitor";
    private static final String PREFS_NAME = "PermissionMonitorPrefs";
    private static final String KEY_SELECTED_PERMISSIONS = "selected_permissions";
    
    private TextView mAppNameText;
    private TextView mPackageNameText;
    private TextView mRecordCountText;
    private Button mRefreshButton;
    private Button mBackButton;
    private RecyclerView mPermissionRecyclerView;
    private PermissionAdapter mPermissionAdapter;
    
    private String mPackageName;
    private String mAppName;
    
    private IPermissionMonitor mPermissionMonitor;
    private Handler mMainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_detail);
        
        mMainHandler = new Handler(Looper.getMainLooper());
        
        Intent intent = getIntent();
        mPackageName = intent.getStringExtra("package_name");
        mAppName = intent.getStringExtra("app_name");
        
        if (mPackageName == null) {
            Toast.makeText(this, "无效的应用包名", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupRecyclerView();
        updateAppInfo();
        connectToService();
    }
    
    private void initViews() {
        mAppNameText = findViewById(R.id.app_name_text);
        mPackageNameText = findViewById(R.id.package_name_text);
        mRecordCountText = findViewById(R.id.record_count_text);
        mRefreshButton = findViewById(R.id.refresh_button);
        mBackButton = findViewById(R.id.back_button);
        mPermissionRecyclerView = findViewById(R.id.permission_recycler_view);
        
        mRefreshButton.setOnClickListener(v -> loadPermissionRecords());
        mBackButton.setOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        mPermissionAdapter = new PermissionAdapter(new ArrayList<>());
        mPermissionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mPermissionRecyclerView.setAdapter(mPermissionAdapter);
    }
    
    private void updateAppInfo() {
        mAppNameText.setText(mAppName != null ? mAppName : "未知应用");
        mPackageNameText.setText(mPackageName);
        mRecordCountText.setText("加载中...");
    }
    
    private void connectToService() {
        new Thread(() -> {
            try {
                IBinder binder = getServiceBinder(SERVICE_NAME);
                if (binder != null) {
                    mPermissionMonitor = IPermissionMonitor.Stub.asInterface(binder);
                    mMainHandler.post(this::loadPermissionRecords);
                } else {
                    mMainHandler.post(() -> {
                        Toast.makeText(this, "服务未找到", Toast.LENGTH_SHORT).show();
                        mRecordCountText.setText("服务未连接");
                    });
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to connect to service", e);
                mMainHandler.post(() -> {
                    Toast.makeText(this, "连接服务失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mRecordCountText.setText("连接失败");
                });
            }
        }).start();
    }

    private IBinder getServiceBinder(String serviceName) throws Exception {
        Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        return (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, serviceName);
    }
    
    private void loadPermissionRecords() {
        if (mPermissionMonitor == null) {
            Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                long endTime = System.currentTimeMillis();
                long startTime = endTime - (7 * 24 * 60 * 60 * 1000);
                
                List<PermissionRecord> records = mPermissionMonitor.getRecords(mPackageName, startTime, endTime);
                
                List<PermissionAdapter.PermissionItem> permissionItems = new ArrayList<>();
                for (PermissionRecord record : records) {
                    if (!isPermissionSelected(record.getPermissionName())) {
                        continue;
                    }
                    
                    String permissionName = record.getPermissionName();
                    String friendlyName = PermissionUtils.getFriendlyPermissionName(permissionName);
                    String category = PermissionUtils.getPermissionCategory(permissionName);
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String time = sdf.format(new Date(record.getTimestamp()));
                    
                    permissionItems.add(new PermissionAdapter.PermissionItem(
                            friendlyName,
                            permissionName,
                            category,
                            time,
                            record.isBlocked()
                    ));
                }
                
                mMainHandler.post(() -> {
                    mPermissionAdapter.updatePermissionList(permissionItems);
                    mRecordCountText.setText("记录数量: " + permissionItems.size());
                    
                    if (permissionItems.isEmpty()) {
                        Toast.makeText(PermissionDetailActivity.this, "没有找到权限记录", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to load permission records", e);
                mMainHandler.post(() -> {
                    Toast.makeText(PermissionDetailActivity.this, "加载权限记录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mRecordCountText.setText("加载失败");
                });
            }
        }).start();
    }
    
    private boolean isPermissionSelected(String permissionName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> selectedPermissions = prefs.getStringSet(KEY_SELECTED_PERMISSIONS, null);
        
        if (selectedPermissions == null || selectedPermissions.isEmpty()) {
            return true;
        }
        
        return selectedPermissions.contains(permissionName);
    }
}
