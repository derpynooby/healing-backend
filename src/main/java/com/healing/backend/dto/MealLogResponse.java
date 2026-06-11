package com.healing.backend.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MealLogResponse {
    public Long id;
    public String foodName;
    public Integer calories;
    public Float protein;
    public Float carbs;
    public Float fat;
    public String mealType;
    public String mealTypeLabel;
    public Boolean isCheatTreat;
    public LocalDate date;
    public LocalDateTime createdAt;
}
