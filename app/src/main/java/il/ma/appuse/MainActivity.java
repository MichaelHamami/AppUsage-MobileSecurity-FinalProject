package il.ma.appuse;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private MaterialButton main_BTN_get_apps;
    private MaterialButton main_BTN_usage_settings;
    private TextView main_TXT_explanation_usage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called");
        boolean granted = CheckUsagePermission(this);
        if(granted)
        {
            removeExplanationUI();
        }
        else
        {
            showUsageExplanationUI();

        }
    }

    private void showUsageExplanationUI() {
        this.main_TXT_explanation_usage.setVisibility(View.VISIBLE);
        this.main_BTN_usage_settings.setVisibility(View.VISIBLE);
    }
    
    private void removeExplanationUI() {
        this.main_TXT_explanation_usage.setVisibility(View.INVISIBLE);
        this.main_BTN_usage_settings.setVisibility(View.GONE);
    }

    public static boolean CheckUsagePermission(Context context) {
        boolean granted;
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        return granted;
    }

    private void initViews() {
        this.main_BTN_get_apps.setOnClickListener(v -> getApps());
        this.main_BTN_usage_settings.setOnClickListener(v->goUsageSettings());
    }

    public  void goUsageSettings() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void findViews() {
        this.main_BTN_get_apps = findViewById(R.id.main_BTN_get_apps);
        this.main_TXT_explanation_usage = findViewById(R.id.main_TXT_explanation_usage);
        this.main_BTN_usage_settings = findViewById(R.id.main_BTN_usage_settings);
    }
    public void getApps() {
        Log.d(TAG, "getApps called");
        if(CheckUsagePermission(this)) {
            startActivity(new Intent(this, ActivityUsageStats.class));
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Usage Access Permission");
            builder.setMessage("There's no permission for this app to access your system usage data, please go to 'Usage Access' in your device to enable access to the data.");
            builder.setPositiveButton("Take Me To Settings", (dialog, which) -> goUsageSettings());
            builder.setNegativeButton("Cancel", (dialog, which) -> {});
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}