package com.healing.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private Integer age;
    private Float weight;
    private Float height;
    private String gender;
    private String activityLevel;
    private String goal;

    @Column(columnDefinition = "TEXT")
    private String favoriteFoods;   // comma-separated

    @Column(columnDefinition = "TEXT")
    private String hobbies;         // comma-separated

    @Builder.Default
    private Integer totalXp = 0;

    @Builder.Default
    private Integer currentLevel = 1;

    private Integer dailyCalorieTarget;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalXp == null) totalXp = 0;
        if (currentLevel == null) currentLevel = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Kalori kalkulasi ──────────────────────────────

    public float getBMR() {
        if (weight == null || height == null || age == null) return 2000f;
        if ("male".equalsIgnoreCase(gender)) {
            return 88.362f + (13.397f * weight) + (4.799f * height) - (5.677f * age);
        } else {
            return 447.593f + (9.247f * weight) + (3.098f * height) - (4.330f * age);
        }
    }

    public int getRecommendedCalories() {
        float bmr = getBMR();
        float tdee;
        switch (activityLevel != null ? activityLevel : "sedentary") {
            case "light":       tdee = bmr * 1.375f; break;
            case "moderate":    tdee = bmr * 1.55f;  break;
            case "active":      tdee = bmr * 1.725f; break;
            case "very_active": tdee = bmr * 1.9f;   break;
            default:            tdee = bmr * 1.2f;
        }
        switch (goal != null ? goal : "maintain") {
            case "lose": return Math.round(tdee - 500);
            case "gain": return Math.round(tdee + 300);
            default:     return Math.round(tdee);
        }
    }

    public String getLevelTitle() {
        if (currentLevel == null) return "Newbie";
        if (currentLevel >= 20) return "Health Legend";
        if (currentLevel >= 15) return "Wellness Master";
        if (currentLevel >= 10) return "Active Hero";
        if (currentLevel >= 5)  return "Healing Starter";
        return "Newbie";
    }
}
