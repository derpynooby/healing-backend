package com.healing.backend.service;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.repository.MealLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealLogRepository mealLogRepository;

    @Transactional
    public MealLog addMeal(User user, MealLogRequest req) {
        // Sync spec: cek apakah foodName ada di favoriteFoods user
        boolean isCheat = isCheatTreat(user, req.getFoodName());

        MealLog meal = MealLog.builder()
                .user(user)
                .foodName(req.getFoodName())
                .calories(req.getCalories() != null ? req.getCalories() : 0)
                .protein(req.getProtein())
                .carbs(req.getCarbs())
                .fat(req.getFat())
                .mealType(req.getMealType())
                .isCheatTreat(isCheat)
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .build();

        return mealLogRepository.save(meal);
    }

    /**
     * Sync spec: DailySummaryResponse wajib include
     * - caloriePercent (0-100)
     * - adjustedCaloriesPerMeal
     * - cheatMeals list
     */
    public DailySummaryResponse getDailySummary(User user, LocalDate date) {
        List<MealLog> meals  = mealLogRepository
                .findByUserIdAndDateOrderByCreatedAtAsc(user.getId(), date);
        int total   = mealLogRepository.getTotalCaloriesForDate(user.getId(), date);
        int target  = user.getRecommendedCalories();
        int remaining = Math.max(0, target - total);
        List<MealLog> cheats = mealLogRepository.getCheatMealsForDate(user.getId(), date);

        int percent = target > 0 ? Math.min(100, total * 100 / target) : 0;
        int mealsLeft = estimateMealsLeft(date);
        int adjustedPerMeal = mealsLeft > 0 ? Math.max(200, remaining / mealsLeft) : 0;

        return DailySummaryResponse.builder()
                .date(date.toString())
                .totalCalories(total)
                .targetCalories(target)
                .remainingCalories(remaining)
                .caloriePercent(percent)
                .meals(meals.stream().map(this::toResponse).collect(Collectors.toList()))
                .cheatMeals(cheats.stream().map(this::toResponse).collect(Collectors.toList()))
                .adjustedCaloriesPerMeal(adjustedPerMeal)
                .build();
    }

    @Transactional
    public void deleteMeal(Long mealId, Long userId) {
        MealLog meal = mealLogRepository.findById(mealId)
                .orElseThrow(() -> new RuntimeException("Meal tidak ditemukan"));
        if (!meal.getUser().getId().equals(userId))
            throw new RuntimeException("Akses ditolak");
        mealLogRepository.delete(meal);
    }

    /**
     * Sync spec: cek apakah foodName ada di favoriteFoods user
     * case-insensitive + partial match
     */
    public boolean isCheatTreat(User user, String foodName) {
        if (user.getFavoriteFoods() == null || user.getFavoriteFoods().isBlank()) return false;
        String lower = foodName.toLowerCase().trim();
        return Arrays.stream(user.getFavoriteFoods().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(fav -> lower.contains(fav) || fav.contains(lower));
    }

    /**
     * Map MealLog → MealLogResponse
     * Sync spec: wajib include mealTypeLabel dan isCheatTreat sebagai Boolean
     */
    public MealLogResponse toResponse(MealLog meal) {
        return MealLogResponse.builder()
                .id(meal.getId())
                .foodName(meal.getFoodName())
                .calories(meal.getCalories())
                .protein(meal.getProtein())
                .carbs(meal.getCarbs())
                .fat(meal.getFat())
                .mealType(meal.getMealType())
                .mealTypeLabel(meal.getMealTypeLabel())   // "Sarapan","Snack","Makan Siang","Makan Malam"
                .isCheatTreat(meal.getIsCheatTreat())     // Boolean (nullable-safe)
                .date(meal.getDate())
                .createdAt(meal.getCreatedAt())
                .build();
    }

    private int estimateMealsLeft(LocalDate date) {
        if (!date.equals(LocalDate.now())) return 0;
        int hour = LocalTime.now().getHour();
        if (hour < 9)  return 3;
        if (hour < 12) return 2;
        if (hour < 18) return 1;
        return 0;
    }
}
