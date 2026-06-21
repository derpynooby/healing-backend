package com.healing.backend.dto;

import lombok.*;

// ─────────────── FOOD SCAN ───────────────

/**
 * Request scan foto makanan.
 * imageBase64: gambar dalam format base64 string.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class FoodScanRequest {
    // Gambar dalam format base64
    public String imageBase64;
    // MIME type: "image/jpeg" atau "image/png"
    public String mimeType;
}

