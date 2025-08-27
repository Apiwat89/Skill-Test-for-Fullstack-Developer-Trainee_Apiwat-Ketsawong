package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Employee extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "attendance.db";
    private static final int DATABASE_VERSION = 3;

    public Employee(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // employees table
        sqLiteDatabase.execSQL("CREATE TABLE employees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "position TEXT," +
                "faceEmbedding BLOB)");

        // attendance table
        sqLiteDatabase.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employeeId INTEGER," +
                "timestamp TEXT," +
                "type TEXT," +
                "FOREIGN KEY(employeeId) REFERENCES employees(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS attendance");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS employees");
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Insert employee with embedding
    public void insertEmployeeWithEmbedding(String name, String position, byte[] embedding) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", position);
        values.put("faceEmbedding", floatArrayToByteArray(embedding));
        db.insert("employees", null, values);
        db.close();
    }

    // แปลง float[] -> byte[]
    private byte[] floatArrayToByteArray(byte[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        for (float f : array) buffer.putFloat(f);
        return buffer.array();
    }

//    // แปลง byte[] -> float[]
//    private float[] byteArrayToFloatArray(byte[] bytes) {
//        float[] array = new float[bytes.length / 4];
//        ByteBuffer.wrap(bytes).asFloatBuffer().get(array);
//        return array;
//    }
//
//    // ดึง embedding ของพนักงาน
//    public float[] getEmployeeEmbedding(int employeeId) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT faceEmbedding FROM employees WHERE id = ?", new String[]{String.valueOf(employeeId)});
//        float[] embedding = null;
//        if (cursor.moveToFirst()) {
//            byte[] blob = cursor.getBlob(0);
//            embedding = byteArrayToFloatArray(blob);
//        }
//        cursor.close();
//        db.close();
//        return embedding;
//    }

    // ดึงข้อมูลพนักงานทั้งหมด
    public List<MainActivity.EmployeeModel> getAllEmployees() {
        List<MainActivity.EmployeeModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, position, faceEmbedding FROM employees", null);
        if (cursor.moveToFirst()) {
            do {
                MainActivity.EmployeeModel em = new MainActivity.EmployeeModel();
                em.id = cursor.getInt(0);
                em.name = cursor.getString(1);
                em.position = cursor.getString(2);
                em.faceEmbedding = cursor.getBlob(3);
                list.add(em);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // ดึงพนักงานตาม id
    public MainActivity.EmployeeModel getEmployeeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, position, faceEmbedding FROM employees WHERE id = ?", new String[]{String.valueOf(id)});
        MainActivity.EmployeeModel employee = null;
        if (cursor.moveToFirst()) {
            employee = new MainActivity.EmployeeModel();
            employee.id = cursor.getInt(0);
            employee.name = cursor.getString(1);
            employee.position = cursor.getString(2);
            employee.faceEmbedding = cursor.getBlob(3);
        }
        cursor.close();
        db.close();
        return employee;
    }

    // insert attendance
    public void insertAttendance(int employeeId, String timestamp, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("employeeId", employeeId);
        values.put("timestamp", timestamp);
        values.put("type", type);
        db.insert("attendance", null, values);
        db.close();
    }
}