package com.example.apppermissionmonitorservice.client.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.server.permissionmonitor.IPermissionMonitor;
import com.android.server.permissionmonitor.PermissionRecord;
import com.example.apppermissionmonitorservice.R;
import com.example.apppermissionmonitorservice.utils.LogUtils;
import com.example.apppermissionmonitorservice.utils.PermissionUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private static final String SERVICE_NAME = "permission_monitor";
    private static final String PREFS_NAME = "PermissionMonitorPrefs";
    private static final String KEY_SELECTED_PERMISSIONS = "selected_permissions";

    private Button refreshStatsButton;
    private Button backStatsButton;
    private BarChart barChart;

    private IPermissionMonitor permissionMonitor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        connectToService();
    }

    private void initViews() {
        refreshStatsButton = findViewById(R.id.refresh_stats_button);
        backStatsButton = findViewById(R.id.back_stats_button);
        barChart = findViewById(R.id.bar_chart);

        refreshStatsButton.setOnClickListener(v -> loadStatsData());
        backStatsButton.setOnClickListener(v -> finish());

        setupBarChart();
    }

    private void setupBarChart() {
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void connectToService() {
        new Thread(() -> {
            try {
                IBinder binder = getServiceBinder(SERVICE_NAME);
                if (binder != null) {
                    permissionMonitor = IPermissionMonitor.Stub.asInterface(binder);
                    mainHandler.post(() -> {
                        loadStatsData();
                    });
                }
            } catch (SecurityException e) {
                LogUtils.e(TAG, "SecurityException: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to connect to service", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "连接服务失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private IBinder getServiceBinder(String serviceName) throws Exception {
        Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        return (IBinder) serviceManagerClass.getMethod("getService", String.class)
                .invoke(null, serviceName);
    }

    private void loadStatsData() {
        new Thread(() -> {
            try {
                if (permissionMonitor == null) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "服务未连接", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                long endTime = System.currentTimeMillis();
                long startTime = endTime - (7 * 24 * 60 * 60 * 1000L);
                List<PermissionRecord> records = permissionMonitor.getAllRecords(startTime, endTime);

                Map<String, Integer> permissionCountMap = new HashMap<>();
                for (PermissionRecord record : records) {
                    if (!isPermissionSelected(record.getPermissionName())) {
                        continue;
                    }
                    
                    String permissionName = record.getPermissionName();
                    permissionCountMap.put(permissionName, permissionCountMap.getOrDefault(permissionName, 0) + 1);
                }

                mainHandler.post(() -> {
                    updateBarChart(permissionCountMap);
                    Toast.makeText(this, "统计数据已刷新", Toast.LENGTH_SHORT).show();
                });
            } catch (RemoteException e) {
                LogUtils.e(TAG, "Failed to load stats data", e);
                mainHandler.post(() -> {
                    Toast.makeText(this, "加载统计数据失败", Toast.LENGTH_SHORT).show();
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

    private void updateBarChart(Map<String, Integer> permissionCountMap) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : permissionCountMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(PermissionUtils.getFriendlyPermissionName(entry.getKey()));
            index++;
        }

        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add("暂无数据");
        }

        BarDataSet dataSet = new BarDataSet(entries, "权限使用次数");
        dataSet.setColor(Color.parseColor("#FF7043"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());

        barChart.invalidate();
    }
}
