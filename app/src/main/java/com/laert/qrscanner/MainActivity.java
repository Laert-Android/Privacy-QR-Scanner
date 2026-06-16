package com.laert.qrscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private PreviewView previewView;
    private TextView tvResult;
    private LinearLayout bottomSheet;
    private Button btnCopy, btnShare, btnScanAgain, btnOpen;
    private ExecutorService cameraExecutor;
    private boolean isScanned = false;
    private String lastResult = "";

    @SuppressLint("MissingInflatedId")
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
        btnOpen = findViewById(R.id.btnCopy);
        cameraExecutor = Executors.newSingleThreadExecutor();

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

        btnOpen.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lastResult));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open this link", Toast.LENGTH_SHORT).show();
            }
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

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isScanned) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(
                                mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                .build();

                        BarcodeScanner scanner = BarcodeScanning.getClient(options);
                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {
                                    if (!barcodes.isEmpty() && !isScanned) {
                                        isScanned = true;
                                        lastResult = barcodes.get(0).getRawValue();
                                        runOnUiThread(() -> {
                                            tvResult.setText(lastResult);
                                            bottomSheet.setVisibility(View.VISIBLE);
                                            vibrate();
                                            // Show Open button only if result is a URL
                                            if (lastResult.startsWith("http://") ||
                                                    lastResult.startsWith("https://") ||
                                                    lastResult.startsWith("yt.be") ||
                                                    lastResult.contains("://") ||
                                                    lastResult.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
                                                btnOpen.setVisibility(View.VISIBLE);
                                            } else {
                                                btnOpen.setVisibility(View.GONE);
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> e.printStackTrace())
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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