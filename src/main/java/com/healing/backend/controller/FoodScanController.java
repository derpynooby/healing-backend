package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints untuk fitur AI Calorie Tracker dari foto.
 *
 * POST /api/food-scan               → scan foto makanan
 * POST /api/food-scan/{id}/log      → log hasil scan ke meal
 * GET  /api/food-scan/history       → riwayat scan
 * GET  /api/food-scan/today         → scan hari ini
 */
@RestController
@RequestMapping("/api/food-scan")
@RequiredArgsConstructor
public class FoodScanController {

    private final FoodScanService foodScanService;
    private final UserService userService;

    /**
     * POST /api/food-scan
     * Body: { imageBase64: "...", mimeType: "image/jpeg" }
     * Response: ApiResponse<FoodScanResponse> dengan data nutrisi + disclaimer
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FoodScanResponse>> scanFood(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FoodScanRequest req) {
        try {
            if (req.getImageBase64() == null || req.getImageBase64().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("imageBase64 tidak boleh kosong"));
            }
            User user   = userService.getUserByEmail(userDetails.getUsername());
            FoodScan scan = foodScanService.scanFood(user, req);
            return ResponseEntity.ok(ApiResponse.ok(
                "Analisis foto berhasil! Periksa estimasi nutrisi di bawah.",
                foodScanService.toResponse(scan)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal menganalisis foto: " + e.getMessage()));
        }
    }

    /**
     * POST /api/food-scan/{id}/log
     * Body: { mealType: "lunch", date: "2024-06-17" }
     * Response: ApiResponse<FoodScanResponse>
     * Juga trigger adaptive rescheduling jika kalori melebihi target.
     */
    @PostMapping("/{id}/log")
    public ResponseEntity<ApiResponse<FoodScanResponse>> logToMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody LogScanToMealRequest req) {
        try {
            User user     = userService.getUserByEmail(userDetails.getUsername());
            FoodScan scan = foodScanService.logScanToMeal(user, id, req);
            return ResponseEntity.ok(ApiResponse.ok(
                "Makanan berhasil dicatat ke log harian! 🥗",
                foodScanService.toResponse(scan)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal log makanan: " + e.getMessage()));
        }
    }

    /**
     * GET /api/food-scan/history
     * Response: ApiResponse<List<FoodScanResponse>>
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<FoodScanResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<FoodScanResponse> list = foodScanService.getScanHistory(user.getId())
                .stream().map(foodScanService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * GET /api/food-scan/today
     * Response: ApiResponse<List<FoodScanResponse>>
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<FoodScanResponse>>> getToday(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<FoodScanResponse> list = foodScanService.getScansForDate(user.getId(), date)
                .stream().map(foodScanService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
