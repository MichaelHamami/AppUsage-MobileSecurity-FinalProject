package il.ma.appuse;

import androidx.appcompat.app.AppCompatActivity;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private MaterialButton main_BTN_get_apps;
    private UsageStatsManager mUsageStatsManager;
    ;
    private Map<String, String> mAppLabelList;
    private final ArrayMap<String, String> mAppLabelMap = new ArrayMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
    }
    private void initViews() {
        this.main_BTN_get_apps.setOnClickListener(v -> getApps());
    }

    private void findViews() {
        this.main_BTN_get_apps = findViewById(R.id.main_BTN_get_apps);
    }
    private void getApps() {
        Log.d(TAG, "getApps called");

        final PackageManager pm = getPackageManager();
//get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -5);
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        final List<UsageStats> stats =
                mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,
                        cal.getTimeInMillis(), System.currentTimeMillis());
        ArrayMap<String, UsageStats> map = new ArrayMap<>();
        final int statCount = stats.size();
        for (int i = 0; i < statCount; i++) {
            final android.app.usage.UsageStats pkgStats = stats.get(i);
//            if(i == 0) {
//                Log.d(TAG,  pkgStats.toString());
            Log.d(TAG,  pkgStats.getPackageName().toString());
                Log.d(TAG, DateUtils.formatSameDayTime(pkgStats.getLastTimeUsed(),
                        System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.MEDIUM).toString());
//                Log.d(TAG, DateUtils.formatElapsedTime(pkgStats.getTotalTimeInForeground() / 1000));
                Log.d(TAG, DateUtils.formatElapsedTime(pkgStats.getTotalTimeInForeground()));

//            }
            // load application labels for each application
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(pkgStats.getPackageName(), 0);
                String label = appInfo.loadLabel(pm).toString();
                mAppLabelMap.put(pkgStats.getPackageName(), label);
                Log.d(TAG, label);

//                if(i == 0)
//                {
//                    Log.d(TAG, label);
//                }
                UsageStats existingStats =
                        map.get(pkgStats.getPackageName());
                if (existingStats == null) {
                    map.put(pkgStats.getPackageName(), pkgStats);
                } else {
                    existingStats.add(pkgStats);
                }

            } catch (PackageManager.NameNotFoundException e) {
                // This package may be gone.
            }
        }

//        for (ApplicationInfo packageInfo : packages) {
////            Log.d(TAG, "Installed package :" + packageInfo.packageName);
//            if (packageInfo.packageName.equals("com.classy.survivegame"))
//            {
//                Log.d(TAG, packageInfo.dataDir);
//
//            }
//            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
//            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
    }
}