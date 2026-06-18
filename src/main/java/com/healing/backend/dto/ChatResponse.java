package com.healing.backend.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatResponse {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
