package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Base64;
import android.widget.Toast;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import android.Manifest;

import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.face.FaceLandmark;

public class Register extends AppCompatActivity implements View.OnClickListener {
    private TextInputEditText InputNameEmployee, InputPositionEmployee;
    private Button btnNextRegister;
    private Employee emp;
    private boolean faceSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        InputNameEmployee = findViewById(R.id.InputNameEmployee);
        InputPositionEmployee = findViewById(R.id.InputPositonEmployee);
        btnNextRegister = findViewById(R.id.btnNextRegister);

        btnNextRegister.setOnClickListener(this);
        requestPermissions();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnNextRegister) {
            if (InputNameEmployee.getText().toString().isEmpty()) {
                InputNameEmployee.setError("Please enter name");
            } else if (InputPositionEmployee.getText().toString().isEmpty()) {
                InputPositionEmployee.setError("Please enter position");
            } else {
                InputNameEmployee.setVisibility(View.GONE);
                InputPositionEmployee.setVisibility(View.GONE);
                btnNextRegister.setVisibility(View.GONE);

                findViewById(R.id.faceScannerView).setVisibility(View.VISIBLE);
                startFaceScanner(InputNameEmployee.getText().toString(), InputPositionEmployee.getText().toString());
//                testFaceFromImage();
            }
        }
    }

    private void startFaceScanner(@NonNull String name, @NonNull String position) {
        PreviewView previewView = findViewById(R.id.faceScannerView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image Analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();

                FaceDetector detector = FaceDetection.getClient(options);

                final int REQUIRED_FRAMES = 3; // ต้องเจอใบหน้าติดกัน 3 frame
                final int[] faceCounter = {0};

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null && !faceSaved) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        detector.process(inputImage)
                                .addOnSuccessListener(faces -> {
                                    if (!faces.isEmpty()) {
                                        faceCounter[0]++;
                                        if (faceCounter[0] >= REQUIRED_FRAMES) {
                                            Face face = faces.get(0);

                                            // แปลง MediaImage -> Bitmap
                                            Bitmap frameBitmap = toBitmap(mediaImage);
                                            Bitmap faceBitmap = cropFace(frameBitmap, face);

                                            // สร้าง feature vector จาก faceBitmap หรือใช้ convertFaceToData
                                            String faceData = convertFaceToData(face);

                                            emp = new Employee(this);
                                            emp.insertEmployee(name, position, faceData);
                                            faceSaved = true;

                                            Toast.makeText(this, "Face registered successfully!", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    } else {
                                        faceCounter[0] = 0; // reset ถ้าไม่มีใบหน้า
                                    }
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ฟังก์ชันแปลง MediaImage -> Bitmap
    private Bitmap toBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // Crop ใบหน้า
    private Bitmap cropFace(Bitmap frameBitmap, Face face) {
        Rect bounds = face.getBoundingBox();
        int x = Math.max(bounds.left, 0);
        int y = Math.max(bounds.top, 0);
        int width = Math.min(bounds.width(), frameBitmap.getWidth() - x);
        int height = Math.min(bounds.height(), frameBitmap.getHeight() - y);
        return Bitmap.createBitmap(frameBitmap, x, y, width, height);
    }

    // แปลง Face เป็น Feature Vector แบบง่าย
    private String convertFaceToData(Face face) {
        StringBuilder sb = new StringBuilder();
        sb.append(face.getBoundingBox().exactCenterX()).append(",")
                .append(face.getBoundingBox().exactCenterY());

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

    private void testFaceFromImage() {
        // โหลดรูปจาก drawable
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_face);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // สร้าง FaceDetector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // ตรวจ landmark เพื่อ feature vector
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        Face face = faces.get(0);

                        // แปลงเป็น feature vector
                        String faceData = convertFaceToData(face);

                        // บันทึกลง DB
                        emp = new Employee(this);
                        String name = InputNameEmployee.getText().toString().trim();
                        String position = InputPositionEmployee.getText().toString().trim();
                        emp.insertEmployee(name, position, faceData);

                        Toast.makeText(this, "Employee saved with face data!", Toast.LENGTH_SHORT).show();
                        Toast.makeText(this, "Face registered successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Face not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}