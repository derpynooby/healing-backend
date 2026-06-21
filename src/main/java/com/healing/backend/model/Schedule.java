package com.healing.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity untuk menyimpan jadwal harian yang dibuat AI.
 * Setiap entry adalah satu slot jadwal (makan/olahraga/tidur).
 */
@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Tanggal jadwal
    private LocalDate date;

    // Tipe slot: MEAL / WORKOUT / SLEEP
    private String slotType;

    // Label: "Sarapan", "Cardio 30 mnt", "Tidur Malam"
    private String title;

    // Deskripsi detail dari AI
    @Column(columnDefinition = "TEXT")
    private String description;

    // Waktu mulai: "07:00"
    private String startTime;

    // Waktu selesai: "07:30"
    private String endTime;

    // Durasi dalam menit
    private Integer durationMinutes;

    // Target kalori/XP untuk slot ini
    private Integer targetCalories;

    // Status: PENDING / COMPLETED / SKIPPED / RESCHEDULED
    @Builder.Default
    private String status = "PENDING";

    // Alasan rescheduling (jika ada)
    private String rescheduleReason;

    // Apakah jadwal ini hasil adaptive rescheduling
    @Builder.Default
    private Boolean isRescheduled = false;

    // Kalori ekstra yang perlu dibakar (dari Cheat & Treat)
    @Builder.Default
    private Integer extraCaloriesToBurn = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (isRescheduled == null) isRescheduled = false;
        if (extraCaloriesToBurn == null) extraCaloriesToBurn = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
