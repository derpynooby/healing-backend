package com.healing.backend.service;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutLogRepository workoutLogRepository;
    private final UserService userService;

    @Transactional
    public WorkoutLog addWorkout(User user, WorkoutLogRequest req) {
        WorkoutLog w = WorkoutLog.builder()
                .user(user)
                .workoutName(req.getWorkoutName())
                .workoutType(req.getWorkoutType())
                .durationMinutes(req.getDurationMinutes())
                .isHobbyBased(Boolean.TRUE.equals(req.getIsHobbyBased()))
                .hobbyTag(req.getHobbyTag())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .completed(false)
                .build();
        return workoutLogRepository.save(w);
    }

    @Transactional
    public WorkoutLog completeWorkout(Long workoutId, Long userId) {
        WorkoutLog w = workoutLogRepository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout tidak ditemukan"));
        if (!w.getUser().getId().equals(userId))
            throw new RuntimeException("Akses ditolak");
        w.setCompleted(true);
        workoutLogRepository.save(w);
        userService.addXp(userId, w.getXpEarned());
        return w;
    }

    public List<WorkoutLog> getWorkoutsForDate(Long userId, LocalDate date) {
        return workoutLogRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, date);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int xp    = workoutLogRepository.getXpInRange(userId, weekStart, today);
        int count = workoutLogRepository.getCompletedCountInRange(userId, weekStart, today);
        return WeeklyStatsResponse.builder()
                .totalXp(xp)
                .completedWorkouts(count)
                .weekStart(weekStart.toString())
                .weekEnd(today.toString())
                .build();
    }

    public WorkoutLogResponse toResponse(WorkoutLog w) {
        return WorkoutLogResponse.builder()
                .id(w.getId())
                .workoutName(w.getWorkoutName())
                .workoutType(w.getWorkoutType())
                .durationMinutes(w.getDurationMinutes())
                .caloriesBurned(w.getCaloriesBurned())
                .xpEarned(w.getXpEarned())
                .isHobbyBased(w.getIsHobbyBased())
                .hobbyTag(w.getHobbyTag())
                .completed(w.getCompleted())
                .date(w.getDate())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
