package com.healing.backend.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkoutLogResponse {
    public Long id;
    public String workoutName;
    public String workoutType;
    public Integer durationMinutes;
    public Integer caloriesBurned;
    public Integer xpEarned;
    public Boolean isHobbyBased;
    public String hobbyTag;
    public Boolean completed;
    public LocalDate date;
    public LocalDateTime createdAt;
}
