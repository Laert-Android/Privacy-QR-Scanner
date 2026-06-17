package com.laert.qrscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private View scanLine;
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private PreviewView previewView;
    private TextView tvResult;
    private LinearLayout bottomSheet;
    private Button btnCopy, btnShare, btnScanAgain, btnOpen;
    private ExecutorService cameraExecutor;
    private boolean isScanned = false;
    private String lastResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        tvResult = findViewById(R.id.tvResult);
        bottomSheet = findViewById(R.id.bottomSheet);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);
        btnScanAgain = findViewById(R.id.btnScanAgain);
        btnOpen = findViewById(R.id.btnOpen);
        scanLine = findViewById(R.id.scanLine);
        cameraExecutor = Executors.newSingleThreadExecutor();

        Log.d("QRScanner", "App started");

        startScanAnimation();

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("QR Result", lastResult);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, lastResult);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        btnScanAgain.setOnClickListener(v -> {
            isScanned = false;
            btnOpen.setVisibility(View.GONE);
            bottomSheet.setVisibility(View.GONE);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startScanAnimation() {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(
                scanLine, "translationY", -125f, 125f);
        animator.setDuration(1500);
        animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.start();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                MultiFormatReader reader = new MultiFormatReader();

                imageAnalysis.setAnalyzer(cameraExecutor, (ImageProxy imageProxy) -> {
                    Log.d("QRScanner", "Analyzer running");

                    if (isScanned) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {
                        android.media.Image.Plane[] planes = mediaImage.getPlanes();
                        byte[] data = new byte[planes[0].getBuffer().remaining()];
                        planes[0].getBuffer().get(data);

                        int width = mediaImage.getWidth();
                        int height = mediaImage.getHeight();

                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                                data, width, height, 0, 0, width, height, false);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                        try {
                            Result result = reader.decode(bitmap);
                            if (result != null && !isScanned) {
                                isScanned = true;
                                lastResult = result.getText();
                                Log.d("QRScanner", "Scanned: " + lastResult);
                                runOnUiThread(() -> handleResult(lastResult));
                            }
                        } catch (NotFoundException e) {
                            Log.d("QRScanner", "No QR found in frame");
                        } finally {
                            reader.reset();
                        }
                    }
                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleResult(String result) {
        tvResult.setText(result);
        bottomSheet.setVisibility(View.VISIBLE);
        vibrate();

        if (result.startsWith("WIFI:")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Connect WiFi");
            btnOpen.setOnClickListener(v -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Open WiFi settings to connect", Toast.LENGTH_SHORT).show();
            });

        } else if (result.startsWith("tel:")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Call");
            btnOpen.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(result));
                startActivity(intent);
            });

        } else if (result.startsWith("smsto:") || result.startsWith("sms:")
                || result.startsWith("SMSTO:") || result.startsWith("SMS:")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Send SMS");
            btnOpen.setOnClickListener(v -> {
                String sms = result.replaceFirst("(?i)smsto:|sms:", "");
                String[] parts = sms.split(":");
                String number = parts[0];
                String message = parts.length > 1 ? parts[1] : "";
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                intent.putExtra("sms_body", message);
                startActivity(intent);
            });

        } else if (result.startsWith("mailto:") || result.startsWith("MAILTO:")
                || result.contains("@")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Send Email");
            btnOpen.setOnClickListener(v -> {
                String mailto = result.startsWith("mailto:") ? result : "mailto:" + result;
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(mailto));
                startActivity(intent);
            });

        } else if (result.startsWith("geo:")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Open Map");
            btnOpen.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result));
                startActivity(intent);
            });

        } else if (result.startsWith("http://") || result.startsWith("https://")
                || result.contains(".")) {
            btnOpen.setVisibility(View.VISIBLE);
            btnOpen.setText("Open");
            btnOpen.setOnClickListener(v -> {
                try {
                    String url = result;
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open this link", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            btnOpen.setVisibility(View.GONE);
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}