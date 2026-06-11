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

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

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
                    .body(ApiResponse.error("AI tidak tersedia: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<ChatResponse> history = chatService.getHistory(user.getId())
                .stream().map(chatService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        chatService.clearHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Riwayat chat dihapus", null));
    }
}
