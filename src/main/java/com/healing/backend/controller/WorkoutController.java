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

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutLogResponse>> addWorkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WorkoutLogRequest req) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        WorkoutLog w = workoutService.addWorkout(user, req);
        return ResponseEntity.ok(ApiResponse.ok("Aktivitas ditambahkan!", workoutService.toResponse(w)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<WorkoutLogResponse>> completeWorkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        WorkoutLog w = workoutService.completeWorkout(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(
            "+" + w.getXpEarned() + " XP earned! 🎉",
            workoutService.toResponse(w)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<WorkoutLogResponse>>> getDailyWorkouts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<WorkoutLogResponse> list = workoutService
                .getWorkoutsForDate(user.getId(), date)
                .stream().map(workoutService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/weekly-stats")
    public ResponseEntity<ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(workoutService.getWeeklyStats(user.getId())));
    }
}
