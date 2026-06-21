package com.healing.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request generate jadwal mingguan dari AI.
 * Input sesuai spec: usia, BB, TB, target, jam kerja, aktivitas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
