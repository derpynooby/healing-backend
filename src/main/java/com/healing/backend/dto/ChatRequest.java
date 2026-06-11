package com.healing.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChatRequest {
    @NotBlank public String message;
}
