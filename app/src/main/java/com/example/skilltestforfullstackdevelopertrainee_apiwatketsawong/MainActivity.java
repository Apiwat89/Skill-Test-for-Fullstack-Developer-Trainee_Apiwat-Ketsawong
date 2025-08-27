package com.example.skilltestforfullstackdevelopertrainee_apiwatketsawong;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnAddEmployee, btnReportEmployee;
    private RecyclerView recyclerView;
    private Employee emp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        btnAddEmployee.setOnClickListener(this);
        btnReportEmployee = findViewById(R.id.btnReportEmployee);
        btnReportEmployee.setOnClickListener(this);

        recyclerView = findViewById(R.id.employeeListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emp = new Employee(this);
        List<EmployeeModel> employeeList = loadEmployees();

        EmployeeAdapter adapter = new EmployeeAdapter(this, employeeList, employee -> {
            // เมื่อคลิกชื่อพนักงาน ส่งไปหน้า ScanFaceActivity
            Intent intent = new Intent(MainActivity.this, ScanFace.class);
            intent.putExtra("employeeId", employee.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnAddEmployee) {
            startActivity(new Intent(this, Register.class));
        } else if (view.getId() == R.id.btnReportEmployee) {
            startActivity(new Intent(this, Report.class));
        }
    }

    private List<EmployeeModel> loadEmployees() {
        return emp.getAllEmployees();
    }

    public static class EmployeeModel {
        public int id;
        public String name;
        public String position;
        public String faceData;
    }

    public interface OnItemClickListener {
        void onItemClick(EmployeeModel employee);
    }

    public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
        private Context context;
        private List<EmployeeModel> employeeList;
        private OnItemClickListener listener;

        public EmployeeAdapter(Context context, List<EmployeeModel> employeeList, OnItemClickListener listener) {
            this.context = context;
            this.employeeList = employeeList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.employee_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EmployeeModel emp = employeeList.get(position);
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