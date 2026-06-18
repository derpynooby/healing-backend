package com.healing.backend.controller;

import com.healing.backend.dto.*;
import com.healing.backend.model.*;
import com.healing.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sync spec endpoints:
 * POST   /api/chat          — kirim pesan ke Gemini AI
 * GET    /api/chat/history  — ambil riwayat chat
 * DELETE /api/chat/history  — hapus riwayat chat
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * POST /api/chat
     * Body: ChatRequest { message }
     * Response: ApiResponse<ChatResponse> { id, role, content, createdAt }
     *
     * Sync spec: backend wajib memproses message menggunakan Gemini API
     * dan mengembalikan jawabannya dalam format ChatResponse.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChatRequest req) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            ChatMessage reply = chatService.sendMessage(user, req.getMessage());
            return ResponseEntity.ok(ApiResponse.ok(chatService.toResponse(reply)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("AI tidak tersedia saat ini: " + e.getMessage()));
        }
    }

    /**
     * GET /api/chat/history
     * Response: ApiResponse<List<ChatResponse>>
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<ChatResponse> history = chatService.getHistory(user.getId())
                .stream()
                .map(chatService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * DELETE /api/chat/history
     * Response: ApiResponse<Void>
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        chatService.clearHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Riwayat chat berhasil dihapus", null));
    }
}
