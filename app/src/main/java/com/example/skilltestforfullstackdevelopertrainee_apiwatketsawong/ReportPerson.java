package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportPerson extends AppCompatActivity {
    private TextView titleText;
    private RecyclerView recordRecyclerView;
    private Employee emp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_person);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleText = findViewById(R.id.titleText);
        recordRecyclerView = findViewById(R.id.recordRecyclerView);
        recordRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        emp = new Employee(this);
        int employeeId = getIntent().getIntExtra("employeeId", -1);
        if (employeeId == -1) return;

        MainActivity.EmployeeModel employee = emp.getEmployeeById(employeeId);
        if (employee != null) {
            titleText.setText(employee.name + " Records");

            // ดึง attendance
            List<AttendanceRecord> records = emp.getAttendanceByEmployee(employeeId);

            AttendanceAdapter adapter = new AttendanceAdapter(records);
            recordRecyclerView.setAdapter(adapter);
        }
    }

    // Model สำหรับ attendance
    public static class AttendanceRecord {
        public String timestamp;
        public String type;
    }

    // Adapter
    public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

        private List<AttendanceRecord> records;

        public AttendanceAdapter(List<AttendanceRecord> records) {
            this.records = records;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.attendance_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.timestampText.setText(record.timestamp);
            holder.typeText.setText(record.type);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timestampText, typeText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                timestampText = itemView.findViewById(R.id.timestampText);
                typeText = itemView.findViewById(R.id.typeText);
                itemView.setBackgroundResource(R.drawable.record_item_border);
            }
        }
    }
}