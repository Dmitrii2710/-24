package com.system.monitor;

import androidx.annotation.NonNull;
import androidx.fragment.app.*;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final MonitorService service;

    public ViewPagerAdapter(@NonNull FragmentActivity fa, MonitorService s) {
        super(fa);
        this.service = s;
    }

    @NonNull
    @Override
    public Fragment createFragment(int pos) {
        switch (pos) {
            case 0: return new LiveFragment(service);
            case 1: return new InfoFragment("SOC", service.getSocData());
            case 2: return new InfoFragment("Device", service.getDeviceInfo());
            case 3: return new InfoFragment("System", service.getSystemInfo());
            case 4: return new InfoFragment("Battery", service.getBatteryInfo());
            case 5: return new BenchFragment();
            default: return new InfoFragment("Info", new String[][]{});
        }
    }

    @Override
    public int getItemCount() { return 6; }
}