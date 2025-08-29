package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScanFace extends AppCompatActivity {
    private int employeeId;
    private Employee emp;
    private boolean faceRegistered = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private android.location.Location currentLocation;

    // ปักหมุดไว้ที่ https://maps.app.goo.gl/tDUvzRgVJJqD2itC7
    private static final double OFFICE_LAT = 13.722363592997352;
    private static final double OFFICE_LNG = 100.5295836254038;
    private static final float ALLOWED_RADIUS_METERS = 200f;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        emp = new Employee(this);

        employeeId = getIntent().getIntExtra("employeeId", -1);
        if (employeeId == -1) {
            Toast.makeText(this, "Invalid Employee", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadModel();
        requestPermissions();
        requestLocationPermission();

        initLocationRequest();
        startLocationUpdates();

        startCamera(employeeId);

//        testFaceWithDrawable(employeeId); test
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadModel() {
        try {
            String fileName = "mobilefacenet.tflite";
            File file = new File(getFilesDir(), fileName);
            if (!file.exists()) {
                InputStream is = getAssets().open(fileName);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                fos.close();
                is.close();
            }

            FileInputStream inputStream = new FileInputStream(file);
            FileChannel fileChannel = inputStream.getChannel();
            MappedByteBuffer mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            tfLite = new Interpreter(mappedBuffer);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load TFLite model", Toast.LENGTH_SHORT).show();
        }
    }

    private void initLocationRequest() {
        locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                currentLocation = locationResult.getLastLocation();
                Log.d("LocationUpdate", "Lat: " + currentLocation.getLatitude() + ", Lng: " + currentLocation.getLongitude());
            }
        }, getMainLooper());
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera(int employeeId) {
        PreviewView previewView = findViewById(R.id.previewView);
        FaceOverlayView faceOverlay = findViewById(R.id.faceOverlay);

        previewView.post(() -> {
            int width = previewView.getWidth() * 2 / 3;
            int height = previewView.getHeight() / 3;
            int left = (previewView.getWidth() - width) / 2;
            int top = (previewView.getHeight() - height) / 2;
            RectF rect = new RectF(left, top, left + width, top + height);

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
                                        try {
                                            if (!faces.isEmpty()) {
                                                Face face = faces.get(0);

                                                float scaleX = previewView.getWidth() / (float) mediaImage.getHeight();
                                                float scaleY = previewView.getHeight() / (float) mediaImage.getWidth();
                                                float centerX = face.getBoundingBox().centerX() * scaleX;
                                                float centerY = face.getBoundingBox().centerY() * scaleY;

                                                long currentTime = System.currentTimeMillis();

                                                if (rect.contains(centerX, centerY)) {
                                                    if (faceStartTime[0] == 0) faceStartTime[0] = currentTime;
                                                    else if (currentTime - faceStartTime[0] >= REQUIRED_DURATION_MS) {
                                                        float[] embedding = getFaceEmbedding(imageProxy, face);
                                                        if (embedding == null) {
                                                            Toast.makeText(this, "Unable to capture face. Try again.", Toast.LENGTH_SHORT).show();
                                                            imageProxy.close();
                                                            return;
                                                        }

                                                        byte[] savedFaceBytes = employee.faceEmbedding;
                                                        float[] savedEmbedding = byteArrayToFloatArray(savedFaceBytes);

                                                        float distance = calculateDistance(embedding, savedEmbedding);

                                                        if (distance < 0.7f) {
                                                            faceRegistered = true;
                                                            Toast.makeText(this, "Face matched!", Toast.LENGTH_SHORT).show();
                                                            checkLocationAndSaveFace(employee);
                                                        } else {
                                                            runOnUiThread(() ->
                                                                    Toast.makeText(this, "Face not match!", Toast.LENGTH_SHORT).show()
                                                            );
                                                        }
                                                    }
                                                } else faceStartTime[0] = 0;
                                            } else faceStartTime[0] = 0;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            imageProxy.close();
                                        }
                                    })
                                    .addOnFailureListener(e -> imageProxy.close());
                        } else {
                            imageProxy.close();
                        }
                    });

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));
        });
    }

    @ExperimentalGetImage
    private float[] getFaceEmbedding(ImageProxy imageProxy, Face face) {
        try {
            Bitmap bitmap = toBitmap(imageProxy);
            if (bitmap == null) return null;

            Rect bounds = new Rect(face.getBoundingBox());
            bounds.left   = Math.max(0, bounds.left);
            bounds.top    = Math.max(0, bounds.top);
            bounds.right  = Math.min(bitmap.getWidth(), bounds.right);
            bounds.bottom = Math.min(bitmap.getHeight(), bounds.bottom);

            int faceWidth = bounds.width();
            int faceHeight = bounds.height();
            if (faceWidth <= 0 || faceHeight <= 0) return null;

            Bitmap faceBitmap;
            try {
                faceBitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, faceWidth, faceHeight);
            } catch (Exception e) {return null;}

            Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true);

            ByteBuffer input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
            input.order(ByteOrder.nativeOrder());

            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    int px = resized.getPixel(x, y);

                    float r = ((px >> 16) & 0xFF) / 255.0f;
                    float g = ((px >> 8) & 0xFF) / 255.0f;
                    float b = (px & 0xFF) / 255.0f;

                    input.putFloat((r - 0.5f) * 2f);
                    input.putFloat((g - 0.5f) * 2f);
                    input.putFloat((b - 0.5f) * 2f);
                }
            }

            float[][] embedding = new float[1][EMBEDDING_SIZE];
            tfLite.run(input, embedding);

            return embedding[0];
        } catch (Exception e) {return null;}
    }

    private Bitmap toBitmap(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();
        if (image == null) return null;

        YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(this, imageProxy.getWidth(), imageProxy.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        byte[] nv21 = yuv420ToNv21(image);
        yuvToRgbConverter.yuvToRgb(nv21, bitmap);

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    private byte[] yuv420ToNv21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
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

    private float calculateDistance(float[] a, float[] b) {
        if (a.length != b.length) return Float.MAX_VALUE;
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    private void checkLocationAndSaveFace(MainActivity.EmployeeModel employee) {
        if (currentLocation == null) {
            Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    new CancellationTokenSource().getToken()
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    checkLocationAndSaveFace(employee);
                } else {
                    Toast.makeText(this, "Cannot get location", Toast.LENGTH_SHORT).show();
                    faceRegistered = false;
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                faceRegistered = false;
            });

            return;
        }

        float[] results = new float[1];
        android.location.Location.distanceBetween(
                OFFICE_LAT, OFFICE_LNG,
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                results
        );
        float distanceInMeters = results[0];

        if (distanceInMeters <= ALLOWED_RADIUS_METERS) {
            saveAttendance(employee);
        } else {
            Toast.makeText(this, "Outside office zone: " + distanceInMeters + "m", Toast.LENGTH_LONG).show();
            faceRegistered = false;
        }
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
        Toast.makeText(this, "Saved: " + type + " " + timeStr, Toast.LENGTH_SHORT).show();

        runOnUiThread(() -> {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }, 2000);
        });
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
//            Log.d("FaceCheck", "Embeddings matched"); // <-- เช็คว่าเข้า if หรือไม่
//            Toast.makeText(this, "Face matched!", Toast.LENGTH_SHORT).show();
//            Log.d("FaceCheck", "Calling checkLocationAndSaveFace"); // <-- เช็คว่าเรียกฟังก์ชันต่อ
//            checkLocationAndSaveFace(employee);
//        } else {
//            Toast.makeText(this, "Face mismatch!", Toast.LENGTH_SHORT).show();
//        }
//    }
}
