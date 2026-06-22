package com.system.monitor;

import android.content.*;
import android.os.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private MonitorService mService;
    private boolean mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MonitorService.LocalBinder) service).getService();
            mBound = true;
            setupUI();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { mBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Устанавливаем тему Material Components напрямую из библиотеки
        setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().hide();
        }

        Intent intent = new Intent(this, MonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void setupUI() {
        ViewPager2 vp = findViewById(R.id.viewPager);
        TabLayout tab = findViewById(R.id.tabLayout);

        // Эффект плавного затухания страниц при свайпе
        vp.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos * 0.15f);
        });

        vp.setAdapter(new ViewPagerAdapter(this, mService));

        // Названия вкладок на русском языке
        String[] tabs = {"ОНЛАЙН", "ПРОЦЕССОР", "УСТРОЙСТВО", "СИСТЕМА", "БАТАРЕЯ", "ТЕСТ"};
        new TabLayoutMediator(tab, vp, (t, p) -> t.setText(tabs[p])).attach();

        // Отображение модели устройства в шапке
        String model = Build.MANUFACTURER + " " + Build.MODEL;
        if (model.length() > 30) model = model.substring(0, 30) + "…";
        TextView tvModel = findViewById(R.id.tvDeviceModel);
        if (tvModel != null) tvModel.setText(model);

        // Кнопка копирования всего отчета
        if (findViewById(R.id.btnCopyAll) != null) {
            findViewById(R.id.btnCopyAll).setOnClickListener(v -> copyAllData());
        }
    }

    private void copyAllData() {
        if (!mBound || mService == null) return;
        StringBuilder sb = new StringBuilder();

        // Локализация финального текстового отчета
        sb.append("=== Отчет о системе (SysInfo) ===\n\n");
        appendSection(sb, "ПРОЦЕССОР (SOC)", mService.getSocData());
        appendSection(sb, "УСТРОЙСТВО", mService.getDeviceInfo());
        appendSection(sb, "СИСТЕМА", mService.getSystemInfo());
        appendSection(sb, "БАТАРЕЯ", mService.getBatteryInfo());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SysInfo", sb.toString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            // Всплывашка на русском
            Toast.makeText(this, "✓ Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
        }
    }

    private void appendSection(StringBuilder sb, String title, String[][] data) {
        if (data == null) return;
        sb.append("--- ").append(title).append(" ---\n");
        for (String[] row : data) {
            sb.append(row[0]).append(": ").append(row[1]).append("\n");
        }
        sb.append("\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(connection);
            mBound = false;
        }
    }
}