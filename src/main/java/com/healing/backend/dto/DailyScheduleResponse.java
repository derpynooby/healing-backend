package com.healing.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyScheduleResponse {
    public String date;
    public List<ScheduleResponse> slots;
    public Integer totalTargetCalories;
    public Integer totalTargetCaloriesBurn;
    public String aiSummary;      // Ringkasan jadwal hari ini dari AI
}
