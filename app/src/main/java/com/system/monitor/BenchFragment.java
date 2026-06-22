package com.system.monitor;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.Random;

public class BenchFragment extends Fragment {
    private TextView tvWriteSpeed, tvReadSpeed, tvLatency, tvBenchStatus;
    private ProgressBar benchProgress;
    private com.google.android.material.button.MaterialButton btnStartBench;
    private boolean isRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bench, container, false);

        tvWriteSpeed = root.findViewById(R.id.tvWriteSpeed);
        tvReadSpeed = root.findViewById(R.id.tvReadSpeed);
        tvLatency = root.findViewById(R.id.tvLatency);
        tvBenchStatus = root.findViewById(R.id.tvBenchStatus);
        benchProgress = root.findViewById(R.id.benchProgress);
        btnStartBench = root.findViewById(R.id.btnStartBench);

        btnStartBench.setOnClickListener(v -> {
            if (!isRunning) startBenchmark();
        });

        return root;
    }

    private void startBenchmark() {
        isRunning = true;
        btnStartBench.setEnabled(false);
        benchProgress.setVisibility(View.VISIBLE);
        tvBenchStatus.setText("Running write test...");
        tvWriteSpeed.setText("—");
        tvReadSpeed.setText("—");
        tvLatency.setText("—");

        new Thread(() -> {
            try {
                // Write benchmark
                updateStatus("Running write test...");
                long writeSpeed = runWriteTest();

                // Read benchmark
                updateStatus("Running read test...");
                long readSpeed = runReadTest();

                // Latency benchmark
                updateStatus("Measuring latency...");
                long latency = runLatencyTest();

                // Done
                long finalWriteSpeed = writeSpeed;
                long finalReadSpeed = readSpeed;
                long finalLatency = latency;

                new Handler(Looper.getMainLooper()).post(() -> {
                    tvWriteSpeed.setText(String.format(Locale.US, "%,d", finalWriteSpeed));
                    tvReadSpeed.setText(String.format(Locale.US, "%,d", finalReadSpeed));
                    tvLatency.setText(String.format(Locale.US, "%d", finalLatency));
                    tvBenchStatus.setText("Benchmark complete ✓");
                    benchProgress.setVisibility(View.GONE);
                    btnStartBench.setEnabled(true);
                    isRunning = false;
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvBenchStatus.setText("Error: " + e.getMessage());
                    benchProgress.setVisibility(View.GONE);
                    btnStartBench.setEnabled(true);
                    isRunning = false;
                });
            }
        }).start();
    }

    private long runWriteTest() {
        // Allocate 32 MB, measure time to fill
        int size = 32 * 1024 * 1024; // 32 MB in bytes
        byte[] buffer = new byte[size];

        long start = System.nanoTime();
        for (int i = 0; i < size; i++) buffer[i] = (byte) (i & 0xFF);
        long elapsed = System.nanoTime() - start;

        // MB/s
        return (long) ((size / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0));
    }

    private long runReadTest() {
        // Allocate 32 MB, measure time to read sequentially
        int size = 32 * 1024 * 1024;
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++) buffer[i] = (byte) (i & 0xFF);

        long checksum = 0;
        long start = System.nanoTime();
        for (int i = 0; i < size; i++) checksum += buffer[i];
        long elapsed = System.nanoTime() - start;

        // Prevent dead-code elimination
        if (checksum == Long.MIN_VALUE) buffer[0] = 0;

        return (long) ((size / (1024.0 * 1024.0)) / (elapsed / 1_000_000_000.0));
    }

    private long runLatencyTest() {
        // Random access latency on 16 MB array (fits in L2/L3)
        int size = 4 * 1024 * 1024; // 4M int array = 16 MB
        int[] buffer = new int[size];
        Random rnd = new Random(42);

        // Fill with random indices (pointer chasing)
        for (int i = 0; i < size; i++) buffer[i] = rnd.nextInt(size);

        int accesses = 1_000_000;
        int idx = 0;
        long start = System.nanoTime();
        for (int i = 0; i < accesses; i++) idx = buffer[idx];
        long elapsed = System.nanoTime() - start;

        // Prevent dead-code elimination
        if (idx < 0) buffer[0] = 0;

        return elapsed / accesses; // nanoseconds per access
    }

    private void updateStatus(String text) {
        new Handler(Looper.getMainLooper()).post(() -> tvBenchStatus.setText(text));
    }
}