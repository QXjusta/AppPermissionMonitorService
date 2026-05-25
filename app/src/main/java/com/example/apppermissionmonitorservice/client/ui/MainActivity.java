package com.example.apppermissionmonitorservice.client.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.server.permissionmonitor.IPermissionCallback;
import com.android.server.permissionmonitor.IPermissionMonitor;
import com.android.server.permissionmonitor.PermissionRecord;
import com.example.apppermissionmonitorservice.R;
import com.example.apppermissionmonitorservice.client.adapter.AppListAdapter;
import com.example.apppermissionmonitorservice.utils.LogUtils;
import com.example.apppermissionmonitorservice.utils.PermissionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SERVICE_NAME = "permission_monitor";
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(最早记录|最新记录):\\s*(\\d{10,})");
    private static final String NOTIFICATION_CHANNEL_ID = "permission_monitor_channel";
    private static final String PREFS_NAME = "PermissionMonitorPrefs";
    private static final String KEY_SELECTED_PERMISSIONS = "selected_permissions";

    private TextView mStatusText;
    private Button mConnectButton;
    private Button mRefreshButton;
    private Button mSettingsButton;
    private Button mStatsButton;
    private RecyclerView mAppListRecyclerView;
    private AppListAdapter mAppListAdapter;
    private View mLiveEventCard;
    private TextView mLiveEventText;

    private IPermissionMonitor mPermissionMonitor;
    private boolean mIsConnected = false;
    private Handler mMainHandler;
    private PermissionCallbackImpl mCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        initViews();
        setupRecyclerView();
        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectToService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterCallback();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "权限监控通知";
            String description = "权限使用实时提示";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initViews() {
        mStatusText = findViewById(R.id.status_text);
        mConnectButton = findViewById(R.id.connect_button);
        mRefreshButton = findViewById(R.id.refresh_button);
        mSettingsButton = findViewById(R.id.settings_button);
        mStatsButton = findViewById(R.id.stats_button);
        mAppListRecyclerView = findViewById(R.id.app_list_recycler_view);
        mLiveEventCard = findViewById(R.id.live_event_card);
        mLiveEventText = findViewById(R.id.live_event_text);

        mConnectButton.setOnClickListener(v -> {
            if (mIsConnected) {
                disconnectService();
            } else {
                connectToService();
            }
        });

        mRefreshButton.setOnClickListener(v -> {
            if (mIsConnected) {
                loadAppList();
            } else {
                Toast.makeText(this, "请先连接服务", Toast.LENGTH_SHORT).show();
            }
        });

        mSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        mStatsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        mAppListAdapter = new AppListAdapter(new ArrayList<>(), appInfo -> {
            Intent intent = new Intent(this, PermissionDetailActivity.class);
            intent.putExtra("package_name", appInfo.getPackageName());
            intent.putExtra("app_name", appInfo.getAppName());
            startActivity(intent);
        });

        mAppListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAppListRecyclerView.setAdapter(mAppListAdapter);
        mAppListRecyclerView.setNestedScrollingEnabled(true);
    }

    private void updateUI() {
        if (mIsConnected) {
            mConnectButton.setText("断开连接");
            mRefreshButton.setEnabled(true);
            mStatsButton.setEnabled(true);
        } else {
            mConnectButton.setText("连接服务");
            mRefreshButton.setEnabled(false);
            mStatsButton.setEnabled(false);
            mStatusText.setText("服务状态: 未连接");
        }
    }

    private void connectToService() {
        new Thread(() -> {
            try {
                IBinder binder = getServiceBinder(SERVICE_NAME);
                if (binder != null) {
                    mPermissionMonitor = IPermissionMonitor.Stub.asInterface(binder);
                    mIsConnected = true;

                    registerCallback();

                    mMainHandler.post(() -> {
                        updateUI();
                        loadServiceStatus();
                        loadAppList();
                    });
                } else {
                    mMainHandler.post(() -> {
                        mStatusText.setText("服务状态: 服务未找到\n请确保系统服务已启动");
                        Toast.makeText(this, "服务未找到，请确保系统服务已启动", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (SecurityException e) {
                LogUtils.e(TAG, "SecurityException: " + e.getMessage());
                mMainHandler.post(() -> {
                    mStatusText.setText("服务状态: 权限不足\n" + e.getMessage());
                    Toast.makeText(this, "权限不足，无法访问服务", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to connect to service", e);
                mMainHandler.post(() -> {
                    mStatusText.setText("服务状态: 连接失败\n" + e.getMessage());
                    Toast.makeText(this, "连接服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private IBinder getServiceBinder(String serviceName) throws Exception {
        Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        return (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, serviceName);
    }

    private void disconnectService() {
        unregisterCallback();
        mPermissionMonitor = null;
        mIsConnected = false;
        updateUI();
        mStatusText.setText("服务状态: 已断开");
        mAppListAdapter.clearAppList();
        mLiveEventCard.setVisibility(View.GONE);
        Toast.makeText(this, "服务已断开", Toast.LENGTH_SHORT).show();
    }

    private void registerCallback() {
        if (mPermissionMonitor == null) return;

        try {
            mCallback = new PermissionCallbackImpl(mMainHandler, new PermissionCallbackImpl.CallbackListener() {
                @Override
                public void onPermissionUsed(PermissionRecord record) {
                    LogUtils.d(TAG, "Permission used: " + record.getPermissionName());
                    handleNewPermissionEvent(record);
                }

                @Override
                public void onServiceStatusChanged(String status) {
                    LogUtils.d(TAG, "Service status changed: " + status);
                }
            });

            mPermissionMonitor.registerCallback(mCallback);
            LogUtils.d(TAG, "Callback registered");
        } catch (RemoteException e) {
            LogUtils.e(TAG, "Failed to register callback", e);
        }
    }

    private void unregisterCallback() {
        if (mPermissionMonitor != null && mCallback != null) {
            try {
                mPermissionMonitor.unregisterCallback(mCallback);
                LogUtils.d(TAG, "Callback unregistered");
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to unregister callback", e);
            }
        }
        mCallback = null;
    }

    private void handleNewPermissionEvent(PermissionRecord record) {
        if (!isPermissionSelected(record.getPermissionName())) {
            return;
        }
        
        String appName = getAppName(record.getPackageName());
        String permissionName = PermissionUtils.getFriendlyPermissionName(record.getPermissionName());
        String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(record.getTimestamp()));

        String eventText = String.format(Locale.getDefault(), "%s 在 %s 使用了 %s", appName, timeStr, permissionName);

        mLiveEventText.setText(eventText);
        mLiveEventCard.setVisibility(View.VISIBLE);

        Toast.makeText(this, eventText, Toast.LENGTH_SHORT).show();

        sendNotification(appName, permissionName);

        loadAppList();
    }
    
    private boolean isPermissionSelected(String permissionName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> selectedPermissions = prefs.getStringSet(KEY_SELECTED_PERMISSIONS, null);
        
        if (selectedPermissions == null || selectedPermissions.isEmpty()) {
            return true;
        }
        
        return selectedPermissions.contains(permissionName);
    }

    private void sendNotification(String appName, String permissionName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("权限使用提示")
                .setContentText(appName + " 使用了 " + permissionName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void loadServiceStatus() {
        if (mPermissionMonitor == null) return;

        try {
            String status = mPermissionMonitor.getServiceStatus();
            String formattedStatus = formatStatusForDisplay(status);
            LogUtils.d(TAG, "Service status: " + formattedStatus);
            mStatusText.setText("服务状态: 已连接\n" + formattedStatus);
        } catch (RemoteException e) {
            LogUtils.e(TAG, "Failed to get service status", e);
            mStatusText.setText("服务状态: 已连接（获取状态失败）");
        }
    }

    private String formatStatusForDisplay(String status) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(status);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + ": " + formatTimestampSafely(matcher.group(2));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String formatTimestampSafely(String value) {
        try {
            long timestamp = Long.parseLong(value);
            if (timestamp <= 0) {
                return "无";
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(timestamp));
        } catch (Exception e) {
            return value;
        }
    }

    private void loadAppList() {
        if (!mIsConnected || mPermissionMonitor == null) {
            Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                long endTime = System.currentTimeMillis();
                long startTime = endTime - (24 * 60 * 60 * 1000);

                List<PermissionRecord> records = mPermissionMonitor.getAllRecords(startTime, endTime);

                Set<String> packageSet = new HashSet<>();
                List<AppListAdapter.AppInfo> appList = new ArrayList<>();

                for (PermissionRecord record : records) {
                    if (!isPermissionSelected(record.getPermissionName())) {
                        continue;
                    }
                    
                    String packageName = record.getPackageName();
                    if (!packageSet.contains(packageName)) {
                        packageSet.add(packageName);
                        String appName = getAppName(packageName);
                        appList.add(new AppListAdapter.AppInfo(packageName, appName));
                    }
                }

                mMainHandler.post(() -> {
                    mAppListAdapter.updateAppList(appList);
                    if (appList.isEmpty()) {
                        Toast.makeText(MainActivity.this, "没有找到权限记录", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "找到 " + appList.size() + " 个应用", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to load app list", e);
                mMainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "加载应用列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private static class PermissionCallbackImpl extends IPermissionCallback.Stub {
        private final Handler mainHandler;
        private final CallbackListener listener;

        public interface CallbackListener {
            void onPermissionUsed(PermissionRecord record);
            void onServiceStatusChanged(String status);
        }

        public PermissionCallbackImpl(Handler mainHandler, CallbackListener listener) {
            this.mainHandler = mainHandler;
            this.listener = listener;
        }

        @Override
        public void onPermissionUsed(PermissionRecord record) {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onPermissionUsed(record);
                }
            });
        }

        @Override
        public void onServiceStatusChanged(String status) {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onServiceStatusChanged(status);
                }
            });
        }
    }
}
