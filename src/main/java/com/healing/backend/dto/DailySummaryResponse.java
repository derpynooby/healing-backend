package com.healing.backend.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailySummaryResponse {
    public String date;
    public Integer totalCalories;
    public Integer targetCalories;
    public Integer remainingCalories;
    public Integer caloriePercent;
    public List<MealLogResponse> meals;
    public List<MealLogResponse> cheatMeals;
    public Integer adjustedCaloriesPerMeal;
}
