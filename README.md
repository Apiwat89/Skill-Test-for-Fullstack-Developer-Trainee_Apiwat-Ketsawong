📌 Face Recognition Attendance App
แอป Android สำหรับ บันทึกเวลาเข้า–ออกงาน ด้วยการ
- ตรวจจับใบหน้าด้วย CameraX + ML Kit
- แปลงเป็น Face Embedding ด้วย TensorFlow Lite (TFLite)
- ตรวจสอบความเหมือนกับฐานข้อมูลพนักงาน
- เช็ค ตำแหน่ง GPS ก่อนบันทึกเวลา

🚀 Features
- Face Register → ลงทะเบียนพนักงานพร้อมข้อมูลใบหน้า
- Face Scan → สแกนเพื่อตรวจสอบการเข้างาน
- ใช้ TFLite model แปลงรูปหน้าเป็น embedding (vector)
- เช็ค Location ก่อนบันทึกเวลา
- เก็บข้อมูลใน SQLite (BLOB สำหรับ embeddings)
- ใช้ CameraX + ML Kit Face Detection
