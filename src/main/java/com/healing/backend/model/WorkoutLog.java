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
    private String workoutType;     // cardio / strength / hobby_walk / hobby_dance
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private Integer xpEarned;
    private Boolean isHobbyBased;
    private String hobbyTag;
    private Boolean completed;
    private LocalDate date;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (date == null) date = LocalDate.now();
        if (completed == null) completed = false;
        if (isHobbyBased == null) isHobbyBased = false;
        if (caloriesBurned == null) caloriesBurned = calculateCalories();
        if (xpEarned == null) xpEarned = calculateXp();
    }

    private int calculateCalories() {
        if (durationMinutes == null) return 0;
        switch (workoutType != null ? workoutType : "") {
            case "cardio":       return (int)(durationMinutes * 8.5f);
            case "strength":     return (int)(durationMinutes * 5.0f);
            case "hobby_walk":   return (int)(durationMinutes * 4.0f);
            case "hobby_dance":  return (int)(durationMinutes * 6.5f);
            default:             return (int)(durationMinutes * 4.5f);
        }
    }

    private int calculateXp() {
        if (durationMinutes == null) return 0;
        int base = durationMinutes * 2;
        return Boolean.TRUE.equals(isHobbyBased) ? (int)(base * 1.25f) : base;
    }
}
