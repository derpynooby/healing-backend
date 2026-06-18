package com.healing.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String foodName;

    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;

    // breakfast / snack / lunch / dinner
    private String mealType;

    @Builder.Default
    private Boolean isCheatTreat = false;

    private LocalDate date;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (date == null) date = LocalDate.now();
        if (isCheatTreat == null) isCheatTreat = false;
    }

    public String getMealTypeLabel() {
        if (mealType == null) return "Lainnya";
        switch (mealType) {
            case "breakfast": return "Sarapan";
            case "snack":     return "Snack";
            case "lunch":     return "Makan Siang";
            case "dinner":    return "Makan Malam";
            default:          return "Lainnya";
        }
    }
}
