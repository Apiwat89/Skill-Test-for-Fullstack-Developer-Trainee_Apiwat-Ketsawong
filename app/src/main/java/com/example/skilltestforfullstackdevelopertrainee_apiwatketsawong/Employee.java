package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Employee extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "attendance.db";
    private static final int DATABASE_VERSION = 2;

    public Employee(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE employees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "position TEXT," +
                "faceData TEXT)");

        sqLiteDatabase.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "employeeId INTEGER," +
                "timestamp TEXT," +
                "type TEXT," +
                "FOREIGN KEY(employeeId) REFERENCES employees(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS attendance");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS employees");
        onCreate(sqLiteDatabase);
    }

    // insert employee
    public void insertEmployee(String name, String position, String faceData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("position", position);
        values.put("faceData", faceData);
        db.insert("employees", null, values);
        db.close();
    }

    //  readAll employee
    public List<MainActivity.EmployeeModel> getAllEmployees() {
        List<MainActivity.EmployeeModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, position, faceData FROM employees", null);
        if(cursor.moveToFirst()){
            do {
                MainActivity.EmployeeModel em = new MainActivity.EmployeeModel();
                em.id = cursor.getInt(0);
                em.name = cursor.getString(1);
                em.position = cursor.getString(2);
                em.faceData = cursor.getString(3);
                list.add(em);
            } while(cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    //  readID employee
    public MainActivity.EmployeeModel getEmployeeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, position, faceData FROM employees WHERE id = ?", new String[]{String.valueOf(id)});
        MainActivity.EmployeeModel employee = null;
        if (cursor.moveToFirst()) {
            employee = new MainActivity.EmployeeModel();
            employee.id = cursor.getInt(0);
            employee.name = cursor.getString(1);
            employee.position = cursor.getString(2);
            employee.faceData = cursor.getString(3);
        }
        cursor.close();
        db.close();
        return employee;
    }

    // Insert attendance
    public void insertAttendance(int employeeId, String timestamp, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT INTO attendance (employeeId, timestamp, type) VALUES (?, ?, ?)";
        db.execSQL(sql, new Object[]{employeeId, timestamp, type});
    }
}
