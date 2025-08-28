package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import org.tensorflow.lite.Interpreter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import android.Manifest;

import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.face.FaceLandmark;

import androidx.camera.core.ExperimentalGetImage;

public class Register extends AppCompatActivity implements View.OnClickListener {
    private TextInputEditText InputNameEmployee, InputPositionEmployee;
    private Button btnNextRegister;
    private Employee emp;
    private boolean faceSaved = false;

    // TFLite
    private Interpreter tfLite;
    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_SIZE = 192;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        InputNameEmployee = findViewById(R.id.InputNameEmployee);
        InputPositionEmployee = findViewById(R.id.InputPositonEmployee);
        btnNextRegister = findViewById(R.id.btnNextRegister);

        btnNextRegister.setOnClickListener(this);
        loadModel();

//        testFaceRegistrationWithDrawable(); // test
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
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
                requestPermissions();

                String name = InputNameEmployee.getText().toString().trim();
                String position = InputPositionEmployee.getText().toString().trim();

                InputNameEmployee.setVisibility(View.GONE);
                InputPositionEmployee.setVisibility(View.GONE);
                btnNextRegister.setVisibility(View.GONE);

                findViewById(R.id.cameraContainer).setVisibility(View.VISIBLE);
                startFaceScanner(name, position);
            }
        }
    }

    private void loadModel() {
        try {
            // copy จาก assets -> internal storage
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

            // โหลด model จาก internal storage
            FileInputStream inputStream = new FileInputStream(file);
            FileChannel fileChannel = inputStream.getChannel();
            MappedByteBuffer mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            tfLite = new Interpreter(mappedBuffer);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load TFLite model", Toast.LENGTH_SHORT).show();
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startFaceScanner(@NonNull String name, @NonNull String position) {
        PreviewView previewView = findViewById(R.id.faceScannerView);
        FaceOverlayView faceOverlay = findViewById(R.id.faceOverlay);

        previewView.post(() -> {
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

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
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

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                        @SuppressLint("UnsafeOptInUsageError")
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null && !faceSaved) {
                            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            detector.process(inputImage)
                                    .addOnSuccessListener(faces -> {
                                        try {
                                            if (!faces.isEmpty()) {
                                                Face face = faces.get(0);

                                                // ตรวจสอบใบหน้าอยู่ในกรอบ
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
                                                            Toast.makeText(this, "Unable to capture face. Please scan again.", Toast.LENGTH_SHORT).show();
                                                            return;
                                                        }

                                                        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                                                        for (float f : embedding) buffer.putFloat(f);
                                                        byte[] faceBytes = buffer.array();

                                                        emp = new Employee(this);
                                                        emp.insertEmployeeWithEmbedding(name, position, faceBytes);

                                                        faceSaved = true;
                                                        Toast.makeText(this, "Face registered successfully!", Toast.LENGTH_SHORT).show();

                                                        Intent intent = new Intent(this, MainActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
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
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                } catch (Exception e) {
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

            ByteBuffer input = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
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

        YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(this, 640, 480);
        if (yuvToRgbConverter == null) {
            yuvToRgbConverter = new YuvToRgbConverter(this, imageProxy.getWidth(), imageProxy.getHeight());
        }

        Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        byte[] nv21 = yuv420ToNv21(image);
        yuvToRgbConverter.yuvToRgb(nv21, bitmap);

        // หมุนตาม orientation
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

//        private void testFaceRegistrationWithDrawable() {
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_face);
//
//        if (bitmap == null) {
//            Toast.makeText(this, "Cannot load drawable", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // สร้าง InputImage จาก Bitmap
//        InputImage image = InputImage.fromBitmap(bitmap, 0); // 0 หมายถึง rotation
//
//        // สร้าง FaceDetector
//        FaceDetector detector = FaceDetection.getClient(
//                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
//                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
//                        .setMinFaceSize(0.2f)
//                        .enableTracking()
//                        .build()
//        );
//
//        // ประมวลผลรูป
//        detector.process(image)
//                .addOnSuccessListener(faces -> {
//                    if (!faces.isEmpty()) {
//                        // ใช้ Face ตัวแรก
//                        Face face = faces.get(0);
//
//                        // เรียกฟังก์ชัน embedding
//                        float[] embedding = getFaceEmbedding(bitmap, face);
//
//                        // แสดงผล embedding size
//                        Toast.makeText(this, "Embedding size: " + embedding.length, Toast.LENGTH_LONG).show();
//
//                        // ทดสอบบันทึกลง DB
//                        emp = new Employee(this);
//                        emp.insertEmployeeWithEmbedding("Test Name", "Developer", embedding);
//
//                        Toast.makeText(this, "Inserted into DB", Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(this, "No face detected in drawable", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> Toast.makeText(this, "Face detection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//    }
//
//    public float[] getFaceEmbedding(Bitmap bitmap, Face face) {
//        Rect bounds = face.getBoundingBox();
//        bounds.intersect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//        Bitmap faceBitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
//        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true);
//
//        int width = resized.getWidth();
//        int height = resized.getHeight();
//        float[] embedding = new float[width * height * 3];
//        int idx = 0;
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int pixel = resized.getPixel(x, y);
//                embedding[idx++] = ((pixel >> 16 & 0xFF) - 128f) / 128f;
//                embedding[idx++] = ((pixel >> 8 & 0xFF) - 128f) / 128f;
//                embedding[idx++] = ((pixel & 0xFF) - 128f) / 128f;
//            }
//        }
//        return embedding;
//    }
}