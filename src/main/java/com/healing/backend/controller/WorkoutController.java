package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sync spec endpoints:
 * POST /api/workouts                           — tambah workout
 * PUT  /api/workouts/{id}/complete             — selesaikan workout + trigger XP
 * GET  /api/workouts/daily?date=yyyy-MM-dd     — daftar workout harian
 * GET  /api/workouts/weekly-stats              — statistik mingguan
 */
@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final UserService userService;

    /**
     * POST /api/workouts
     * Body: WorkoutLogRequest
     * Response: ApiResponse<WorkoutLogResponse>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutLogResponse>> addWorkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WorkoutLogRequest req) {
        User user    = userService.getUserByEmail(userDetails.getUsername());
        WorkoutLog w = workoutService.addWorkout(user, req);
        return ResponseEntity.ok(ApiResponse.ok(
            "Aktivitas berhasil ditambahkan!",
            workoutService.toResponse(w)
        ));
    }

    /**
     * PUT /api/workouts/{id}/complete
     * Response: ApiResponse<WorkoutLogResponse>
     *
     * Sync spec — XP Flow:
     * - hitung XP dari workout
     * - tambahkan ke totalXp user
     * - cek apakah user naik level (Level Up)
     * - message berisi XP yang didapat + notif level up jika ada
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkoutLogResponse>> completeWorkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        WorkoutService.WorkoutResult result = workoutService.completeWorkout(id, user.getId());

        // Sync spec: message berisi XP + notif level up
        String message = "+" + result.workout.getXpEarned() + " XP earned! 🎉";
        if (result.levelUp) {
            message += " Level Up! 🚀 Kamu naik ke Level " + userService
                    .getUserByEmail(userDetails.getUsername()).getCurrentLevel();
        }

        return ResponseEntity.ok(ApiResponse.ok(message, workoutService.toResponse(result.workout)));
    }

    /**
     * GET /api/workouts/daily?date=yyyy-MM-dd
     * Response: ApiResponse<List<WorkoutLogResponse>>
     * date default = today jika tidak diisi
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<WorkoutLogResponse>>> getDailyWorkouts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<WorkoutLogResponse> list = workoutService
                .getWorkoutsForDate(user.getId(), date)
                .stream()
                .map(workoutService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * GET /api/workouts/weekly-stats
     * Response: ApiResponse<WeeklyStatsResponse>
     * Sync spec: wajib include totalXp, completedWorkouts, totalMinutes, totalCaloriesBurned
     */
    @GetMapping("/weekly-stats")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(workoutService.getWeeklyStats(user.getId())));
    }
}
