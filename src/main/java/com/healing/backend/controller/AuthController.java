package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.User;
import com.healing.backend.security.JwtUtil;
import com.healing.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

/**
 * Sync spec: /api/auth/register dan /api/auth/login
 * harus bisa diakses tanpa token (permitAll di SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/auth/register
     * Body: RegisterRequest
     * Response: ApiResponse<AuthResponse> { token, tokenType, user }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        try {
            User user   = userService.register(req);
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(ApiResponse.ok(
                "Registrasi berhasil!",
                AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(userService.toResponse(user))
                    .build()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * Body: LoginRequest { email, password }
     * Response: ApiResponse<AuthResponse> { token, tokenType, user }
     * Error 401 jika email/password salah
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
            User user    = userService.getUserByEmail(req.getEmail());
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(ApiResponse.ok(
                "Login berhasil!",
                AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(userService.toResponse(user))
                    .build()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Email atau password salah"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
