package com.healing.backend.service;

import com.healing.backend.dto.ChatResponse;
import com.healing.backend.model.*;
import com.healing.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MealLogRepository mealLogRepository;
    private final WorkoutLogRepository workoutLogRepository;
    private final GeminiService geminiService;

    /**
     * Sync spec: backend wajib memproses message dari user menggunakan
     * Gemini API dan mengembalikan jawabannya dalam format ChatResponse.
     */
    @Transactional
    public ChatMessage sendMessage(User user, String userMessage) throws IOException {
        // Simpan pesan user
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        // Kumpulkan data real-time user untuk system prompt
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int consumed  = mealLogRepository.getTotalCaloriesForDate(user.getId(), today);
        int burned    = workoutLogRepository.getCaloriesBurnedForDate(user.getId(), today);
        int xpWeek    = workoutLogRepository.getXpInRange(user.getId(), weekStart, today);
        List<MealLog> todayMeals = mealLogRepository
                .findByUserIdAndDateOrderByCreatedAtAsc(user.getId(), today);

        // Build system prompt dengan konteks user
        String systemPrompt = geminiService.buildSystemPrompt(
                user, consumed, burned, xpWeek, todayMeals);

        // Ambil history (max 30 pesan), urutkan dari lama ke baru
        List<ChatMessage> history = chatMessageRepository
                .findRecentByUserId(user.getId(), PageRequest.of(0, 30));
        Collections.reverse(history);

        // Panggil Gemini API
        String reply = geminiService.chat(systemPrompt, history, userMessage);

        // Simpan respons AI
        ChatMessage aiMsg = ChatMessage.builder()
                .user(user)
                .role("assistant")
                .content(reply)
                .build();
        return chatMessageRepository.save(aiMsg);
    }

    public List<ChatMessage> getHistory(Long userId) {
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void clearHistory(Long userId) {
        chatMessageRepository.deleteByUserId(userId);
    }

    public ChatResponse toResponse(ChatMessage msg) {
        return ChatResponse.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
