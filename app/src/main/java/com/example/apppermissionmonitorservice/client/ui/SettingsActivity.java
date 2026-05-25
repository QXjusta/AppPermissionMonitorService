package com.example.apppermissionmonitorservice.client.ui;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.server.permissionmonitor.IPermissionMonitor;
import com.example.apppermissionmonitorservice.R;
import com.example.apppermissionmonitorservice.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final String SERVICE_NAME = "permission_monitor";
    private static final String PREFS_NAME = "PermissionMonitorPrefs";
    private static final String KEY_RETENTION_DAYS = "retention_days";
    private static final String KEY_SELECTED_PERMISSIONS = "selected_permissions";
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(最早记录|最新记录):\\s*(\\d{10,})");

    private TextView serviceStatusText;
    private Button refreshButton;
    private Button saveSettingsButton;
    private Button backButton;
    private Button clearRecordsButton;
    private RadioGroup retentionRadioGroup;
    private RadioButton retention3Days;
    private RadioButton retention7Days;
    private RadioButton retention30Days;
    private CheckBox checkCamera;
    private CheckBox checkRecordAudio;
    private CheckBox checkFineLocation;
    private CheckBox checkCoarseLocation;
    private CheckBox checkReadContacts;
    private CheckBox checkWriteContacts;
    private CheckBox checkReadSms;
    private CheckBox checkWriteSms;
    private CheckBox checkReadCalendar;
    private CheckBox checkWriteCalendar;

    private IPermissionMonitor permissionMonitor;
    private Handler mainHandler;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mainHandler = new Handler(Looper.getMainLooper());
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        loadSavedSettings();
        connectToService();
    }

    private void initViews() {
        serviceStatusText = findViewById(R.id.service_status_text);
        refreshButton = findViewById(R.id.refresh_button);
        saveSettingsButton = findViewById(R.id.save_settings_button);
        backButton = findViewById(R.id.back_button);
        clearRecordsButton = findViewById(R.id.clear_records_button);
        retentionRadioGroup = findViewById(R.id.retention_radio_group);
        retention3Days = findViewById(R.id.retention_3_days);
        retention7Days = findViewById(R.id.retention_7_days);
        retention30Days = findViewById(R.id.retention_30_days);
        checkCamera = findViewById(R.id.check_camera);
        checkRecordAudio = findViewById(R.id.check_record_audio);
        checkFineLocation = findViewById(R.id.check_fine_location);
        checkCoarseLocation = findViewById(R.id.check_coarse_location);
        checkReadContacts = findViewById(R.id.check_read_contacts);
        checkWriteContacts = findViewById(R.id.check_write_contacts);
        checkReadSms = findViewById(R.id.check_read_sms);
        checkWriteSms = findViewById(R.id.check_write_sms);
        checkReadCalendar = findViewById(R.id.check_read_calendar);
        checkWriteCalendar = findViewById(R.id.check_write_calendar);

        refreshButton.setOnClickListener(v -> loadServiceInfo());
        saveSettingsButton.setOnClickListener(v -> saveSettings());
        backButton.setOnClickListener(v -> finish());
        clearRecordsButton.setOnClickListener(v -> clearOldRecords());
    }

    private void loadSavedSettings() {
        int savedDays = sharedPreferences.getInt(KEY_RETENTION_DAYS, 7);
        switch (savedDays) {
            case 3:
                retention3Days.setChecked(true);
                break;
            case 30:
                retention30Days.setChecked(true);
                break;
            case 7:
            default:
                retention7Days.setChecked(true);
                break;
        }

        Set<String> savedPermissions = sharedPreferences.getStringSet(KEY_SELECTED_PERMISSIONS, null);
        if (savedPermissions != null) {
            checkCamera.setChecked(savedPermissions.contains(Manifest.permission.CAMERA));
            checkRecordAudio.setChecked(savedPermissions.contains(Manifest.permission.RECORD_AUDIO));
            checkFineLocation.setChecked(savedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION));
            checkCoarseLocation.setChecked(savedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION));
            checkReadContacts.setChecked(savedPermissions.contains(Manifest.permission.READ_CONTACTS));
            checkWriteContacts.setChecked(savedPermissions.contains("android.permission.WRITE_CONTACTS"));
            checkReadSms.setChecked(savedPermissions.contains(Manifest.permission.READ_SMS));
            checkWriteSms.setChecked(savedPermissions.contains("android.permission.WRITE_SMS"));
            checkReadCalendar.setChecked(savedPermissions.contains("android.permission.READ_CALENDAR"));
            checkWriteCalendar.setChecked(savedPermissions.contains("android.permission.WRITE_CALENDAR"));
        } else {
            checkCamera.setChecked(true);
            checkRecordAudio.setChecked(true);
            checkFineLocation.setChecked(true);
            checkCoarseLocation.setChecked(true);
            checkReadContacts.setChecked(true);
            checkWriteContacts.setChecked(true);
            checkReadSms.setChecked(true);
            checkWriteSms.setChecked(true);
            checkReadCalendar.setChecked(true);
            checkWriteCalendar.setChecked(true);
        }
    }

    private void saveSettings() {
        int days;
        if (retention3Days.isChecked()) {
            days = 3;
        } else if (retention30Days.isChecked()) {
            days = 30;
        } else {
            days = 7;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_RETENTION_DAYS, days);
        editor.apply();

        saveMonitoredPermissions();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private void saveMonitoredPermissions() {
        List<String> permissions = new ArrayList<>();
        Set<String> permissionSet = new HashSet<>();
        
        if (checkCamera.isChecked()) {
            permissions.add(Manifest.permission.CAMERA);
            permissionSet.add(Manifest.permission.CAMERA);
        }
        if (checkRecordAudio.isChecked()) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
            permissionSet.add(Manifest.permission.RECORD_AUDIO);
        }
        if (checkFineLocation.isChecked()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionSet.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkCoarseLocation.isChecked()) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionSet.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (checkReadContacts.isChecked()) {
            permissions.add(Manifest.permission.READ_CONTACTS);
            permissionSet.add(Manifest.permission.READ_CONTACTS);
        }
        if (checkWriteContacts.isChecked()) {
            permissions.add("android.permission.WRITE_CONTACTS");
            permissionSet.add("android.permission.WRITE_CONTACTS");
        }
        if (checkReadSms.isChecked()) {
            permissions.add(Manifest.permission.READ_SMS);
            permissionSet.add(Manifest.permission.READ_SMS);
        }
        if (checkWriteSms.isChecked()) {
            permissions.add("android.permission.WRITE_SMS");
            permissionSet.add("android.permission.WRITE_SMS");
        }
        if (checkReadCalendar.isChecked()) {
            permissions.add("android.permission.READ_CALENDAR");
            permissionSet.add("android.permission.READ_CALENDAR");
        }
        if (checkWriteCalendar.isChecked()) {
            permissions.add("android.permission.WRITE_CALENDAR");
            permissionSet.add("android.permission.WRITE_CALENDAR");
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_SELECTED_PERMISSIONS, permissionSet);
        editor.apply();

        if (permissionMonitor != null) {
            try {
                permissionMonitor.setMonitoredPermissions(permissions);
                Toast.makeText(this, "监控权限配置已保存到服务", Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to set monitored permissions", e);
                Toast.makeText(this, "无法保存监控权限到服务", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMonitoredPermissions() {
        if (permissionMonitor == null) return;

        new Thread(() -> {
            try {
                List<String> permissions = permissionMonitor.getMonitoredPermissions();
                mainHandler.post(() -> {
                    for (String p : permissions) {
                        if (Manifest.permission.CAMERA.equals(p)) {
                            checkCamera.setChecked(true);
                        } else if (Manifest.permission.RECORD_AUDIO.equals(p)) {
                            checkRecordAudio.setChecked(true);
                        } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(p)) {
                            checkFineLocation.setChecked(true);
                        } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(p)) {
                            checkCoarseLocation.setChecked(true);
                        } else if (Manifest.permission.READ_CONTACTS.equals(p)) {
                            checkReadContacts.setChecked(true);
                        } else if ("android.permission.WRITE_CONTACTS".equals(p)) {
                            checkWriteContacts.setChecked(true);
                        } else if (Manifest.permission.READ_SMS.equals(p)) {
                            checkReadSms.setChecked(true);
                        } else if ("android.permission.WRITE_SMS".equals(p)) {
                            checkWriteSms.setChecked(true);
                        } else if ("android.permission.READ_CALENDAR".equals(p)) {
                            checkReadCalendar.setChecked(true);
                        } else if ("android.permission.WRITE_CALENDAR".equals(p)) {
                            checkWriteCalendar.setChecked(true);
                        }
                    }
                });
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to get monitored permissions", e);
            }
        }).start();
    }

    private void connectToService() {
        new Thread(() -> {
            try {
                IBinder binder = getServiceBinder(SERVICE_NAME);
                if (binder != null) {
                    permissionMonitor = IPermissionMonitor.Stub.asInterface(binder);
                    mainHandler.post(() -> {
                        loadServiceInfo();
                        loadMonitoredPermissions();
                    });
                }
            } catch (SecurityException e) {
                LogUtils.e(TAG, "SecurityException: " + e.getMessage());
                mainHandler.post(() -> {
                    serviceStatusText.setText("服务状态: 权限不足\n" + e.getMessage());
                });
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to connect to service", e);
                mainHandler.post(() -> {
                    serviceStatusText.setText("服务状态: 连接失败\n" + e.getMessage());
                });
            }
        }).start();
    }

    private IBinder getServiceBinder(String serviceName) throws Exception {
        Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        return (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, serviceName);
    }

    private void loadServiceInfo() {
        new Thread(() -> {
            try {
                if (permissionMonitor != null) {
                    String status = permissionMonitor.getServiceStatus();
                    String formattedStatus = formatStatusForDisplay(status);
                    mainHandler.post(() -> {
                        serviceStatusText.setText("服务状态: 已连接\n" + formattedStatus);
                    });
                }
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to get service info", e);
                mainHandler.post(() -> {
                    serviceStatusText.setText("服务状态: 已连接（获取信息失败）");
                });
            }
        }).start();
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

    private void clearOldRecords() {
        int days = sharedPreferences.getInt(KEY_RETENTION_DAYS, 7);
        new Thread(() -> {
            try {
                if (permissionMonitor != null) {
                    long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
                    boolean success = permissionMonitor.clearRecords(cutoffTime);
                    mainHandler.post(() -> {
                        if (success) {
                            Toast.makeText(this, "清理成功（保留" + days + "天）", Toast.LENGTH_SHORT).show();
                            loadServiceInfo();
                        } else {
                            Toast.makeText(this, "没有可清理的记录", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to clear records", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "清理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
