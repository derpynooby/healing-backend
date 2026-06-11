package com.healing.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatResponse {
    public Long id;
    public String role;
    public String content;
    public LocalDateTime createdAt;
}
