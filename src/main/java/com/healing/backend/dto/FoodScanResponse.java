package com.healing.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FoodScanResponse {
    public Long id;
    // Nama makanan yang terdeteksi
    public String detectedFoodName;
    // Confidence score 0.0-1.0
    public Float confidenceScore;
    // Estimasi berat
    public Integer estimatedWeightGram;
    public String estimatedPortion;
    // Nutrisi
    public Integer totalCalories;
    public Float carbohydrates;
    public Float protein;
    public Float fat;
    public Float fiber;
    // Status
    public Boolean loggedToMeal;
    public String mealType;
    public LocalDate date;
    public LocalDateTime createdAt;
    // Disclaimer wajib sesuai spec
    public String disclaimer;
}
