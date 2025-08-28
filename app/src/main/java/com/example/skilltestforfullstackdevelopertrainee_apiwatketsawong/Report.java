package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.content.Intent;
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

public class Report extends AppCompatActivity {
    private RecyclerView recyclerView;
    private Employee emp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.employeeListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emp = new Employee(this);
        List<Report.EmployeeModel> employeeList = loadEmployees();

        Report.EmployeeAdapter adapter = new Report.EmployeeAdapter(this, employeeList, employee -> {
            // เมื่อคลิกชื่อพนักงาน ส่งไปหน้า ScanFaceActivity
            Intent intent = new Intent(Report.this, ReportPerson.class);
            intent.putExtra("employeeId", employee.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private List<Report.EmployeeModel> loadEmployees() {
        return emp.getAllEmployeesReport();
    }

    public static class EmployeeModel {
        public int id;
        public String name;
        public String position;
        public byte[] faceEmbedding;
    }

    public interface OnItemClickListener {
        void onItemClick(Report.EmployeeModel employee);
    }

    public class EmployeeAdapter extends RecyclerView.Adapter<Report.EmployeeAdapter.ViewHolder> {
        private Context context;
        private List<Report.EmployeeModel> employeeList;
        private Report.OnItemClickListener listener;

        public EmployeeAdapter(Context context, List<Report.EmployeeModel> employeeList, Report.OnItemClickListener listener) {
            this.context = context;
            this.employeeList = employeeList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Report.EmployeeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.employee_item, parent, false);
            return new Report.EmployeeAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Report.EmployeeAdapter.ViewHolder holder, int position) {
            Report.EmployeeModel emp = employeeList.get(position);
            holder.nameText.setText(emp.name);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(emp));
        }

        @Override
        public int getItemCount() {
            return employeeList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.employeeName);
            }
        }
    }
}