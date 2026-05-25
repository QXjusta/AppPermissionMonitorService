package com.example.apppermissionmonitorservice.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apppermissionmonitorservice.R;

import java.util.List;

/**
 * 应用列表适配器
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {
    
    public static class AppInfo {
        private final String packageName;
        private final String appName;
        
        public AppInfo(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public String getAppName() {
            return appName;
        }
    }
    
    public interface OnAppItemClickListener {
        void onAppItemClick(AppInfo appInfo);
    }
    
    private List<AppInfo> appList;
    private final OnAppItemClickListener listener;
    
    public AppListAdapter(List<AppInfo> appList, OnAppItemClickListener listener) {
        this.appList = appList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list, parent, false);
        return new AppViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.bind(appInfo);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppItemClick(appInfo);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return appList.size();
    }
    
    /**
     * 更新应用列表
     */
    public void updateAppList(List<AppInfo> newAppList) {
        this.appList = newAppList;
        notifyDataSetChanged();
    }
    
    /**
     * 清空应用列表
     */
    public void clearAppList() {
        this.appList.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 添加应用
     */
    public void addApp(AppInfo appInfo) {
        this.appList.add(appInfo);
        notifyItemInserted(this.appList.size() - 1);
    }
    
    static class AppViewHolder extends RecyclerView.ViewHolder {
        private final TextView appNameTextView;
        private final TextView packageNameTextView;
        
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(R.id.app_name_text_view);
            packageNameTextView = itemView.findViewById(R.id.package_name_text_view);
        }
        
        public void bind(AppInfo appInfo) {
            appNameTextView.setText(appInfo.getAppName());
            packageNameTextView.setText(appInfo.getPackageName());
        }
    }
}