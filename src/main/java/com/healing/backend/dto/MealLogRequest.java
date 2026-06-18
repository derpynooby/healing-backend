package com.healing.backend.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class MealLogRequest {
    @NotBlank public String foodName;
    @NotNull public Integer calories;
    public Float protein;
    public Float carbs;
    public Float fat;
    public String mealType;
    public LocalDate date;
}
