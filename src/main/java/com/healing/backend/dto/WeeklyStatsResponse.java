package com.healing.backend.dto;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeeklyStatsResponse {
    private Integer totalXp;
    private Integer completedWorkouts;
    private Integer totalMinutes;
    private Integer totalCaloriesBurned;
    private String weekStart;
    private String weekEnd;
}
