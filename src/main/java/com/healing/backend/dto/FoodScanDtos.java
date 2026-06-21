package com.healing.backend.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ─────────────── FOOD SCAN ───────────────

/**
 * Request scan foto makanan.
 * imageBase64: gambar dalam format base64 string.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class FoodScanRequest {
    // Gambar dalam format base64
    public String imageBase64;
    // MIME type: "image/jpeg" atau "image/png"
    public String mimeType;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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

@Data @NoArgsConstructor @AllArgsConstructor
class LogScanToMealRequest {
    // mealType: breakfast / snack / lunch / dinner
    public String mealType;
    public LocalDate date;
}
