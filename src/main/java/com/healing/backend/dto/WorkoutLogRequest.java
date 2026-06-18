package com.healing.backend.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class WorkoutLogRequest {
    @NotBlank public String workoutName;
    @NotBlank public String workoutType;
    @NotNull public Integer durationMinutes;
    public Boolean isHobbyBased;
    public String hobbyTag;
    public LocalDate date;
}
