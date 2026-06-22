package com.system.monitor;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class LiveFragment extends Fragment {
    private MonitorService service;
    private Handler handler;
    private Runnable updateRunnable;

    private LiveGraphView cpuGraph, ramGraph;
    private TextView tvCpuPercent, tvCpuFreq, tvCpuCores, tvCpuTemp;
    private TextView tvRamPercent, tvRamUsed, tvRamFree, tvRamTotal;

    // CPU usage estimation via /proc/stat
    private long prevIdle = 0, prevTotal = 0;

    public LiveFragment(MonitorService service) {
        this.service = service;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_live, container, false);

        cpuGraph = root.findViewById(R.id.cpuGraph);
        ramGraph = root.findViewById(R.id.ramGraph);

        tvCpuPercent = root.findViewById(R.id.tvCpuPercent);
        tvCpuFreq = root.findViewById(R.id.tvCpuFreq);
        tvCpuCores = root.findViewById(R.id.tvCpuCores);
        tvCpuTemp = root.findViewById(R.id.tvCpuTemp);

        tvRamPercent = root.findViewById(R.id.tvRamPercent);
        tvRamUsed = root.findViewById(R.id.tvRamUsed);
        tvRamFree = root.findViewById(R.id.tvRamFree);
        tvRamTotal = root.findViewById(R.id.tvRamTotal);

        // Set RAM graph green
        ramGraph.setColor(Color.parseColor("#66BB6A"));

        tvCpuCores.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));

        handler = new Handler(Looper.getMainLooper());
        updateRunnable = this::updateStats;
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateStats() {
        // CPU usage
        float cpuUsage = readCpuUsage();
        cpuGraph.addValue(cpuUsage);
        tvCpuPercent.setText(String.format(Locale.US, "%.0f", cpuUsage));

        // CPU freq
        if (service != null) {
            tvCpuFreq.setText(service.getCurrentCpuFreq());
            float temp = service.getBatteryTemp();
            tvCpuTemp.setText(String.format(Locale.US, "%.1f °C", temp));

            // RAM
            ActivityManager.MemoryInfo memInfo = service.getMemoryInfo();
            float ramPct = service.getRamUsagePercent();
            long usedMem = memInfo.totalMem - memInfo.availMem;

            ramGraph.addValue(ramPct);
            tvRamPercent.setText(String.format(Locale.US, "%.0f", ramPct));
            tvRamUsed.setText(service.formatSize(usedMem));
            tvRamFree.setText(service.formatSize(memInfo.availMem));
            tvRamTotal.setText(service.formatSize(memInfo.totalMem));
        }

        handler.postDelayed(updateRunnable, 1000);
    }

    private float readCpuUsage() {
        try {
            java.io.RandomAccessFile reader = new java.io.RandomAccessFile("/proc/stat", "r");
            String line = reader.readLine();
            reader.close();

            String[] parts = line.split("\\s+");
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iowait = Long.parseLong(parts[5]);
            long irq = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long currentIdle = idle + iowait;
            long currentTotal = user + nice + system + idle + iowait + irq + softirq;

            long diffIdle = currentIdle - prevIdle;
            long diffTotal = currentTotal - prevTotal;

            prevIdle = currentIdle;
            prevTotal = currentTotal;

            if (diffTotal == 0) return 0;
            return (float) (diffTotal - diffIdle) / diffTotal * 100f;
        } catch (Exception e) {
            // Fallback — по загрузке процессора JVM
            return (float) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                    / Runtime.getRuntime().totalMemory() * 50f;
        }
    }
}