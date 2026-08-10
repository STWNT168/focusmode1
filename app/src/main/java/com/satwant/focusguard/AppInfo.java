package com.satwant.focusguard;

public class AppInfo {
    public String label;
    public String packageName;
    public boolean blocked;

    public AppInfo(String label, String packageName, boolean blocked) {
        this.label = label;
        this.packageName = packageName;
        this.blocked = blocked;
    }
}
