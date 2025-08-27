package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.location.Location;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScanFace extends AppCompatActivity {
    private PreviewView previewView;
    private FaceOverlayView faceOverlay;
    private int employeeId;
    private Employee emp;

    private boolean faceRegistered = false;
    private FusedLocationProviderClient fusedLocationClient;
    private static final double OFFICE_LAT = 37.4220936;
    private static final double OFFICE_LNG = -122.083922;
    private static final float ALLOWED_RADIUS_METERS = 200f;
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_face);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        faceOverlay = findViewById(R.id.faceOverlay);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        employeeId = getIntent().getIntExtra("employeeId", -1);
        if (employeeId == -1) {
            Toast.makeText(this, "Invalid Employee", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (checkPermissions()) {
            previewView.post(this::startCamera);
//            testFaceFromDrawable();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkPermissions()) {
                previewView.post(this::startCamera);
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();

                FaceDetector detector = FaceDetection.getClient(options);
                final int REQUIRED_FRAMES = 3;
                final int[] faceCounter = {0};

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @android.annotation.SuppressLint("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null && !faceRegistered) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        detector.process(inputImage)
                                .addOnSuccessListener(faces -> {
                                    if (!faces.isEmpty()) {
                                        faceCounter[0]++;
                                        if (faceCounter[0] >= REQUIRED_FRAMES) {
                                            Face face = faces.get(0);

                                            // วาดกรอบตรงกลางหน้าจอ
                                            int width = previewView.getWidth() * 2 / 3;
                                            int height = previewView.getHeight() / 3;
                                            int left = (previewView.getWidth() - width) / 2;
                                            int top = (previewView.getHeight() - height) / 2;
                                            RectF rect = new RectF(left, top, left + width, top + height);
                                            faceOverlay.setFaceRect(rect);

                                            // ตรวจว่าใบหน้าอยู่ในกรอบ
                                            Rect boundingBox = face.getBoundingBox();
                                            if (rect.contains(boundingBox.centerX(), boundingBox.centerY())) {
                                                emp = new Employee(ScanFace.this);
                                                MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
                                                if (employee != null) {
                                                    String detectedFaceData = convertFaceToData(face);
                                                    if (detectedFaceData.equals(employee.faceData)) {
                                                        faceRegistered = true;
                                                        Toast.makeText(this, "Face match!", Toast.LENGTH_SHORT).show();
                                                        checkLocationAndSaveFace(employee);
                                                    } else {
                                                        Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show();
                                                        faceCounter[0] = 0;
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(this, "Please align your face in the frame", Toast.LENGTH_SHORT).show();
                                                faceCounter[0] = 0;
                                            }
                                        }
                                    } else {
                                        faceCounter[0] = 0;
                                    }
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void checkLocationAndSaveFace(MainActivity.EmployeeModel employee) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(OFFICE_LAT, OFFICE_LNG,
                                location.getLatitude(), location.getLongitude(), results);
                        float distanceInMeters = results[0];

                        if (distanceInMeters <= ALLOWED_RADIUS_METERS) {
                            saveAttendance(employee);
                        } else {
                            Toast.makeText(this, "Outside office zone: " + distanceInMeters + "m", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Cannot get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAttendance(MainActivity.EmployeeModel employee) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);

        String period;
        if (hour >= 6 && hour < 12) period = "เช้า";
        else if (hour >= 12 && hour < 14) period = "กลางวัน";
        else if (hour >= 14 && hour < 18) period = "บ่าย";
        else period = "เย็น";

        String timeStr = String.format("%02d:%02d", hour, minute);
        emp = new Employee(this);
        emp.insertAttendance(employee.id, period, timeStr);

        Toast.makeText(this, "Attendance saved: " + period + " " + timeStr, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String convertFaceToData(Face face) {
        StringBuilder sb = new StringBuilder();
        sb.append(face.getBoundingBox().exactCenterX()).append(",");
        sb.append(face.getBoundingBox().exactCenterY());

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

        if (leftEye != null) sb.append(",").append(leftEye.getPosition().x).append(",").append(leftEye.getPosition().y);
        if (rightEye != null) sb.append(",").append(rightEye.getPosition().x).append(",").append(rightEye.getPosition().y);
        if (nose != null) sb.append(",").append(nose.getPosition().x).append(",").append(nose.getPosition().y);
        if (mouth != null) sb.append(",").append(mouth.getPosition().x).append(",").append(mouth.getPosition().y);

        return sb.toString();
    }

    private void testFaceFromDrawable() {
        // โหลดรูปตัวอย่างจาก drawable
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_face);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // สร้าง FaceDetector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        emp = new Employee(this);
                        MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
                        if (employee == null) {
                            Toast.makeText(this, "Employee not found!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // จำลองการแปลงใบหน้าเป็นข้อมูล
                        String detectedFaceData = convertFaceToData(faces.get(0));
                        String savedFaceData = employee.faceData;

                        if (detectedFaceData.equals(savedFaceData)) {
                            Toast.makeText(this, "Face match! Employee ID: " + employee.id, Toast.LENGTH_SHORT).show();
                            checkLocationAndSaveFace(employee);
                        } else {
                            Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Face detection failed", Toast.LENGTH_SHORT).show();
                });
    }
}