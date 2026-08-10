package com.satwant.focusguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "focusguard_prefs";
    public static final String KEY_BLOCKED_APPS = "blocked_apps";
    public static final String KEY_SESSION_ACTIVE = "session_active";
    public static final String KEY_SESSION_END = "session_end";
    public static final String KEY_COMMITMENT_MODE = "commitment_mode";

    private RecyclerView recyclerView;
    private final List<AppInfo> appList = new ArrayList<>();
    private AppListAdapter adapter;
    private TextView statusText;
    private Button enableAccessibilityBtn, startBtn, stopBtn;
    private EditText durationInput;
    private android.widget.CheckBox commitmentModeCheckBox;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        statusText = findViewById(R.id.statusText);
        enableAccessibilityBtn = findViewById(R.id.enableAccessibilityBtn);
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);
        durationInput = findViewById(R.id.durationInput);
        commitmentModeCheckBox = findViewById(R.id.commitmentModeCheckBox);
        recyclerView = findViewById(R.id.appRecyclerView);

        loadInstalledApps();
        adapter = new AppListAdapter(appList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        enableAccessibilityBtn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        startBtn.setOnClickListener(v -> startSession());
        stopBtn.setOnClickListener(v -> stopSession());

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Set<String> blocked = prefs.getStringSet(KEY_BLOCKED_APPS, new HashSet<>());
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : installed) {
            if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
            if (info.packageName.equals(getPackageName())) continue;

            String label = pm.getApplicationLabel(info).toString();
            appList.add(new AppInfo(label, info.packageName, blocked.contains(info.packageName)));
        }
    }

    private void startSession() {
        String durStr = durationInput.getText().toString().trim();
        if (TextUtils.isEmpty(durStr)) {
            Toast.makeText(this, "Enter a duration in minutes", Toast.LENGTH_SHORT).show();
            return;
        }
        int minutes = Integer.parseInt(durStr);
        if (minutes <= 0) {
            Toast.makeText(this, "Duration must be more than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Enable the blocking permission first", Toast.LENGTH_LONG).show();
            return;
        }

        Set<String> blockedPackages = new HashSet<>();
        for (AppInfo app : appList) {
            if (app.blocked) blockedPackages.add(app.packageName);
        }

        long endTime = System.currentTimeMillis() + (minutes * 60_000L);
        boolean commitmentMode = commitmentModeCheckBox.isChecked();

        prefs.edit()
                .putStringSet(KEY_BLOCKED_APPS, blockedPackages)
                .putBoolean(KEY_SESSION_ACTIVE, true)
                .putLong(KEY_SESSION_END, endTime)
                .putBoolean(KEY_COMMITMENT_MODE, commitmentMode)
                .apply();

        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra("end_time", endTime);
        startForegroundService(serviceIntent);

        Toast.makeText(this, "Focus session started for " + minutes + " min", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void stopSession() {
        boolean active = prefs.getBoolean(KEY_SESSION_ACTIVE, false);
        long endTime = prefs.getLong(KEY_SESSION_END, 0);
        boolean commitmentMode = prefs.getBoolean(KEY_COMMITMENT_MODE, false);

        if (active && commitmentMode && endTime > System.currentTimeMillis()) {
            Toast.makeText(this, "Commitment Mode is active — you can't stop early", Toast.LENGTH_LONG).show();
            return;
        }

        prefs.edit().putBoolean(KEY_SESSION_ACTIVE, false).apply();
        stopService(new Intent(this, TimerService.class));
        Toast.makeText(this, "Session stopped", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void updateStatus() {
        boolean active = prefs.getBoolean(KEY_SESSION_ACTIVE, false);
        long endTime = prefs.getLong(KEY_SESSION_END, 0);
        boolean commitmentMode = prefs.getBoolean(KEY_COMMITMENT_MODE, false);

        if (active && endTime > System.currentTimeMillis()) {
            long remainingMin = (endTime - System.currentTimeMillis()) / 60000;
            startBtn.setVisibility(View.GONE);
            durationInput.setEnabled(false);
            commitmentModeCheckBox.setEnabled(false);

            if (commitmentMode) {
                statusText.setText("🔒 Commitment Mode — " + remainingMin + " min remaining (locked)");
                stopBtn.setVisibility(View.GONE);
            } else {
                statusText.setText("Focus session active — " + remainingMin + " min remaining");
                stopBtn.setVisibility(View.VISIBLE);
            }
        } else {
            statusText.setText("No active session");
            startBtn.setVisibility(View.VISIBLE);
            stopBtn.setVisibility(View.GONE);
            durationInput.setEnabled(true);
            commitmentModeCheckBox.setEnabled(true);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<android.accessibilityservice.AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (android.accessibilityservice.AccessibilityServiceInfo service : enabledServices) {
            if (service.getResolveInfo().serviceInfo.packageName.equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
