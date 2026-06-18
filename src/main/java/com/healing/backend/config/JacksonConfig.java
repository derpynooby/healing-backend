package com.healing.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sync spec — Format Tanggal & Waktu:
 * - LocalDate     → "yyyy-MM-dd"         (contoh: "2024-06-17")
 * - LocalDateTime → "yyyy-MM-dd'T'HH:mm:ss" (contoh: "2024-06-17T08:30:00")
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_FORMAT     = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();

        // Serializers (Java → JSON)
        module.addSerializer(LocalDate.class,
            new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        module.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));

        // Deserializers (JSON → Java)
        module.addDeserializer(LocalDate.class,
            new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        module.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));

        return new ObjectMapper()
                .registerModule(module)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
