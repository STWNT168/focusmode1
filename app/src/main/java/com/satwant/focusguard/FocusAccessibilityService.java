package com.satwant.focusguard;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class FocusAccessibilityService
        extends AccessibilityService {

    private String lastBlockedPackage = null;
    private long lastBlockedLaunchTime = 0L;

    /*
     * Prevent repeatedly launching BlockedActivity when Android
     * sends multiple accessibility/window events for the same app.
     */
    private static final long BLOCK_SCREEN_COOLDOWN_MS = 1000L;

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        if (event.getEventType()
                != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            return;
        }

        CharSequence pkg =
                event.getPackageName();

        if (pkg == null) {
            return;
        }

        String packageName =
                pkg.toString();

        /*
         * Don't block FocusGuard itself.
         */
        if (packageName.equals(getPackageName())) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        MainActivity.PREFS,
                        MODE_PRIVATE
                );

        boolean active =
                prefs.getBoolean(
                        MainActivity.KEY_SESSION_ACTIVE,
                        false
                );

        long endTime =
                prefs.getLong(
                        MainActivity.KEY_SESSION_END,
                        0
                );

        if (!active) {
            resetLastBlockedApp();
            return;
        }

        /*
         * Automatically clean up an expired session.
         */
        if (System.currentTimeMillis() >= endTime) {

            prefs.edit()
                    .putBoolean(
                            MainActivity.KEY_SESSION_ACTIVE,
                            false
                    )
                    .putBoolean(
                            MainActivity.KEY_COMMITMENT_MODE,
                            false
                    )
                    .apply();

            resetLastBlockedApp();
            return;
        }

        Set<String> blocked =
                prefs.getStringSet(
                        MainActivity.KEY_BLOCKED_APPS,
                        new HashSet<>()
                );

        if (!blocked.contains(packageName)) {
            return;
        }

        long now =
                System.currentTimeMillis();

        /*
         * Ignore duplicate accessibility events for the same
         * blocked application arriving within one second.
         */
        if (packageName.equals(lastBlockedPackage)
                && now - lastBlockedLaunchTime
                < BLOCK_SCREEN_COOLDOWN_MS) {

            return;
        }

        lastBlockedPackage = packageName;
        lastBlockedLaunchTime = now;

        Intent intent =
                new Intent(
                        this,
                        BlockedActivity.class
                );

        intent.putExtra(
                "blocked_package",
                packageName
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        try {
            startActivity(intent);
        } catch (Exception ignored) {
            /*
             * Accessibility services should not crash because
             * an activity launch was temporarily rejected.
             */
        }
    }

    private void resetLastBlockedApp() {
        lastBlockedPackage = null;
        lastBlockedLaunchTime = 0L;
    }

    @Override
    public void onInterrupt() {
        resetLastBlockedApp();
    }

    @Override
    public void onDestroy() {
        resetLastBlockedApp();
        super.onDestroy();
    }
}
