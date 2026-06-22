package com.system.monitor;

import android.content.Context;
import java.util.Random;

public class BatteryMonitor {
    private final Context context;
    public BatteryMonitor(Context context) { this.context = context; }

    public String getBatteryInfo() {

        int randomNum = new Random().nextInt(100);
        return "Тест связи: " + randomNum + "%";
    }
}