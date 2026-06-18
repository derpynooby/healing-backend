package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.User;
import com.healing.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Sync spec: GET /api/user/me dan PUT /api/user/me
 * Wajib authenticated (JWT token required).
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/user/me
     * Header: Authorization: Bearer <token>
     * Response: ApiResponse<UserResponse>
     * Sync spec: UserResponse wajib include totalXp, currentLevel,
     *            levelTitle, dailyCalorieTarget, recommendedCalories
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(userService.toResponse(user)));
    }

    /**
     * PUT /api/user/me
     * Header: Authorization: Bearer <token>
     * Body: UpdateProfileRequest
     * Response: ApiResponse<UserResponse>
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest req) {
        User user    = userService.getUserByEmail(userDetails.getUsername());
        User updated = userService.updateProfile(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(
            "Profil berhasil diperbarui",
            userService.toResponse(updated)
        ));
    }
}
