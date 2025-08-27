package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScanFace extends AppCompatActivity {
    private PreviewView previewView;
    private int employeeId;
    private Employee emp;

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

        employeeId = getIntent().getIntExtra("employeeId", -1);
        if (employeeId == -1) {
            Toast.makeText(this, "Invalid Employee", Toast.LENGTH_SHORT).show();
            finish();
        }

        previewView = findViewById(R.id.previewView);
//        startCamera();
        testFaceFromDrawable();
    }

    private void testFaceFromDrawable() {
        // โหลดรูปตัวอย่างจาก drawable
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_face);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // สร้าง FaceDetector แบบเดียวกับตอนใช้กล้อง
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

                        // แปลง Face เป็น Feature Vector
                        String detectedFaceData = convertFaceToData(faces.get(0));
                        String savedFaceData = employee.faceData;

                        if (detectedFaceData.equals(savedFaceData)) {
                            Toast.makeText(this, "Face match! Employee ID: " + employeeId, Toast.LENGTH_SHORT).show();

                            // TODO: บันทึกเวลาเข้า/ออกงาน
                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
                            int minute = calendar.get(java.util.Calendar.MINUTE);

                            String period;
                            if (hour >= 6 && hour < 12) {
                                period = "เช้า"; // 6:00 - 11:59
                            } else if (hour >= 12 && hour < 14) {
                                period = "กลางวัน"; // 12:00 - 13:59
                            } else if (hour >= 14 && hour < 18) {
                                period = "บ่าย"; // 14:00 - 17:59
                            } else {
                                period = "เย็น"; // 18:00 - 5:59
                            }

                            String timeStr = String.format("%02d:%02d", hour, minute);
                            emp.insertAttendance(employeeId, period, timeStr);
                            Toast.makeText(this, "บันทึกเวลา: " + period + " " + timeStr, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Face not found in drawable!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

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
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();

                FaceDetector detector = FaceDetection.getClient(options);

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    @SuppressWarnings("UnsafeOptInUsageError")
                    Image mediaImage = image.getImage();
                    if (mediaImage != null) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                        detector.process(inputImage)
                                .addOnSuccessListener(faces -> processFaces(faces))
                                .addOnFailureListener(Throwable::printStackTrace)
                                .addOnCompleteListener(result -> image.close());
                    } else {
                        image.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processFaces(List<Face> faces) {
        if (!faces.isEmpty()) {
            emp = new Employee(this);
            MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
            if (employee == null) {
                Toast.makeText(this, "Employee not found!", Toast.LENGTH_SHORT).show();
                return;
            }

            String savedFaceData = employee.faceData;

            // แปลง Face จากกล้องเป็น Feature Vector
            String detectedFaceData = convertFaceToData(faces.get(0));

            if (detectedFaceData.equals(savedFaceData)) {
                Toast.makeText(this, "Face match! Employee ID: " + employeeId, Toast.LENGTH_SHORT).show();
                // TODO: บันทึกเวลาเข้า/ออกงาน
            } else {
                Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // แปลง Face เป็น Feature Vector
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
}