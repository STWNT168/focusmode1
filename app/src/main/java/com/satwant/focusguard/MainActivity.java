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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "focusguard_prefs";

    public static final String KEY_BLOCKED_APPS = "blocked_apps";
    public static final String KEY_SESSION_ACTIVE = "session_active";
    public static final String KEY_SESSION_END = "session_end";
    public static final String KEY_SESSION_START = "session_start";
    public static final String KEY_SESSION_DURATION = "session_duration";
    public static final String KEY_COMMITMENT_MODE = "commitment_mode";

    /*
     * Dashboard data:
     *
     * focus_day_YYYY-MM-DD = completed focus minutes for that day
     */
    private static final String FOCUS_DAY_PREFIX = "focus_day_";

    /*
     * Default daily goal = 2 hours.
     */
    private static final int DAILY_GOAL_MINUTES = 120;

    private RecyclerView recyclerView;
    private final List<AppInfo> appList = new ArrayList<>();
    private AppListAdapter adapter;

    private TextView statusText;

    private TextView todayFocusText;
    private TextView todayGoalText;
    private TextView progressPercentText;
    private TextView streakText;
    private TextView sessionsText;
    private TextView weekTotalText;

    private ProgressBar todayProgress;

    private TextView monValue;
    private TextView tueValue;
    private TextView wedValue;
    private TextView thuValue;
    private TextView friValue;
    private TextView satValue;
    private TextView sunValue;

    private Button enableAccessibilityBtn;
    private Button startBtn;
    private Button stopBtn;

    private EditText durationInput;
    private CheckBox commitmentModeCheckBox;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        );

        statusText = findViewById(R.id.statusText);

        todayFocusText = findViewById(R.id.todayFocusText);
        todayGoalText = findViewById(R.id.todayGoalText);
        progressPercentText = findViewById(R.id.progressPercentText);

        streakText = findViewById(R.id.streakText);
        sessionsText = findViewById(R.id.sessionsText);
        weekTotalText = findViewById(R.id.weekTotalText);

        todayProgress = findViewById(R.id.todayProgress);

        monValue = findViewById(R.id.monValue);
        tueValue = findViewById(R.id.tueValue);
        wedValue = findViewById(R.id.wedValue);
        thuValue = findViewById(R.id.thuValue);
        friValue = findViewById(R.id.friValue);
        satValue = findViewById(R.id.satValue);
        sunValue = findViewById(R.id.sunValue);

        enableAccessibilityBtn =
                findViewById(R.id.enableAccessibilityBtn);

        startBtn =
                findViewById(R.id.startBtn);

        stopBtn =
                findViewById(R.id.stopBtn);

        durationInput =
                findViewById(R.id.durationInput);

        commitmentModeCheckBox =
                findViewById(R.id.commitmentModeCheckBox);

        recyclerView =
                findViewById(R.id.appRecyclerView);

        loadInstalledApps();

        adapter = new AppListAdapter(appList);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerView.setAdapter(adapter);

        enableAccessibilityBtn.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                        )
                )
        );

        startBtn.setOnClickListener(v ->
                startSession()
        );

        stopBtn.setOnClickListener(v ->
                stopSession()
        );

        updateDashboard();
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateDashboard();
        updateStatus();
    }

    private void loadInstalledApps() {

        PackageManager pm =
                getPackageManager();

        Set<String> blocked =
                prefs.getStringSet(
                        KEY_BLOCKED_APPS,
                        new HashSet<>()
                );

        List<ApplicationInfo> installed =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        appList.clear();

        for (ApplicationInfo info : installed) {

            if (pm.getLaunchIntentForPackage(
                    info.packageName
            ) == null) {
                continue;
            }

            if (info.packageName.equals(
                    getPackageName()
            )) {
                continue;
            }

            String label =
                    pm.getApplicationLabel(info)
                            .toString();

            appList.add(
                    new AppInfo(
                            label,
                            info.packageName,
                            blocked.contains(
                                    info.packageName
                            )
                    )
            );
        }
    }

    private void startSession() {

        String durStr =
                durationInput
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(durStr)) {

            Toast.makeText(
                    this,
                    "Enter a duration in minutes",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int minutes;

        try {

            minutes =
                    Integer.parseInt(durStr);

        } catch (NumberFormatException e) {

            Toast.makeText(
                    this,
                    "Please enter a whole number of minutes",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (minutes <= 0) {

            Toast.makeText(
                    this,
                    "Duration must be more than 0",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (minutes > 24 * 60) {

            Toast.makeText(
                    this,
                    "Duration cannot exceed 24 hours",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!isAccessibilityServiceEnabled()) {

            Toast.makeText(
                    this,
                    "Enable the blocking permission first",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Set<String> blockedPackages =
                new HashSet<>();

        for (AppInfo app : appList) {

            if (app.blocked) {
                blockedPackages.add(
                        app.packageName
                );
            }
        }

        if (blockedPackages.isEmpty()) {

            Toast.makeText(
                    this,
                    "Select at least one app to block",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        long startTime =
                System.currentTimeMillis();

        long endTime =
                startTime
                        + (minutes * 60_000L);

        boolean commitmentMode =
                commitmentModeCheckBox.isChecked();

        prefs.edit()

                .putStringSet(
                        KEY_BLOCKED_APPS,
                        blockedPackages
                )

                .putBoolean(
                        KEY_SESSION_ACTIVE,
                        true
                )

                .putLong(
                        KEY_SESSION_START,
                        startTime
                )

                .putLong(
                        KEY_SESSION_END,
                        endTime
                )

                .putInt(
                        KEY_SESSION_DURATION,
                        minutes
                )

                .putBoolean(
                        KEY_COMMITMENT_MODE,
                        commitmentMode
                )

                .apply();

        Intent serviceIntent =
                new Intent(
                        this,
                        TimerService.class
                );

        serviceIntent.putExtra(
                "end_time",
                endTime
        );

        ContextCompat.startForegroundService(
                this,
                serviceIntent
        );

        Toast.makeText(
                this,
                "Focus session started for "
                        + minutes
                        + " min",
                Toast.LENGTH_SHORT
        ).show();

        updateStatus();
        updateDashboard();
    }

    private void stopSession() {

        boolean active =
                prefs.getBoolean(
                        KEY_SESSION_ACTIVE,
                        false
                );

        long endTime =
                prefs.getLong(
                        KEY_SESSION_END,
                        0
                );

        boolean commitmentMode =
                prefs.getBoolean(
                        KEY_COMMITMENT_MODE,
                        false
                );

        if (active
                && commitmentMode
                && endTime > System.currentTimeMillis()) {

            Toast.makeText(
                    this,
                    "Commitment Mode is active — you can't stop early",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        prefs.edit()

                .putBoolean(
                        KEY_SESSION_ACTIVE,
                        false
                )

                .putBoolean(
                        KEY_COMMITMENT_MODE,
                        false
                )

                .remove(KEY_SESSION_START)
                .remove(KEY_SESSION_DURATION)

                .apply();

        stopService(
                new Intent(
                        this,
                        TimerService.class
                )
        );

        Toast.makeText(
                this,
                "Session stopped",
                Toast.LENGTH_SHORT
        ).show();

        updateStatus();
        updateDashboard();
    }

    private void updateStatus() {

        boolean active =
                prefs.getBoolean(
                        KEY_SESSION_ACTIVE,
                        false
                );

        long endTime =
                prefs.getLong(
                        KEY_SESSION_END,
                        0
                );

        boolean commitmentMode =
                prefs.getBoolean(
                        KEY_COMMITMENT_MODE,
                        false
                );

        if (active
                && endTime > System.currentTimeMillis()) {

            long remainingMin =
                    Math.max(
                            1,
                            (endTime
                                    - System.currentTimeMillis())
                                    / 60000
                    );

            startBtn.setVisibility(
                    View.GONE
            );

            durationInput.setEnabled(
                    false
            );

            commitmentModeCheckBox.setEnabled(
                    false
            );

            if (commitmentMode) {

                statusText.setText(
                        "LOCKED • "
                                + remainingMin
                                + " MIN REMAINING"
                );

                stopBtn.setVisibility(
                        View.GONE
                );

            } else {

                statusText.setText(
                        "FOCUS ACTIVE • "
                                + remainingMin
                                + " MIN REMAINING"
                );

                stopBtn.setVisibility(
                        View.VISIBLE
                );
            }

        } else {

            statusText.setText(
                    "READY FOR YOUR NEXT SESSION"
            );

            startBtn.setVisibility(
                    View.VISIBLE
            );

            stopBtn.setVisibility(
                    View.GONE
            );

            durationInput.setEnabled(
                    true
            );

            commitmentModeCheckBox.setEnabled(
                    true
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * DASHBOARD
     * ---------------------------------------------------------
     */

    public static String getDayKey(long timeMillis) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        return format.format(
                new Date(timeMillis)
        );
    }

    public static void recordCompletedSession(
            SharedPreferences prefs,
            int minutes
    ) {

        if (minutes <= 0) {
            return;
        }

        String key =
                FOCUS_DAY_PREFIX
                        + getDayKey(
                        System.currentTimeMillis()
                );

        int existing =
                prefs.getInt(
                        key,
                        0
                );

        prefs.edit()
                .putInt(
                        key,
                        existing + minutes
                )
                .apply();
    }

    private int getFocusMinutesForDay(
            Calendar day
    ) {

        String key =
                FOCUS_DAY_PREFIX
                        + getDayKey(
                        day.getTimeInMillis()
                );

        return prefs.getInt(
                key,
                0
        );
    }

    private Calendar getStartOfWeek() {

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        /*
         * Make Monday day 1.
         */
        int day =
                calendar.get(
                        Calendar.DAY_OF_WEEK
                );

        int difference;

        if (day == Calendar.SUNDAY) {
            difference = -6;
        } else {
            difference =
                    Calendar.MONDAY - day;
        }

        calendar.add(
                Calendar.DAY_OF_MONTH,
                difference
        );

        return calendar;
    }

    private void updateDashboard() {

        Calendar today =
                Calendar.getInstance();

        int todayMinutes =
                getFocusMinutesForDay(
                        today
                );

        int progress =
                Math.min(
                        100,
                        Math.round(
                                (todayMinutes * 100f)
                                        / DAILY_GOAL_MINUTES
                        )
                );

        todayFocusText.setText(
                formatMinutes(todayMinutes)
        );

        todayGoalText.setText(
                "GOAL  •  "
                        + formatMinutes(
                        DAILY_GOAL_MINUTES
                )
        );

        progressPercentText.setText(
                progress + "%"
        );

        todayProgress.setMax(
                DAILY_GOAL_MINUTES
        );

        todayProgress.setProgress(
                Math.min(
                        todayMinutes,
                        DAILY_GOAL_MINUTES
                )
        );

        int streak =
                calculateCurrentStreak();

        streakText.setText(
                String.valueOf(streak)
        );

        int sessions =
                calculateTodaySessionCount();

        sessionsText.setText(
                String.valueOf(sessions)
        );

        Calendar monday =
                getStartOfWeek();

        int weeklyTotal = 0;

        int[] values =
                new int[7];

        for (int i = 0; i < 7; i++) {

            Calendar day =
                    (Calendar) monday.clone();

            day.add(
                    Calendar.DAY_OF_MONTH,
                    i
            );

            values[i] =
                    getFocusMinutesForDay(
                            day
                    );

            weeklyTotal += values[i];
        }

        weekTotalText.setText(
                formatMinutes(weeklyTotal)
        );

        updateWeekValue(
                monValue,
                values[0]
        );

        updateWeekValue(
                tueValue,
                values[1]
        );

        updateWeekValue(
                wedValue,
                values[2]
        );

        updateWeekValue(
                thuValue,
                values[3]
        );

        updateWeekValue(
                friValue,
                values[4]
        );

        updateWeekValue(
                satValue,
                values[5]
        );

        updateWeekValue(
                sunValue,
                values[6]
        );
    }

    private void updateWeekValue(
            TextView view,
            int minutes
    ) {

        if (minutes == 0) {

            view.setText("—");

        } else {

            view.setText(
                    formatMinutes(minutes)
            );
        }
    }

    private int calculateCurrentStreak() {

        Calendar day =
                Calendar.getInstance();

        int streak = 0;

        /*
         * If today has no completed focus yet,
         * allow the current streak to continue from yesterday.
         */
        if (getFocusMinutesForDay(day) == 0) {

            day.add(
                    Calendar.DAY_OF_MONTH,
                    -1
            );
        }

        while (true) {

            if (getFocusMinutesForDay(day) <= 0) {
                break;
            }

            streak++;

            day.add(
                    Calendar.DAY_OF_MONTH,
                    -1
            );

            /*
             * Safety limit.
             */
            if (streak >= 3650) {
                break;
            }
        }

        return streak;
    }

    /*
     * Session count is stored individually.
     *
     * We use the number of completed sessions for
     * the current day.
     */
    private int calculateTodaySessionCount() {

        String key =
                "sessions_"
                        + getDayKey(
                        System.currentTimeMillis()
                );

        return prefs.getInt(
                key,
                0
        );
    }

    public static void recordCompletedSessionCount(
            SharedPreferences prefs
    ) {

        String key =
                "sessions_"
                        + getDayKey(
                        System.currentTimeMillis()
                );

        int count =
                prefs.getInt(
                        key,
                        0
                );

        prefs.edit()
                .putInt(
                        key,
                        count + 1
                )
                .apply();
    }

    private String formatMinutes(
            int minutes
    ) {

        int hours =
                minutes / 60;

        int remaining =
                minutes % 60;

        if (hours > 0) {

            if (remaining == 0) {

                return hours + "h";

            } else {

                return hours
                        + "h "
                        + remaining
                        + "m";
            }
        }

        return minutes + "m";
    }

    private void updateProgressForDashboard() {
        updateDashboard();
    }

    private boolean isAccessibilityServiceEnabled() {

        AccessibilityManager am =
                (AccessibilityManager)
                        getSystemService(
                                ACCESSIBILITY_SERVICE
                        );

        if (am == null) {
            return false;
        }

        List<android.accessibilityservice.AccessibilityServiceInfo>
                enabledServices =
                am.getEnabledAccessibilityServiceList(
                        android.accessibilityservice
                                .AccessibilityServiceInfo
                                .FEEDBACK_ALL_MASK
                );

        for (
                android.accessibilityservice
                        .AccessibilityServiceInfo service
                : enabledServices
        ) {

            if (service.getResolveInfo() == null
                    || service.getResolveInfo()
                    .serviceInfo == null) {

                continue;
            }

            if (service.getResolveInfo()
                    .serviceInfo
                    .packageName
                    .equals(
                            getPackageName()
                    )) {

                return true;
            }
        }

        return false;
    }
}
