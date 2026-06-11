package com.healing.backend.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeeklyStatsResponse {
    public Integer totalXp;
    public Integer completedWorkouts;
    public Integer totalMinutes;
    public Integer totalCaloriesBurned;
    public String weekStart;
    public String weekEnd;
}
