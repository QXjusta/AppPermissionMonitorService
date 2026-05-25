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
 * 权限记录适配器
 */
public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {
    
    public static class PermissionItem {
        private final String permissionName;
        private final String permissionCode;
        private final String category;
        private final String time;
        private final boolean isBlocked;
        
        public PermissionItem(String permissionName, String permissionCode, String category, String time, boolean isBlocked) {
            this.permissionName = permissionName;
            this.permissionCode = permissionCode;
            this.category = category;
            this.time = time;
            this.isBlocked = isBlocked;
        }
        
        public String getPermissionName() {
            return permissionName;
        }
        
        public String getPermissionCode() {
            return permissionCode;
        }
        
        public String getCategory() {
            return category;
        }
        
        public String getTime() {
            return time;
        }
        
        public boolean isBlocked() {
            return isBlocked;
        }
    }
    
    private List<PermissionItem> permissionList;
    
    public PermissionAdapter(List<PermissionItem> permissionList) {
        this.permissionList = permissionList;
    }
    
    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission_record, parent, false);
        return new PermissionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        PermissionItem item = permissionList.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return permissionList.size();
    }
    
    /**
     * 更新权限列表
     */
    public void updatePermissionList(List<PermissionItem> newPermissionList) {
        this.permissionList = newPermissionList;
        notifyDataSetChanged();
    }
    
    /**
     * 清空权限列表
     */
    public void clearPermissionList() {
        this.permissionList.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 添加权限记录
     */
    public void addPermissionItem(PermissionItem item) {
        this.permissionList.add(item);
        notifyItemInserted(this.permissionList.size() - 1);
    }
    
    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        private final TextView permissionNameTextView;
        private final TextView permissionCodeTextView;
        private final TextView categoryTextView;
        private final TextView timeTextView;
        private final TextView statusTextView;
        
        public PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            permissionNameTextView = itemView.findViewById(R.id.permission_name_text_view);
            permissionCodeTextView = itemView.findViewById(R.id.permission_code_text_view);
            categoryTextView = itemView.findViewById(R.id.category_text_view);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);
        }
        
        public void bind(PermissionItem item) {
            permissionNameTextView.setText(item.getPermissionName());
            permissionCodeTextView.setText(item.getPermissionCode());
            categoryTextView.setText("分类: " + item.getCategory());
            timeTextView.setText("时间: " + item.getTime());
            
            if (item.isBlocked()) {
                statusTextView.setText("状态: 已拦截");
                statusTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            } else {
                statusTextView.setText("状态: 已允许");
                statusTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            }
        }
    }
}