package com.healing.backend.dto;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
}
