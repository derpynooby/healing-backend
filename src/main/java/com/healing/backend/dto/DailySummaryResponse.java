package com.healing.backend.dto;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailySummaryResponse {
    private String date;
    private Integer totalCalories;
    private Integer targetCalories;
    private Integer remainingCalories;
    private Integer caloriePercent;          // Sync spec: 0-100
    private List<MealLogResponse> meals;
    private List<MealLogResponse> cheatMeals;
    private Integer adjustedCaloriesPerMeal; // Sync spec: saran kalori jika ada cheat treat
}
