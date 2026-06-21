package com.healing.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity untuk menyimpan hasil scan foto makanan via Gemini Vision AI.
 */
@Entity
@Table(name = "food_scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nama makanan yang terdeteksi AI
    @Column(nullable = false)
    private String detectedFoodName;

    // Confidence score dari AI (0.0 - 1.0)
    private Float confidenceScore;

    // Estimasi berat dalam gram
    private Integer estimatedWeightGram;

    // Estimasi porsi (misal "1 piring", "1/2 porsi")
    private String estimatedPortion;

    // Nutrisi
    private Integer totalCalories;
    private Float carbohydrates;    // gram
    private Float protein;          // gram
    private Float fat;              // gram
    private Float fiber;            // gram

    // Apakah sudah di-log ke meal_logs
    @Builder.Default
    private Boolean loggedToMeal = false;

    // Tipe meal yang dipilih user setelah scan
    private String mealType;

    private LocalDate date;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (date == null) date = LocalDate.now();
        if (loggedToMeal == null) loggedToMeal = false;
    }
}
