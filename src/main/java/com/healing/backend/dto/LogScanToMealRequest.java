package com.healing.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
class LogScanToMealRequest {
    // mealType: breakfast / snack / lunch / dinner
    public String mealType;
    public LocalDate date;
}
