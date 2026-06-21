package com.healing.backend.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────── SCHEDULE ───────────────

/**
 * Request generate jadwal mingguan dari AI.
 * Input sesuai spec: usia, BB, TB, target, jam kerja, aktivitas.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class GenerateScheduleRequest {
    // Tanggal mulai jadwal
    public LocalDate startDate;
    // Berapa hari ke depan (default 7)
    @Builder.Default
    public Integer days = 7;
    // Jam kerja/kuliah (misal: "08:00-17:00")
    public String workHours;
    // Preferensi waktu olahraga: "morning" / "afternoon" / "evening"
    public String preferredWorkoutTime;
    // Jam tidur ideal (misal: "22:00")
    public String preferredSleepTime;
    // Catatan tambahan untuk AI
    public String notes;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScheduleResponse {
    public Long id;
    public String slotType;       // MEAL / WORKOUT / SLEEP
    public String title;
    public String description;
    public String startTime;
    public String endTime;
    public Integer durationMinutes;
    public Integer targetCalories;
    public String status;         // PENDING / COMPLETED / SKIPPED / RESCHEDULED
    public Boolean isRescheduled;
    public String rescheduleReason;
    public Integer extraCaloriesToBurn;
    public LocalDate date;
    public LocalDateTime createdAt;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateScheduleStatusRequest {
    // COMPLETED / SKIPPED
    @NotBlank
    public String status;
    // Alasan skip (opsional)
    public String reason;
    // Kalori yang dikonsumsi (untuk trigger rescheduling otomatis)
    public Integer caloriesConsumed;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyScheduleResponse {
    public String date;
    public List<ScheduleResponse> slots;
    public Integer totalTargetCalories;
    public Integer totalTargetCaloriesBurn;
    public String aiSummary;      // Ringkasan jadwal hari ini dari AI
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeeklyScheduleResponse {
    public String weekStart;
    public String weekEnd;
    public List<DailyScheduleResponse> days;
    public String aiRecommendation; // Rekomendasi mingguan dari AI
}
