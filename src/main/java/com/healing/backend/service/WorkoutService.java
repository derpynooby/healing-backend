package com.healing.backend.service;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.repository.WorkoutLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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

    /**
     * Sync spec: PUT /api/workouts/{id}/complete
     * - set completed = true
     * - hitung XP dan tambahkan ke totalXp user
     * - cek apakah user naik level (Level Up)
     */
    @Transactional
    public WorkoutResult completeWorkout(Long workoutId, Long userId) {
        WorkoutLog w = workoutLogRepository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout tidak ditemukan"));
        if (!w.getUser().getId().equals(userId))
            throw new RuntimeException("Akses ditolak");
        if (Boolean.TRUE.equals(w.getCompleted()))
            throw new RuntimeException("Workout sudah selesai sebelumnya");

        w.setCompleted(true);
        workoutLogRepository.save(w);

        // Sync spec: tambah XP + cek level up
        boolean levelUp = userService.addXpAndCheckLevelUp(userId, w.getXpEarned());
        return new WorkoutResult(w, levelUp);
    }

    public List<WorkoutLog> getWorkoutsForDate(Long userId, LocalDate date) {
        return workoutLogRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, date);
    }

    public WeeklyStatsResponse getWeeklyStats(Long userId) {
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int xp       = workoutLogRepository.getXpInRange(userId, weekStart, today);
        int count    = workoutLogRepository.getCompletedCountInRange(userId, weekStart, today);
        int minutes  = workoutLogRepository.getTotalMinutesInRange(userId, weekStart, today);
        int caloBurn = workoutLogRepository.getTotalCaloriesBurnedInRange(userId, weekStart, today);

        return WeeklyStatsResponse.builder()
                .totalXp(xp)
                .completedWorkouts(count)
                .totalMinutes(minutes)
                .totalCaloriesBurned(caloBurn)
                .weekStart(weekStart.toString())
                .weekEnd(today.toString())
                .build();
    }

    /**
     * Map WorkoutLog → WorkoutLogResponse
     * Sync spec: wajib include xpEarned dan completed sebagai Boolean
     */
    public WorkoutLogResponse toResponse(WorkoutLog w) {
        return WorkoutLogResponse.builder()
                .id(w.getId())
                .workoutName(w.getWorkoutName())
                .workoutType(w.getWorkoutType())
                .durationMinutes(w.getDurationMinutes())
                .caloriesBurned(w.getCaloriesBurned())
                .xpEarned(w.getXpEarned())       // Sync spec: wajib ada
                .isHobbyBased(w.getIsHobbyBased())
                .hobbyTag(w.getHobbyTag())
                .completed(w.getCompleted())       // Sync spec: Boolean
                .date(w.getDate())
                .createdAt(w.getCreatedAt())
                .build();
    }

    // Inner class untuk membawa result + levelUp flag
    public static class WorkoutResult {
        public final WorkoutLog workout;
        public final boolean levelUp;
        public WorkoutResult(WorkoutLog workout, boolean levelUp) {
            this.workout = workout;
            this.levelUp = levelUp;
        }
    }
}
