package com.healing.backend.service;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodScanService {

    private final FoodScanRepository foodScanRepository;
    private final MealLogRepository mealLogRepository;
    private final SmartSchedulingService schedulingService;
    private final GeminiVisionService geminiVisionService;

    /**
     * Analisis foto makanan via Gemini Vision.
     * Simpan hasil ke food_scans.
     */
    @Transactional
    public FoodScan scanFood(User user, FoodScanRequest req) throws IOException {
        FoodScan scan = geminiVisionService.analyzeFoodImage(user, req);
        scan.setUser(user);
        return foodScanRepository.save(scan);
    }

    /**
     * Log hasil scan ke meal_logs.
     * Sekaligus cek apakah perlu adaptive rescheduling.
     * Sesuai spec Prompt 3 (integrasi foto kalori → jadwal olahraga).
     */
    @Transactional
    public FoodScan logScanToMeal(User user, Long scanId, LogScanToMealRequest req)
            throws IOException {

        FoodScan scan = foodScanRepository.findById(scanId)
                .orElseThrow(() -> new RuntimeException("Scan tidak ditemukan"));
        if (!scan.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Akses ditolak");

        // Buat MealLog dari hasil scan
        MealLog mealLog = MealLog.builder()
                .user(user)
                .foodName(scan.getDetectedFoodName())
                .calories(scan.getTotalCalories() != null ? scan.getTotalCalories() : 0)
                .protein(scan.getProtein())
                .carbs(scan.getCarbohydrates())
                .fat(scan.getFat())
                .mealType(req.getMealType() != null ? req.getMealType() : "snack")
                .isCheatTreat(false)  // akan di-check oleh MealService jika dipanggil
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .build();
        mealLogRepository.save(mealLog);

        scan.setLoggedToMeal(true);
        scan.setMealType(req.getMealType());
        foodScanRepository.save(scan);

        // Sesuai spec Prompt 3: cek total kalori hari ini
        // Jika melebihi target → trigger adaptive rescheduling otomatis
        LocalDate today = LocalDate.now();
        int totalConsumedToday = mealLogRepository
                .getTotalCaloriesForDate(user.getId(), today);
        int targetCalories = user.getRecommendedCalories();

        if (totalConsumedToday > targetCalories) {
            int extra = totalConsumedToday - targetCalories;
            // Tambah workout ekstra untuk bakar kalori berlebih
            schedulingService.addExtraWorkoutForCalories(user, today, extra);
        }

        return scan;
    }

    public List<FoodScan> getScanHistory(Long userId) {
        return foodScanRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<FoodScan> getScansForDate(Long userId, LocalDate date) {
        return foodScanRepository.findByUserIdAndDateOrderByCreatedAtDesc(userId, date);
    }

    public FoodScanResponse toResponse(FoodScan scan) {
        return FoodScanResponse.builder()
                .id(scan.getId())
                .detectedFoodName(scan.getDetectedFoodName())
                .confidenceScore(scan.getConfidenceScore())
                .estimatedWeightGram(scan.getEstimatedWeightGram())
                .estimatedPortion(scan.getEstimatedPortion())
                .totalCalories(scan.getTotalCalories())
                .carbohydrates(scan.getCarbohydrates())
                .protein(scan.getProtein())
                .fat(scan.getFat())
                .fiber(scan.getFiber())
                .loggedToMeal(scan.getLoggedToMeal())
                .mealType(scan.getMealType())
                .date(scan.getDate())
                .createdAt(scan.getCreatedAt())
                // Disclaimer wajib sesuai spec
                .disclaimer(GeminiVisionService.NUTRITION_DISCLAIMER)
                .build();
    }
}
