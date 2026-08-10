package com.satwant.focusguard;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class FocusAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String packageName = pkg.toString();

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        boolean active = prefs.getBoolean(MainActivity.KEY_SESSION_ACTIVE, false);
        long endTime = prefs.getLong(MainActivity.KEY_SESSION_END, 0);

        if (!active || System.currentTimeMillis() > endTime) return;

        Set<String> blocked = prefs.getStringSet(MainActivity.KEY_BLOCKED_APPS, new HashSet<>());
        if (blocked.contains(packageName)) {
            Intent intent = new Intent(this, BlockedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    public void onInterrupt() {
        // no-op
    }
}
