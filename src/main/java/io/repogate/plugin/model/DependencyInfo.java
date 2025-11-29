package io.repogate.plugin.model;

import java.util.Objects;

public class DependencyInfo {
    private final String packageName;
    private final String packageManager;
    private final String version;
    private final String filePath;
    private ApprovalStatus status;

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        DENIED,
        SCANNING,
        NOT_FOUND,
        ERROR
    }

    public DependencyInfo(String packageName, String packageManager, String version) {
        this(packageName, packageManager, version, "");
    }
    
    public DependencyInfo(String packageName, String packageManager, String version, String filePath) {
        this.packageName = packageName;
        this.packageManager = packageManager;
        this.version = version;
        this.filePath = filePath != null ? filePath : "";
        this.status = ApprovalStatus.PENDING;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public String getVersion() {
        return version;
    }
    
    public String getFilePath() {
        return filePath;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyInfo that = (DependencyInfo) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(packageManager, that.packageManager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, packageManager);
    }

    @Override
    public String toString() {
        return "DependencyInfo{" +
                "packageName='" + packageName + '\'' +
                ", packageManager='" + packageManager + '\'' +
                ", version='" + version + '\'' +
                ", status=" + status +
                '}';
    }
}
