package com.android.server.permissionmonitor;

import android.os.Parcel;
import android.os.Parcelable;

public class PermissionRecord implements Parcelable {
    private long id;
    private String packageName;
    private String permissionName;
    private long timestamp;
    private boolean isBlocked;
    private String extraInfo;

    public PermissionRecord() {
    }

    public PermissionRecord(String packageName, String permissionName, long timestamp) {
        this.packageName = packageName;
        this.permissionName = permissionName;
        this.timestamp = timestamp;
        this.isBlocked = false;
        this.extraInfo = "";
    }

    protected PermissionRecord(Parcel in) {
        id = in.readLong();
        packageName = in.readString();
        permissionName = in.readString();
        timestamp = in.readLong();
        isBlocked = in.readByte() != 0;
        extraInfo = in.readString();
    }

    public static final Creator<PermissionRecord> CREATOR = new Creator<PermissionRecord>() {
        @Override
        public PermissionRecord createFromParcel(Parcel in) {
            return new PermissionRecord(in);
        }

        @Override
        public PermissionRecord[] newArray(int size) {
            return new PermissionRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(packageName);
        dest.writeString(permissionName);
        dest.writeLong(timestamp);
        dest.writeByte((byte) (isBlocked ? 1 : 0));
        dest.writeString(extraInfo);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public String toString() {
        return "PermissionRecord{" +
                "id=" + id +
                ", packageName='" + packageName + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", timestamp=" + timestamp +
                ", isBlocked=" + isBlocked +
                ", extraInfo='" + extraInfo + '\'' +
                '}';
    }
}
