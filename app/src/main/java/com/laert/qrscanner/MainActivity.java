package com.laert.qrscanner;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.FrameLayout;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle edge-to-edge display for Samsung and other devices
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    int bottomInset = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
                    BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
                    bottomNav.setPadding(0, 0, 0, bottomInset);
                    FrameLayout container = findViewById(R.id.fragmentContainer);
                    container.setPadding(0, 0, 0, 0);
                    ViewGroup.MarginLayoutParams params =
                            (ViewGroup.MarginLayoutParams) container.getLayoutParams();
                    params.bottomMargin = 56 + bottomInset;
                    container.setLayoutParams(params);
                    return insets;
                });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Load scan fragment by default
        if (savedInstanceState == null) {
            loadFragment(new ScanFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_scan) {
                loadFragment(new ScanFragment());
                return true;
            } else if (id == R.id.nav_generate) {
                loadFragment(new GenerateFragment());
                return true;
            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}