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
    private static final int DATABASE_VERSION = 4;

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

    // Insert employee
    public void insertEmployeeWithEmbedding(String name, String position, byte[] embedding) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", position);
        values.put("faceEmbedding", embedding);
        db.insert("employees", null, values);
        db.close();
    }

    // Pull all employee data into MainActivity
    public List<MainActivity.EmployeeModel> getAllEmployeesMainActivity() {
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

    // Pull all employee data into a report
    public List<Report.EmployeeModel> getAllEmployeesReport() {
        List<Report.EmployeeModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, position, faceEmbedding FROM employees", null);
        if (cursor.moveToFirst()) {
            do {
                Report.EmployeeModel em = new Report.EmployeeModel();
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

    // Pull employee data by ID on the MainActivity page.
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

    // Pull attendance data with report page code
    public List<ReportPerson.AttendanceRecord> getAttendanceByEmployee(int employeeId) {
        List<ReportPerson.AttendanceRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT timestamp, type FROM attendance WHERE employeeId = ? ORDER BY timestamp DESC",
                new String[]{String.valueOf(employeeId)}
        );
        if (cursor.moveToFirst()) {
            do {
                ReportPerson.AttendanceRecord record = new ReportPerson.AttendanceRecord();
                record.timestamp = cursor.getString(0);
                record.type = cursor.getString(1);
                list.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}