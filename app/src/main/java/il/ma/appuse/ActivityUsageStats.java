package il.ma.appuse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;


public class ActivityUsageStats extends Activity implements OnItemSelectedListener {
    private static final String TAG = "ActivityUsageStats";
    private UsageStatsManager mUsageStatsManager;
    private LayoutInflater mInflater;
    private UsageStatsAdapter mAdapter;
    private PackageManager mPackageManager;
    private CheckBox mChkAllApps;
    private ImageView mOrderByArrow;
    private static int ORDER_BY = 1;
    private ListView mListView;


    public static class AppNameComparator implements Comparator<UsageStats> {
        private final Map<String, String> mAppLabelList;

        AppNameComparator(Map<String, String> appList) {
            mAppLabelList = appList;
        }

        @Override
        public final int compare(UsageStats a, UsageStats b) {
            String val_1 = mAppLabelList.get(a.getPackageName());
            String val_2 = mAppLabelList.get(b.getPackageName());
            return val_1.compareTo(val_2) * ORDER_BY;
        }
    }

    public static class LastTimeUsedComparator implements Comparator<UsageStats> {
        @Override
        public final int compare(UsageStats a, UsageStats b) {
            // return by descending order
            return (int)(b.getLastTimeUsed() - a.getLastTimeUsed()) * ORDER_BY;
        }
    }

    public static class UsageTimeComparator implements Comparator<UsageStats> {
        @Override
        public final int compare(UsageStats a, UsageStats b) {
            return (int)(b.getTotalTimeInForeground() - a.getTotalTimeInForeground()) * ORDER_BY;
        }
    }

    // View Holder used when displaying views
    static class AppViewHolder {
        ImageView appIcon;
        TextView pkgName;
        TextView lastTimeUsed;
        TextView usageTime;
    }

    class UsageStatsAdapter extends BaseAdapter {

        // Constants defining order for display order
        private static final int DISPLAY_ORDER_USAGE_TIME = 0;
        private static final int DISPLAY_ORDER_LAST_TIME_USED = 1;
        private static final int DISPLAY_ORDER_APP_NAME = 2;
        private static final String NEVER_USED = "Never";

        private int mDisplayOrder = DISPLAY_ORDER_USAGE_TIME;
        private LastTimeUsedComparator mLastTimeUsedComparator = new LastTimeUsedComparator();
        private UsageTimeComparator mUsageTimeComparator = new UsageTimeComparator();
        private AppNameComparator mAppLabelComparator;
        private final ArrayMap<String, String> mAppLabelMap = new ArrayMap<>();
        private final ArrayList<UsageStats> mPackageStats = new ArrayList<>();

        UsageStatsAdapter() {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -5);

            final List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, cal.getTimeInMillis(), System.currentTimeMillis());
            if (stats == null) {
                return;
            }

            ArrayMap<String, UsageStats> map = new ArrayMap<>();
            final int statCount = stats.size();
            for (int i = 0; i < statCount; i++) {
                final android.app.usage.UsageStats pkgStats = stats.get(i);
                try {
                    ApplicationInfo appInfo = mPackageManager.getApplicationInfo(pkgStats.getPackageName(), 0);
                    // Filter apps the user haven't used since installing this app
                    if (mChkAllApps.isChecked() && pkgStats.getLastTimeUsed() == 0 || pkgStats.getPackageName().equals(getPackageName())){
                        continue;
                    }

                    String label = appInfo.loadLabel(mPackageManager).toString();

//                    Log.d(TAG, pkgStats.getPackageName() + " | " + label);
//                    Log.d(TAG, label);
                    mAppLabelMap.put(pkgStats.getPackageName(), label);
                    UsageStats existingStats = map.get(pkgStats.getPackageName());
                    if (existingStats == null) {
                        map.put(pkgStats.getPackageName(), pkgStats);

                    } else {
                        existingStats.add(pkgStats);
                    }

                } catch (NameNotFoundException e) {
                    // This package may be gone.
                }
            }
            mPackageStats.addAll(map.values());

            // Sort list
            mAppLabelComparator = new AppNameComparator(mAppLabelMap);
            sortList();
        }

        @Override
        public int getCount() {
            return mPackageStats.size();
        }

        @Override
        public Object getItem(int position) {
            return mPackageStats.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder;
            if (convertView == null) {
                // if there's content, inflate the view
                convertView = mInflater.inflate(R.layout.layout_usage_state_list_item, null);
                holder = new AppViewHolder();
                holder.appIcon = convertView.findViewById(R.id.usage_state_item_IMG_app_icon);
                holder.pkgName = convertView.findViewById(R.id.usage_state_item_TXT_app_label);
                holder.lastTimeUsed = convertView.findViewById(R.id.usage_state_item_TXT_last_time_used);
                holder.usageTime = convertView.findViewById(R.id.usage_state_item_TXT_total_time);
                convertView.setTag(holder);
            } else {
                holder = (AppViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder
            UsageStats pkgStats = mPackageStats.get(position);
            if (pkgStats != null) {
                String appName = mAppLabelMap.get(pkgStats.getPackageName());
                Drawable drawable;

                try {
                    drawable = getPackageManager().getApplicationIcon(pkgStats.getPackageName());
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                   drawable = ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_launcher);
                }
                holder.appIcon.setImageDrawable(drawable);

                holder.pkgName.setText(appName);

                if (pkgStats.getLastTimeUsed() != 0) {
                    holder.lastTimeUsed.setText(DateUtils.formatSameDayTime(pkgStats.getLastTimeUsed(), System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.MEDIUM));
                } else {
                    holder.lastTimeUsed.setText(NEVER_USED);
                }
                holder.usageTime.setText(DateUtils.formatElapsedTime(pkgStats.getTotalTimeInForeground() / 1000));
            } else {
                Log.d(TAG, "No usage stats info for app:" + position);
            }
            return convertView;
        }

        void sortList(int sortOrder) {
            if (mDisplayOrder == sortOrder) { // same sort order - do nothing
                return;
            }
            mDisplayOrder= sortOrder;
            sortList();
        }
        private void sortList() {
            if (mDisplayOrder == DISPLAY_ORDER_USAGE_TIME) {
                Log.d(TAG, "Sorting by usage time");
                Collections.sort(mPackageStats, mUsageTimeComparator);
            } else if (mDisplayOrder == DISPLAY_ORDER_LAST_TIME_USED) {
                 Log.d(TAG, "Sorting by last time used");
                Collections.sort(mPackageStats, mLastTimeUsedComparator);
            } else if (mDisplayOrder == DISPLAY_ORDER_APP_NAME) {
                 Log.d(TAG, "Sorting by application name");
                Collections.sort(mPackageStats, mAppLabelComparator);
            }
            notifyDataSetChanged();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!MainActivity.CheckUsagePermission(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Usage Access Permission");
            builder.setMessage("Usage access permission was turned off, please turn it back on in order to use the app");
            builder.setPositiveButton("Take Me To Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            builder.setNegativeButton("Cancel", (dialog, which) -> finish());
            builder.setCancelable(false);
            AlertDialog alert = builder.create();
            alert.show();
        }
        refreshDisplayAppList();

    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_usage_stats);

        setUpViews();

        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPackageManager = getPackageManager();
        Spinner typeSpinner = findViewById(R.id.activity_usage_state_SPINNER);
        typeSpinner.setOnItemSelectedListener(this);

        mListView = findViewById(R.id.pkg_list);
        mAdapter = new UsageStatsAdapter();
        mListView.setAdapter(mAdapter);


        mChkAllApps.setOnClickListener(v -> {
            refreshDisplayAppList();
        });

        mOrderByArrow.setOnClickListener(v -> {
            ORDER_BY *= -1;
            if(ORDER_BY == 1) {
                mOrderByArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24);
            } else {
                mOrderByArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24);
            }
            refreshDisplayAppList();
        });

    }

    private void refreshDisplayAppList() {
        mAdapter = new UsageStatsAdapter();
        mListView.setAdapter(mAdapter);
    }

    private void setUpViews() {
        mChkAllApps = findViewById(R.id.activity_usage_stats_CHK_show_all_apps);
        mOrderByArrow = findViewById(R.id.activity_usage_IMG_sort_order);
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mAdapter.sortList(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }
}


