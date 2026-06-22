package com.system.monitor;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.DisplayMetrics;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MonitorService extends Service {
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MonitorService getService() { return MonitorService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    "sysinfo_channel", "SysInfo Monitor", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Background hardware monitoring");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            startForeground(1, new NotificationCompat.Builder(this, "sysinfo_channel")
                    .setContentTitle("SysInfo")
                    .setContentText("Monitoring system resources")
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build());
        }
    }

    // ==================== SOC ====================
    public String[][] getSocData() {
        return new String[][]{
                {"SoC Model", Build.VERSION.SDK_INT >= 31 ? Build.SOC_MODEL : Build.HARDWARE},
                {"Cores", String.valueOf(Runtime.getRuntime().availableProcessors())},
                {"Architecture", System.getProperty("os.arch")},
                {"Supported ABIs", Arrays.toString(Build.SUPPORTED_ABIS).replaceAll("[\\[\\]]", "")},
                {"CPU Governor", readOneLine("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "Locked by OS")},
                {"Max Frequency", readCpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")},
                {"Min Frequency", readCpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")},
                {"Board", Build.BOARD},
                {"Hardware Platform", Build.HARDWARE},
                {"Bootloader", Build.BOOTLOADER}
        };
    }

    // ==================== DEVICE ====================
    public String[][] getDeviceInfo() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memInfo);

        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long totalRom = stat.getBlockCountLong() * stat.getBlockSizeLong();
        long freeRom = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
        double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
        double screenInches = Math.round(Math.sqrt(x + y) * 10.0) / 10.0;

        return new String[][]{
                {"Model", Build.MODEL},
                {"Manufacturer", Build.MANUFACTURER},
                {"Brand", Build.BRAND},
                {"Device", Build.DEVICE},
                {"Product", Build.PRODUCT},
                {"Screen Size", screenInches > 0 ? screenInches + " inches" : "Unknown"},
                {"Screen Resolution", metrics.widthPixels + " × " + metrics.heightPixels + " px"},
                {"Screen Density", metrics.densityDpi + " dpi"},
                {"Total RAM", formatSize(memInfo.totalMem)},
                {"Available RAM", formatSize(memInfo.availMem)},
                {"Internal Storage", formatSize(totalRom)},
                {"Available Storage", formatSize(freeRom)},
                {"USB Host", getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST) ? "Supported" : "Not Supported"}
        };
    }

    // ==================== SYSTEM ====================
    public String[][] getSystemInfo() {
        long millis = SystemClock.elapsedRealtime();
        String uptime = String.format(Locale.US, "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        boolean isRooted = new File("/system/xbin/su").exists() || new File("/system/bin/su").exists();

        return new String[][]{
                {"Android Version", Build.VERSION.RELEASE},
                {"API Level", String.valueOf(Build.VERSION.SDK_INT)},
                {"Security Patch", Build.VERSION.SDK_INT >= 23 ? Build.VERSION.SECURITY_PATCH : "N/A"},
                {"Build ID", Build.DISPLAY},
                {"Codename", Build.VERSION.CODENAME},
                {"Incremental", Build.VERSION.INCREMENTAL},
                {"Java VM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")},
                {"Kernel Architecture", System.getProperty("os.arch")},
                {"Kernel Version", System.getProperty("os.version")},
                {"Root Access", isRooted ? "Yes" : "No"},
                {"System Uptime", uptime}
        };
    }

    // ==================== BATTERY ====================
    public String[][] getBatteryInfo() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return new String[][]{{"Battery", "Data Unavailable"}};

        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (int) ((level / (float) scale) * 100);
        int temp = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int volt = b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

        String healthStr;
        switch (b.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            case BatteryManager.BATTERY_HEALTH_GOOD: healthStr = "Good"; break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: healthStr = "Overheat"; break;
            case BatteryManager.BATTERY_HEALTH_DEAD: healthStr = "Dead"; break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: healthStr = "Over Voltage"; break;
            case BatteryManager.BATTERY_HEALTH_COLD: healthStr = "Cold"; break;
            default: healthStr = "Unknown";
        }

        String plugStr = "Battery (Unplugged)";
        int plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) plugStr = "AC Charger";
        else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) plugStr = "USB Port";
        else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) plugStr = "Wireless";

        String statusStr = "Discharging";
        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) statusStr = "Charging";
        else if (status == BatteryManager.BATTERY_STATUS_FULL) statusStr = "Full";
        else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) statusStr = "Not Charging";

        String tech = b.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

        return new String[][]{
                {"Health", healthStr},
                {"Level", pct + " %"},
                {"Power Source", plugStr},
                {"Status", statusStr},
                {"Technology", tech != null ? tech : "Li-ion"},
                {"Temperature", String.format(Locale.US, "%.1f °C", temp / 10.0f)},
                {"Voltage", volt + " mV"}
        };
    }

    // ==================== LIVE DATA ====================
    public float getRamUsagePercent() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memInfo);
        if (memInfo.totalMem == 0) return 0;
        return (float) (memInfo.totalMem - memInfo.availMem) / memInfo.totalMem * 100f;
    }

    public ActivityManager.MemoryInfo getMemoryInfo() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memInfo);
        return memInfo;
    }

    public float getBatteryTemp() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return 0;
        return b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
    }

    public String getCurrentCpuFreq() {
        return readCpuFreq("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
    }

    // ==================== HELPERS ====================
    public String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String readCpuFreq(String path) {
        String res = readOneLine(path, null);
        if (res == null) return "N/A";
        try { return (Long.parseLong(res.trim()) / 1000) + " MHz"; }
        catch (Exception e) { return res; }
    }

    private String readOneLine(String path, String def) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) { return r.readLine(); }
        catch (Exception e) { return def; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }
}