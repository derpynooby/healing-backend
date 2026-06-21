package com.healing.backend.dto;

import lombok.*;

import java.util.List;

// ─────────────── SCHEDULE ───────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeeklyScheduleResponse {
    public String weekStart;
    public String weekEnd;
    public List<DailyScheduleResponse> days;
    public String aiRecommendation; // Rekomendasi mingguan dari AI
}
