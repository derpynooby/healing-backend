package com.healing.backend.dto;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkoutLogResponse {
    private Long id;
    private String workoutName;
    private String workoutType;
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private Integer xpEarned;    // Sync spec: XP yang didapat dari sesi ini
    private Boolean isHobbyBased;
    private String hobbyTag;
    private Boolean completed;   // Sync spec: Boolean
    private LocalDate date;
    private LocalDateTime createdAt;
}
