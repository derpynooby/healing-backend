# 🌿 Healing App — Full-Stack Architecture

Dua proyek terpisah yang bekerja bersama:

```
healing-backend/    ← Spring Boot (IntelliJ IDEA)
healing-android/    ← Android (Android Studio)
```

---

## 🏗️ Arsitektur Keseluruhan

```
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│  Fragment → RetrofitClient → Retrofit Service Interface │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP + JWT Bearer Token
                       │ REST API (JSON)
┌──────────────────────▼──────────────────────────────────┐
│               Spring Boot Backend                        │
│  Controller → Service → Repository → PostgreSQL         │
│                  ↓                                       │
│            GeminiService → Google Gemini API            │
└─────────────────────────────────────────────────────────┘
```

**Keuntungan arsitektur ini:**
- API key Gemini tersimpan aman di server — tidak pernah dikirim ke device
- Database tersentralisasi — bisa diakses dari multiple device
- Backend dan Android dikembangkan dan di-deploy secara independen
- Mudah ditambahkan platform lain (web, iOS) tanpa ubah backend

---

## 🚀 Setup Backend (IntelliJ IDEA)

### 1. Buka proyek
```
File → Open → pilih folder healing-backend
```
IntelliJ akan otomatis detect Maven project.

### 2. Install PostgreSQL
Download: https://www.postgresql.org/download/
```sql
-- Buat database setelah install:
CREATE DATABASE healing_db;
```

### 3. Konfigurasi application.properties
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/healing_db
spring.datasource.username=postgres
spring.datasource.password=PASSWORD_KAMU

healing.gemini.api-key=AIzaSy_XXXXXXXX   ← dari aistudio.google.com
healing.jwt.secret=ganti_dengan_string_acak_min_32_karakter
```

### 4. Run
```
Klik kanan HealingBackendApplication.java → Run
```
Backend berjalan di: **http://localhost:8080**

### 5. Test API (opsional)
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"123456"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}'
```

---

## 📱 Setup Android (Android Studio)

### 1. Buka proyek
```
File → Open → pilih folder healing-android
```

### 2. Tambahkan JitPack di settings.gradle
```gradle
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

### 3. Cek BASE_URL di app/build.gradle
```gradle
# Untuk Android Emulator (default sudah benar):
buildConfigField "String", "BASE_URL", "\"http://10.0.2.2:8080/\""

# Untuk device fisik di jaringan yang sama:
buildConfigField "String", "BASE_URL", "\"http://192.168.X.X:8080/\""
# (ganti dengan IP PC kamu: ipconfig / ifconfig)

# Untuk production:
buildConfigField "String", "BASE_URL", "\"https://api.healing.app/\""
```

### 4. Run
```
Pastikan backend sudah berjalan → Run 'app'
```

---

## 📋 API Endpoints

| Method | Endpoint | Deskripsi | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Daftar akun baru | ❌ |
| POST | `/api/auth/login` | Login, dapat JWT token | ❌ |
| GET | `/api/user/me` | Data profil user | ✅ |
| PUT | `/api/user/me` | Update profil | ✅ |
| POST | `/api/meals` | Log makanan | ✅ |
| GET | `/api/meals/daily?date=` | Ringkasan kalori harian | ✅ |
| DELETE | `/api/meals/{id}` | Hapus log makanan | ✅ |
| POST | `/api/workouts` | Tambah workout | ✅ |
| PUT | `/api/workouts/{id}/complete` | Selesaikan workout (+XP) | ✅ |
| GET | `/api/workouts/daily?date=` | Workout hari ini | ✅ |
| GET | `/api/workouts/weekly-stats` | Statistik mingguan | ✅ |
| POST | `/api/chat` | Chat dengan Hana AI | ✅ |
| GET | `/api/chat/history` | Riwayat chat | ✅ |
| DELETE | `/api/chat/history` | Hapus riwayat | ✅ |

Auth ✅ = kirim header: `Authorization: Bearer <token>`

---

## 🔑 Flow Autentikasi

```
1. Register/Login → backend return JWT token
2. Android simpan token di SessionManager (SharedPreferences)
3. RetrofitClient otomatis sisipkan token di setiap request
4. Backend validasi token di JwtAuthFilter
5. Logout → hapus token dari SharedPreferences
```

---

## 🛡️ Keamanan

| Aspek | Implementasi |
|---|---|
| Password | BCrypt hash di backend |
| API Key Gemini | Hanya ada di server, tidak dikirim ke Android |
| Auth | JWT Bearer token (exp: 24 jam) |
| CORS | Konfigurasi di SecurityConfig |
| Database | JPA/Hibernate prepared statements (SQL injection safe) |
| HTTPS | Wajib aktifkan di production |

---

## 🌐 Deploy ke Production (Ringkas)

**Backend:**
```bash
mvn clean package
java -jar target/healing-backend-1.0.0.jar
# Atau deploy ke Railway, Render, atau VPS
```

**Android:**
- Ganti BASE_URL ke HTTPS domain production
- Ubah `usesCleartextTraffic="false"` di AndroidManifest
- Build release APK: Build → Generate Signed APK

---

*Healing — Hidup sehat yang menyenangkan* 🌿
