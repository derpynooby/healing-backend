package com.healing.backend.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    public String token;
    public String tokenType;
    public UserResponse user;
}
