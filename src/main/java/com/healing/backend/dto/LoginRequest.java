package com.healing.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @Email @NotBlank public String email;
    @NotBlank public String password;
}
