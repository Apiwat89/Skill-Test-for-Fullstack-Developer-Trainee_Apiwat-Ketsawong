package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
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
import com.google.android.gms.tasks.CancellationTokenSource;
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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScanFace extends AppCompatActivity {
    private PreviewView previewView;
    private int employeeId;
    private Employee emp;
    private boolean faceRegistered = false;
    private boolean faceMismatchShown = false;

    private FusedLocationProviderClient fusedLocationClient;
    private static final double OFFICE_LAT = 37.4220936;
    private static final double OFFICE_LNG = -122.083922;
    private static final float ALLOWED_RADIUS_METERS = 200f;
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    // TFLite
    private Interpreter tfLite;
    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_SIZE = 192;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        emp = new Employee(this);

        employeeId = getIntent().getIntExtra("employeeId", -1);
        if (employeeId == -1) {
            Toast.makeText(this, "Invalid Employee", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadModel();

        if (checkPermissions()) startCamera(employeeId);
        else requestPermissions();

//        testFaceWithDrawable(employeeId); // test
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
        if (requestCode == REQUEST_CODE_PERMISSIONS && checkPermissions()) {
            startCamera(employeeId);
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private MappedByteBuffer loadModelFile(String modelFile) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadModel() {
        try {
            tfLite = new Interpreter(loadModelFile("mobilefacenet.tflite"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load TFLite model", Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera(int employeeId) {
        FaceOverlayView faceOverlay = findViewById(R.id.faceOverlay);

        previewView.post(() -> {
            // สร้างกรอบใบหน้า
            int width = previewView.getWidth() * 2 / 3;
            int height = previewView.getHeight() / 3;
            int left = (previewView.getWidth() - width) / 2;
            int top = (previewView.getHeight() - height) / 2;
            RectF rect = new RectF(left, top, left + width, top + height);
            faceOverlay.setFaceRect(rect);

            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    ImageAnalysis analysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                            .setMinFaceSize(0.2f)
                            .enableTracking()
                            .build();

                    FaceDetector detector = FaceDetection.getClient(options);

                    final long REQUIRED_DURATION_MS = 2000;
                    final long[] faceStartTime = {0};

                    MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
                    if (employee == null) {
                        Toast.makeText(this, "Employee not found!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    analysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                        @SuppressLint("UnsafeOptInUsageError")
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null && !faceRegistered) {
                            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            detector.process(inputImage)
                                    .addOnSuccessListener(faces -> {
                                        if (!faces.isEmpty()) {
                                            Face face = faces.get(0);

                                            float scaleX = (float) previewView.getWidth() / mediaImage.getHeight();
                                            float scaleY = (float) previewView.getHeight() / mediaImage.getWidth();
                                            float centerX = face.getBoundingBox().centerX() * scaleX;
                                            float centerY = face.getBoundingBox().centerY() * scaleY;

                                            long currentTime = System.currentTimeMillis();

                                            if (rect.contains(centerX, centerY)) {
                                                if (faceStartTime[0] == 0) faceStartTime[0] = currentTime;
                                                else if (currentTime - faceStartTime[0] >= REQUIRED_DURATION_MS) {
                                                    float[] embedding = getFaceEmbedding(mediaImage, face);
                                                    byte[] savedFaceBytes = employee.faceEmbedding;
                                                    float[] savedEmbedding = byteArrayToFloatArray(savedFaceBytes);

                                                    if (compareEmbeddings(embedding, savedEmbedding)) {
                                                        faceRegistered = true;
                                                        Toast.makeText(this, "Face matched!", Toast.LENGTH_SHORT).show();
                                                        checkLocationAndSaveFace(employee);
                                                    } else {
                                                        if (!faceMismatchShown) {
                                                            faceMismatchShown = true;
                                                            Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show();
                                                            new android.os.Handler().postDelayed(() -> faceMismatchShown = false, 2000);
                                                        }
                                                    }
                                                }
                                            } else faceStartTime[0] = 0;
                                        } else faceStartTime[0] = 0;
                                        imageProxy.close();
                                    })
                                    .addOnFailureListener(e -> imageProxy.close());
                        } else {
                            imageProxy.close();
                        }
                    });

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));
        });
    }

    @ExperimentalGetImage
    public float[] getFaceEmbedding(Image mediaImage, Face face) {
        Bitmap bitmap = toBitmap(mediaImage);
        Rect bounds = face.getBoundingBox();
        bounds.intersect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Bitmap faceBitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());

        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        imgData.rewind();

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int px = resized.getPixel(x, y);
                float r = ((px >> 16) & 0xFF) / 255f;
                float g = ((px >> 8) & 0xFF) / 255f;
                float b = (px & 0xFF) / 255f;
                imgData.putFloat((r - 0.5f) * 2);
                imgData.putFloat((g - 0.5f) * 2);
                imgData.putFloat((b - 0.5f) * 2);
            }
        }

        float[][] embedding = new float[1][EMBEDDING_SIZE];
        tfLite.run(imgData, embedding);
        return embedding[0];
    }

    private Bitmap toBitmap(Image mediaImage) {
        if (mediaImage == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(mediaImage.getWidth(), mediaImage.getHeight(), Bitmap.Config.ARGB_8888);
        YuvToRgbConverter converter = new YuvToRgbConverter(this);
        converter.yuvToRgb(mediaImage, bitmap);
        return bitmap;
    }

    private float[] byteArrayToFloatArray(byte[] bytes) {
        if (bytes == null) return null;
        int floatCount = bytes.length / 4;
        float[] floats = new float[floatCount];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < floatCount; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    private boolean compareEmbeddings(float[] a, float[] b) {
        if (a.length != b.length) return false;
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        float distance = (float) Math.sqrt(sum);
        return distance < 1.0f;
    }

    private void checkLocationAndSaveFace(MainActivity.EmployeeModel employee) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                new CancellationTokenSource().getToken()
        ).addOnSuccessListener(location -> {
            if (location != null) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                        OFFICE_LAT, OFFICE_LNG,
                        location.getLatitude(), location.getLongitude(),
                        results
                );
                float distanceInMeters = results[0];

                if (distanceInMeters <= ALLOWED_RADIUS_METERS) saveAttendance(employee);
                else Toast.makeText(this, "Outside office zone: " + distanceInMeters + "m", Toast.LENGTH_LONG).show();
            } else Toast.makeText(this, "Cannot get location", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveAttendance(MainActivity.EmployeeModel employee) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String type;
        if (hour >= 6 && hour < 12) type = "เช้า";
        else if (hour >= 12 && hour < 14) type = "กลางวัน";
        else if (hour >= 14 && hour < 18) type = "บ่าย";
        else type = "เย็น";

        String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date());
        emp.insertAttendance(employee.id, timeStr, type);

        Toast.makeText(this, "Attendance saved: " + type + " " + timeStr, Toast.LENGTH_SHORT).show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, 2000);
    }

//    private void testFaceWithDrawable(int employeeId) {
//        MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
//        if (employee == null) {
//            Toast.makeText(this, "Employee not found!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Mock embedding ให้ตรงกับ DB เลย
//        float[] embedding = byteArrayToFloatArray(employee.faceEmbedding);
//
//        // เปรียบเทียบ embedding → ต้อง match
//        if (compareEmbeddings(embedding, embedding)) {
//            Toast.makeText(this, "Face matched!", Toast.LENGTH_SHORT).show();
//            checkLocationAndSaveFace(employee);
//        } else {
//            Toast.makeText(this, "Face mismatch!", Toast.LENGTH_SHORT).show();
//        }
//    }
}