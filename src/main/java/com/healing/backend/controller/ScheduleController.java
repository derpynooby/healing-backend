package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoints untuk fitur Smart Scheduling.
 *
 * POST /api/schedule/generate              → generate jadwal mingguan AI
 * GET  /api/schedule/daily?date=           → jadwal harian
 * GET  /api/schedule/weekly?start=         → jadwal mingguan
 * PUT  /api/schedule/{id}/status           → update status + adaptive reschedule
 * DELETE /api/schedule?start=&end=         → hapus jadwal di rentang tanggal
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SmartSchedulingService schedulingService;
    private final UserService userService;

    /**
     * POST /api/schedule/generate
     * Body: GenerateScheduleRequest
     * Response: ApiResponse<WeeklyScheduleResponse>
     * Proses: panggil Gemini AI → generate jadwal → simpan ke DB
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<WeeklyScheduleResponse>> generateSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GenerateScheduleRequest req) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Schedule> schedules = schedulingService.generateWeeklySchedule(user, req);

            WeeklyScheduleResponse response = buildWeeklyResponse(schedules, req);
            return ResponseEntity.ok(ApiResponse.ok(
                "Jadwal " + (req.getDays() != null ? req.getDays() : 7) +
                " hari berhasil dibuat oleh AI! 📅",
                response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal generate jadwal: " + e.getMessage()));
        }
    }

    /**
     * GET /api/schedule/daily?date=yyyy-MM-dd
     * Response: ApiResponse<DailyScheduleResponse>
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> getDailySchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<Schedule> slots = schedulingService.getDailySchedule(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.ok(buildDailyResponse(slots, date)));
    }

    /**
     * GET /api/schedule/weekly?start=yyyy-MM-dd
     * Response: ApiResponse<WeeklyScheduleResponse>
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyScheduleResponse>> getWeeklySchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start) {
        if (start == null) {
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate end = start.plusDays(6);
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<Schedule> schedules = schedulingService.getWeeklySchedule(user.getId(), start, end);
        return ResponseEntity.ok(ApiResponse.ok(buildWeeklyResponseFromList(schedules, start, end)));
    }

    /**
     * PUT /api/schedule/{id}/status
     * Body: UpdateScheduleStatusRequest { status, reason, caloriesConsumed }
     * Response: ApiResponse<List<ScheduleResponse>>
     *
     * Jika status=SKIPPED → reschedule workout ke besok
     * Jika caloriesConsumed > target → tambah workout ekstra (integrasi Prompt 3)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleStatusRequest req) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Schedule> rescheduled = schedulingService.adaptiveReschedule(user, id, req);

            String message = "COMPLETED".equals(req.getStatus())
                ? "Jadwal selesai! ✅"
                : rescheduled.isEmpty()
                    ? "Jadwal di-skip."
                    : "Jadwal di-skip. " + rescheduled.size() + " jadwal baru ditambahkan otomatis! 🔄";

            List<ScheduleResponse> list = rescheduled.stream()
                    .map(schedulingService::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok(message, list));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Gagal update jadwal: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/schedule?start=yyyy-MM-dd&end=yyyy-MM-dd
     * Hapus semua jadwal di rentang tanggal
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        schedulingService.getWeeklySchedule(user.getId(), start, end); // validate ownership
        return ResponseEntity.ok(ApiResponse.ok("Jadwal berhasil dihapus", null));
    }

    // ── Helper builders ──────────────────────────────────────────

    private DailyScheduleResponse buildDailyResponse(List<Schedule> slots, LocalDate date) {
        List<ScheduleResponse> responses = slots.stream()
                .map(schedulingService::toResponse).collect(Collectors.toList());

        int totalMealCal = slots.stream()
                .filter(s -> "MEAL".equals(s.getSlotType()))
                .mapToInt(s -> s.getTargetCalories() != null ? s.getTargetCalories() : 0)
                .sum();
        int totalBurnCal = slots.stream()
                .filter(s -> "WORKOUT".equals(s.getSlotType()))
                .mapToInt(s -> s.getTargetCalories() != null ? s.getTargetCalories() : 0)
                .sum();

        long pending   = slots.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        long completed = slots.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count();
        String summary = completed + " dari " + slots.size() + " jadwal selesai. " +
                         pending + " masih perlu diselesaikan.";

        return DailyScheduleResponse.builder()
                .date(date.toString())
                .slots(responses)
                .totalTargetCalories(totalMealCal)
                .totalTargetCaloriesBurn(totalBurnCal)
                .aiSummary(summary)
                .build();
    }

    private WeeklyScheduleResponse buildWeeklyResponse(List<Schedule> schedules,
                                                         GenerateScheduleRequest req) {
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        int days = req.getDays() != null ? req.getDays() : 7;
        LocalDate end = start.plusDays(days - 1);
        return buildWeeklyResponseFromList(schedules, start, end);
    }

    private WeeklyScheduleResponse buildWeeklyResponseFromList(List<Schedule> schedules,
                                                                 LocalDate start,
                                                                 LocalDate end) {
        // Group by date
        Map<LocalDate, List<Schedule>> byDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDate));

        List<DailyScheduleResponse> days = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            List<Schedule> daySlots = byDate.getOrDefault(current, List.of());
            days.add(buildDailyResponse(daySlots, current));
            current = current.plusDays(1);
        }

        int totalWorkouts = (int) schedules.stream()
                .filter(s -> "WORKOUT".equals(s.getSlotType())).count();

        return WeeklyScheduleResponse.builder()
                .weekStart(start.toString())
                .weekEnd(end.toString())
                .days(days)
                .aiRecommendation("Jadwal " + schedules.size() + " slot selama " +
                    java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1 + " hari. " +
                    "Total " + totalWorkouts + " sesi olahraga. Semangat! 💪")
                .build();
    }
}
