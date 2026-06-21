package com.example.perangkatsensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor accelSensor;
    private LocationManager locationManager;

    private TextView tvLightValue, tvLightStatus;
    private TextView tvAccelValue, tvAccelStatus;
    private TextView tvLatitude, tvLongitude, tvLocationStatus;

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI Initialization
        tvLightValue = findViewById(R.id.tvLightValue);
        tvLightStatus = findViewById(R.id.tvLightStatus);
        tvAccelValue = findViewById(R.id.tvAccelValue);
        tvAccelStatus = findViewById(R.id.tvAccelStatus);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        // Handle Window Insets for Edge-to-Edge (Status Bar & Navigation Bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sensor Initialization
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Location Initialization
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Memperbarui Sensor...", Toast.LENGTH_SHORT).show();
            checkPermissionsAndStartLocation();
        });

        checkPermissionsAndStartLocation();
    }

    private void checkPermissionsAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
            } else {
                tvLocationStatus.setText("Status: GPS Mati / Tidak Aktif");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Izin lokasi tidak diberikan", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (accelSensor != null) {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // --- SensorEventListener Implementation ---
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            tvLightValue.setText(String.format(Locale.getDefault(), "%.1f Lux", lux));
            
            // Hanya memperbarui teks status, tidak mengganti tema UI
            if (lux < 20) {
                tvLightStatus.setText("Kondisi: Lingkungan Gelap");
            } else if (lux < 100) {
                tvLightStatus.setText("Kondisi: Cahaya Redup");
            } else {
                tvLightStatus.setText("Kondisi: Ruangan Terang");
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            tvAccelValue.setText(String.format(Locale.getDefault(), "X: %.2f | Y: %.2f | Z: %.2f", x, y, z));
            
            // Orientation status logic
            if (Math.abs(x) < 1.5 && Math.abs(y) < 1.5 && Math.abs(z - 9.8) < 1.5) {
                tvAccelStatus.setText("Status: Perangkat Stabil / Datar");
            } else {
                tvAccelStatus.setText("Status: Perangkat Bergerak / Miring");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // --- LocationListener Implementation ---
    @Override
    public void onLocationChanged(@NonNull Location location) {
        tvLatitude.setText(String.format(Locale.getDefault(), "Latitude: %.4f", location.getLatitude()));
        tvLongitude.setText(String.format(Locale.getDefault(), "Longitude: %.4f", location.getLongitude()));
        tvLocationStatus.setText("Status: Lokasi Terkunci (GPS)");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                tvLocationStatus.setText("Status: Izin Lokasi Ditolak");
            }
        }
    }
}
