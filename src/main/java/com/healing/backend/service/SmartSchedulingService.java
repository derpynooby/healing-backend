package com.healing.backend.service;

import com.google.gson.*;
import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.repository.*;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service untuk Smart Scheduling sesuai spec Prompt 2.
 *
 * Fitur:
 * 1. Generate jadwal mingguan dari data profil user via Gemini AI
 * 2. Adaptive rescheduling — otomatis sesuaikan jadwal jika:
 *    - User skip olahraga
 *    - User makan melebihi target kalori (dari Calorie Tracker foto)
 */
@Service
@RequiredArgsConstructor
public class SmartSchedulingService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Value("${healing.gemini.api-key}")
    private String apiKey;

    @Value("${healing.gemini.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    // ──────────────────────────────────────────────────────────────
    // 1. GENERATE JADWAL MINGGUAN
    // ──────────────────────────────────────────────────────────────

    /**
     * Generate jadwal mingguan otomatis via Gemini AI.
     * Input: profil user + preferensi jadwal.
     * Output: list Schedule entities yang disimpan ke DB.
     */
    @Transactional
    public List<Schedule> generateWeeklySchedule(User user, GenerateScheduleRequest req)
            throws IOException {

        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        int days = req.getDays() != null ? req.getDays() : 7;
        LocalDate end = start.plusDays(days - 1);

        // Hapus jadwal lama untuk rentang yang sama
        scheduleRepository.deleteByUserIdAndDateBetween(user.getId(), start, end);

        // Build prompt untuk Gemini
        String systemPrompt = buildSchedulingSystemPrompt(user, req);
        String userPrompt   = buildSchedulingUserPrompt(user, req, start, days);

        // Call Gemini
        String aiResponse = callGemini(systemPrompt, userPrompt);

        // Parse response → Schedule entities
        List<Schedule> schedules = parseScheduleResponse(user, aiResponse, start);

        // Simpan ke database
        return scheduleRepository.saveAll(schedules);
    }

    // ──────────────────────────────────────────────────────────────
    // 2. ADAPTIVE RESCHEDULING
    // ──────────────────────────────────────────────────────────────

    /**
     * Adaptive rescheduling saat user skip olahraga atau over-eat.
     *
     * Sesuai spec Prompt 2:
     * "Jika pengguna melewatkan satu sesi olahraga atau makan berlebih,
     *  AI harus bisa melakukan rescheduling secara adaptif."
     *
     * Sesuai spec Prompt 3 (integrasi):
     * "Jika AI mendeteksi pengguna makan melebihi target kalori lewat foto,
     *  AI secara otomatis menyesuaikan jadwal olahraga sore hari."
     */
    @Transactional
    public List<Schedule> adaptiveReschedule(User user, Long scheduleId,
                                              UpdateScheduleStatusRequest req)
            throws IOException {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Jadwal tidak ditemukan"));
        if (!schedule.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Akses ditolak");

        schedule.setStatus(req.getStatus());
        if (req.getReason() != null) schedule.setRescheduleReason(req.getReason());
        scheduleRepository.save(schedule);

        List<Schedule> rescheduled = new ArrayList<>();

        // Kasus 1: User skip workout → cari slot kosong untuk reschedule
        if ("SKIPPED".equals(req.getStatus()) && "WORKOUT".equals(schedule.getSlotType())) {
            rescheduled = rescheduleSkippedWorkout(user, schedule);
        }

        // Kasus 2: User over-eat (dari foto scan) → tambah workout ekstra
        if (req.getCaloriesConsumed() != null) {
            int targetCal = user.getRecommendedCalories();
            int consumed  = req.getCaloriesConsumed();
            if (consumed > targetCal) {
                int extraCal = consumed - targetCal;
                rescheduled.addAll(addExtraWorkoutForCalories(user, schedule.getDate(), extraCal));
            }
        }

        return rescheduled;
    }

    private List<Schedule> rescheduleSkippedWorkout(User user, Schedule skipped) {
        // Cari slot di hari berikutnya yang kosong
        LocalDate tomorrow = skipped.getDate().plusDays(1);
        List<Schedule> existingTomorrow = scheduleRepository
                .findByUserIdAndDateOrderByStartTimeAsc(user.getId(), tomorrow);

        // Cek apakah sudah ada workout besok
        boolean hasWorkoutTomorrow = existingTomorrow.stream()
                .anyMatch(s -> "WORKOUT".equals(s.getSlotType()));

        if (!hasWorkoutTomorrow) {
            Schedule makeup = Schedule.builder()
                    .user(user)
                    .date(tomorrow)
                    .slotType("WORKOUT")
                    .title("Make-up: " + skipped.getTitle())
                    .description("Sesi makeup dari jadwal yang di-skip kemarin. " +
                                 skipped.getDescription())
                    .startTime("07:00")
                    .endTime(addMinutes("07:00", skipped.getDurationMinutes()))
                    .durationMinutes(skipped.getDurationMinutes())
                    .targetCalories(skipped.getTargetCalories())
                    .status("PENDING")
                    .isRescheduled(true)
                    .rescheduleReason("Makeup dari jadwal yang di-skip tanggal " + skipped.getDate())
                    .build();
            return List.of(scheduleRepository.save(makeup));
        }
        return List.of();
    }

    /**
     * Sesuai spec Prompt 3 — integrasi foto kalori dengan jadwal:
     * Tambah workout ekstra untuk membakar kalori berlebih.
     */
    public List<Schedule> addExtraWorkoutForCalories(User user, LocalDate date, int extraCalories) {
        // Estimasi durasi: ~8.5 kcal/menit cardio
        int extraMinutes = Math.max(15, (int)(extraCalories / 8.5));

        // Cari slot sore hari yang kosong
        String startTime = "17:00";
        String endTime   = addMinutes(startTime, extraMinutes);

        Schedule extra = Schedule.builder()
                .user(user)
                .date(date)
                .slotType("WORKOUT")
                .title("Cardio Ekstra 🔥")
                .description("Sesi cardio tambahan untuk membakar " + extraCalories +
                             " kcal ekstra dari konsumsi makanan melebihi target hari ini.")
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(extraMinutes)
                .targetCalories(extraCalories)
                .status("PENDING")
                .isRescheduled(true)
                .extraCaloriesToBurn(extraCalories)
                .rescheduleReason("Otomatis ditambah karena konsumsi kalori melebihi target +" +
                                  extraCalories + " kcal")
                .build();
        return List.of(scheduleRepository.save(extra));
    }

    // ──────────────────────────────────────────────────────────────
    // QUERY METHODS
    // ──────────────────────────────────────────────────────────────

    public List<Schedule> getDailySchedule(Long userId, LocalDate date) {
        return scheduleRepository.findByUserIdAndDateOrderByStartTimeAsc(userId, date);
    }

    public List<Schedule> getWeeklySchedule(Long userId, LocalDate start, LocalDate end) {
        return scheduleRepository
                .findByUserIdAndDateBetweenOrderByDateAscStartTimeAsc(userId, start, end);
    }

    // ──────────────────────────────────────────────────────────────
    // GEMINI AI CALLS
    // ──────────────────────────────────────────────────────────────

    /**
     * System prompt sesuai spec Prompt 2:
     * "Tolong buatkan sistem prompt (System Prompt/Role) untuk AI"
     */
    private String buildSchedulingSystemPrompt(User user, GenerateScheduleRequest req) {
        return "Kamu adalah AI Health Scheduler yang ahli dalam membuat jadwal kesehatan personal. " +
               "Tugasmu adalah membuat jadwal harian yang mencakup waktu makan, olahraga, dan tidur " +
               "berdasarkan data pengguna. " +
               "Jadwal harus realistis, fleksibel, dan mempertimbangkan jam kerja/kuliah pengguna. " +
               "Selalu respond HANYA dengan JSON array yang valid, tanpa teks lain di luar JSON. " +
               "Batasan keamanan: jangan buat jadwal olahraga lebih dari 2 jam/hari, " +
               "jangan kurangi kalori di bawah 1200 kcal/hari, " +
               "selalu sertakan waktu istirahat yang cukup.";
    }

    /**
     * User prompt dengan data profil sesuai spec Prompt 2.
     */
    private String buildSchedulingUserPrompt(User user, GenerateScheduleRequest req,
                                              LocalDate start, int days) {
        int targetCalories = user.getRecommendedCalories();

        StringBuilder sb = new StringBuilder();
        sb.append("Buat jadwal kesehatan untuk ").append(days).append(" hari mulai ")
          .append(start).append(".\n\n");
        sb.append("DATA PENGGUNA:\n");
        sb.append("- Nama: ").append(user.getName()).append("\n");
        sb.append("- Usia: ").append(user.getAge()).append(" tahun\n");
        sb.append("- Berat: ").append(user.getWeight()).append(" kg\n");
        sb.append("- Tinggi: ").append(user.getHeight()).append(" cm\n");
        sb.append("- Target: ").append(user.getGoal()).append("\n");
        sb.append("- Tingkat aktivitas: ").append(user.getActivityLevel()).append("\n");
        sb.append("- Target kalori harian: ").append(targetCalories).append(" kcal\n");
        if (user.getHobbies() != null)
            sb.append("- Hobi (untuk variasi olahraga): ").append(user.getHobbies()).append("\n");
        if (req.getWorkHours() != null)
            sb.append("- Jam kerja/kuliah: ").append(req.getWorkHours()).append("\n");
        if (req.getPreferredWorkoutTime() != null)
            sb.append("- Preferensi waktu olahraga: ").append(req.getPreferredWorkoutTime()).append("\n");
        if (req.getPreferredSleepTime() != null)
            sb.append("- Jam tidur: ").append(req.getPreferredSleepTime()).append("\n");
        if (req.getNotes() != null)
            sb.append("- Catatan: ").append(req.getNotes()).append("\n");

        sb.append("""
            
            Buat jadwal dalam format JSON array berikut:
            [
              {
                "date": "YYYY-MM-DD",
                "slotType": "MEAL atau WORKOUT atau SLEEP",
                "title": "judul singkat",
                "description": "deskripsi detail dengan tips",
                "startTime": "HH:MM",
                "endTime": "HH:MM",
                "durationMinutes": angka,
                "targetCalories": angka (untuk MEAL=target konsumsi, WORKOUT=target bakar)
              }
            ]
            
            Untuk setiap hari buat minimal:
            - 3 slot MEAL (Sarapan, Makan Siang, Makan Malam)
            - 1-2 slot WORKOUT (sesuai preferensi)
            - 1 slot SLEEP
            
            Total kalori MEAL per hari harus mendekati target kalori pengguna.
            HANYA output JSON array, tidak ada teks lain.
            """);

        return sb.toString();
    }

    private String callGemini(String systemPrompt, String userPrompt) throws IOException {
        JsonObject body = new JsonObject();

        JsonObject sys = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text", systemPrompt);
        sysParts.add(sysText);
        sys.add("parts", sysParts);
        body.add("system_instruction", sys);

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", userPrompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.4);
        genConfig.addProperty("maxOutputTokens", 8192);
        genConfig.addProperty("responseMimeType", "application/json");
        body.add("generationConfig", genConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful())
                throw new IOException("Gemini API error " + response.code() + ": " + respBody);

            JsonObject root = JsonParser.parseString(respBody).getAsJsonObject();
            return root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }
    }

    private List<Schedule> parseScheduleResponse(User user, String jsonText, LocalDate start) {
        jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();
        List<Schedule> schedules = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(jsonText).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject item = el.getAsJsonObject();
                try {
                    LocalDate date = LocalDate.parse(
                        item.get("date").getAsString());
                    Schedule s = Schedule.builder()
                            .user(user)
                            .date(date)
                            .slotType(getStr(item, "slotType", "MEAL"))
                            .title(getStr(item, "title", ""))
                            .description(getStr(item, "description", ""))
                            .startTime(getStr(item, "startTime", "08:00"))
                            .endTime(getStr(item, "endTime", "09:00"))
                            .durationMinutes(getInt(item, "durationMinutes", 30))
                            .targetCalories(getInt(item, "targetCalories", 0))
                            .status("PENDING")
                            .isRescheduled(false)
                            .build();
                    schedules.add(s);
                } catch (Exception e) { /* skip invalid item */ }
            }
        } catch (Exception e) {
            // Fallback: buat jadwal default jika parse gagal
            schedules = buildDefaultSchedule(user, start, 7);
        }
        return schedules;
    }

    /**
     * Jadwal default jika AI gagal respond.
     */
    private List<Schedule> buildDefaultSchedule(User user, LocalDate start, int days) {
        List<Schedule> list = new ArrayList<>();
        int targetCal = user.getRecommendedCalories();
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            list.add(Schedule.builder().user(user).date(date).slotType("MEAL")
                .title("Sarapan").description("Makanan bergizi seimbang")
                .startTime("07:00").endTime("07:30").durationMinutes(30)
                .targetCalories((int)(targetCal * 0.25)).status("PENDING").build());
            list.add(Schedule.builder().user(user).date(date).slotType("WORKOUT")
                .title("Cardio Pagi").description("Jalan cepat atau jogging ringan")
                .startTime("08:00").endTime("08:30").durationMinutes(30)
                .targetCalories(250).status("PENDING").build());
            list.add(Schedule.builder().user(user).date(date).slotType("MEAL")
                .title("Makan Siang").description("Makanan berprotein tinggi")
                .startTime("12:00").endTime("12:30").durationMinutes(30)
                .targetCalories((int)(targetCal * 0.35)).status("PENDING").build());
            list.add(Schedule.builder().user(user).date(date).slotType("MEAL")
                .title("Makan Malam").description("Makanan ringan dan bergizi")
                .startTime("18:30").endTime("19:00").durationMinutes(30)
                .targetCalories((int)(targetCal * 0.30)).status("PENDING").build());
            list.add(Schedule.builder().user(user).date(date).slotType("SLEEP")
                .title("Tidur Malam").description("Tidur 7-8 jam untuk pemulihan optimal")
                .startTime("22:00").endTime("06:00").durationMinutes(480)
                .targetCalories(0).status("PENDING").build());
        }
        return list;
    }

    public ScheduleResponse toResponse(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId()).slotType(s.getSlotType()).title(s.getTitle())
                .description(s.getDescription()).startTime(s.getStartTime())
                .endTime(s.getEndTime()).durationMinutes(s.getDurationMinutes())
                .targetCalories(s.getTargetCalories()).status(s.getStatus())
                .isRescheduled(s.getIsRescheduled()).rescheduleReason(s.getRescheduleReason())
                .extraCaloriesToBurn(s.getExtraCaloriesToBurn())
                .date(s.getDate()).createdAt(s.getCreatedAt()).build();
    }

    private String addMinutes(String time, Integer minutes) {
        if (time == null || minutes == null) return "08:00";
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]) + minutes;
            h += m / 60; m = m % 60;
            return String.format("%02d:%02d", h % 24, m);
        } catch (Exception e) { return "09:00"; }
    }

    private String getStr(JsonObject o, String k, String def) {
        try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def; }
        catch (Exception e) { return def; }
    }

    private Integer getInt(JsonObject o, String k, Integer def) {
        try { return o.has(k) ? o.get(k).getAsInt() : def; }
        catch (Exception e) { return def; }
    }
}
