package com.healing.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String workoutName;
    private String workoutType;
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private Integer xpEarned;

    @Builder.Default
    private Boolean isHobbyBased = false;

    private String hobbyTag;

    @Builder.Default
    private Boolean completed = false;

    private LocalDate date;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (date == null) date = LocalDate.now();
        if (completed == null) completed = false;
        if (isHobbyBased == null) isHobbyBased = false;
        // Auto-calculate calories and XP
        if (caloriesBurned == null) caloriesBurned = calcCalories();
        if (xpEarned == null) xpEarned = calcXp();
    }

    private int calcCalories() {
        if (durationMinutes == null) return 0;
        switch (workoutType != null ? workoutType : "") {
            case "cardio":      return Math.round(durationMinutes * 8.5f);
            case "strength":    return Math.round(durationMinutes * 5.0f);
            case "hobby_walk":  return Math.round(durationMinutes * 4.0f);
            case "hobby_dance": return Math.round(durationMinutes * 6.5f);
            default:            return Math.round(durationMinutes * 4.5f);
        }
    }

    private int calcXp() {
        if (durationMinutes == null) return 0;
        int base = durationMinutes * 2;
        return Boolean.TRUE.equals(isHobbyBased) ? Math.round(base * 1.25f) : base;
    }
}
