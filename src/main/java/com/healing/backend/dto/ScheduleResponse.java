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
public class ScheduleResponse {
    public Long id;
    public String slotType;       // MEAL / WORKOUT / SLEEP
    public String title;
    public String description;
    public String startTime;
    public String endTime;
    public Integer durationMinutes;
    public Integer targetCalories;
    public String status;         // PENDING / COMPLETED / SKIPPED / RESCHEDULED
    public Boolean isRescheduled;
    public String rescheduleReason;
    public Integer extraCaloriesToBurn;
    public LocalDate date;
    public LocalDateTime createdAt;
}
