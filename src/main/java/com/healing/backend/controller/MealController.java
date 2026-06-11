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

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<MealLogResponse>> addMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MealLogRequest req) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        MealLog meal = mealService.addMeal(user, req);
        return ResponseEntity.ok(ApiResponse.ok(
            meal.getIsCheatTreat()
                ? "Cheat & Treat terdeteksi! Porsi disesuaikan 🍫"
                : "Makanan dicatat!",
            mealService.toResponse(meal)
        ));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mealService.getDailySummary(user, date)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        mealService.deleteMeal(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Makanan dihapus", null));
    }
}
