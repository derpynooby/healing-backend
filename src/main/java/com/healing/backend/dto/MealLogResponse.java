package com.healing.backend.dto;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MealLogResponse {
    private Long id;
    private String foodName;
    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;
    private String mealType;
    private String mealTypeLabel;    // Sync spec: "Sarapan","Snack","Makan Siang","Makan Malam"
    private Boolean isCheatTreat;    // Sync spec: Boolean (bukan boolean)
    private LocalDate date;          // Format: yyyy-MM-dd
    private LocalDateTime createdAt; // Format: yyyy-MM-dd'T'HH:mm:ss
}
