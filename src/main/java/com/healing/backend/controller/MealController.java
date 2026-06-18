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

/**
 * Sync spec endpoint:
 * POST   /api/meals                        — tambah makan
 * GET    /api/meals/daily?date=yyyy-MM-dd  — summary harian
 * DELETE /api/meals/{id}                   — hapus makan
 */
@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final UserService userService;

    /**
     * POST /api/meals
     * Body: MealLogRequest { foodName, calories, protein, carbs, fat, mealType, date }
     * Response: ApiResponse<MealLogResponse>
     *
     * Sync spec — Cheat & Treat Flow:
     * Jika foodName ada di favoriteFoods user → isCheatTreat=true
     * dan message = "Cheat & Treat terdeteksi! Porsi disesuaikan 🍫"
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MealLogResponse>> addMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MealLogRequest req) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        MealLog meal = mealService.addMeal(user, req);

        // Sync spec: pesan khusus jika Cheat & Treat terdeteksi
        String message = Boolean.TRUE.equals(meal.getIsCheatTreat())
            ? "Cheat & Treat terdeteksi! Porsi disesuaikan 🍫"
            : "Makanan berhasil dicatat!";

        return ResponseEntity.ok(ApiResponse.ok(message, mealService.toResponse(meal)));
    }

    /**
     * GET /api/meals/daily?date=yyyy-MM-dd
     * Response: ApiResponse<DailySummaryResponse>
     * Sync spec: DailySummaryResponse wajib include
     *   - caloriePercent (0-100)
     *   - adjustedCaloriesPerMeal
     *   - cheatMeals list
     * date default = today jika tidak diisi
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mealService.getDailySummary(user, date)));
    }

    /**
     * DELETE /api/meals/{id}
     * Response: ApiResponse<Void>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        mealService.deleteMeal(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Makanan berhasil dihapus", null));
    }
}
