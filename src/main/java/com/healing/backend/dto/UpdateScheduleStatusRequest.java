package com.healing.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleStatusRequest {
    // COMPLETED / SKIPPED
    @NotBlank
    public String status;
    // Alasan skip (opsional)
    public String reason;
    // Kalori yang dikonsumsi (untuk trigger rescheduling otomatis)
    public Integer caloriesConsumed;
}
