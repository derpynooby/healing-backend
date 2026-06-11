package com.healing.backend.service;

import com.healing.backend.dto.*;
import com.healing.backend.model.User;
import com.healing.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {  // <-- hapus "implements UserDetailsService"

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(user.getAge())
                .weight(user.getWeight())
                .height(user.getHeight())
                .gender(user.getGender())
                .activityLevel(user.getActivityLevel())
                .goal(user.getGoal())
                .favoriteFoods(parseList(user.getFavoriteFoods()))
                .hobbies(parseList(user.getHobbies()))
                .totalXp(user.getTotalXp())
                .currentLevel(user.getCurrentLevel())
                .levelTitle(user.getLevelTitle())
                .dailyCalorieTarget(user.getDailyCalorieTarget())
                .recommendedCalories(user.getRecommendedCalories())
                .build();
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .age(req.getAge())
                .weight(req.getWeight())
                .height(req.getHeight())
                .gender(req.getGender())
                .activityLevel(req.getActivityLevel())
                .goal(req.getGoal())
                .favoriteFoods(joinList(req.getFavoriteFoods()))
                .hobbies(joinList(req.getHobbies()))
                .totalXp(0)
                .currentLevel(1)
                .build();
        user.setDailyCalorieTarget(user.getRecommendedCalories());
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        if (req.getName() != null)          user.setName(req.getName());
        if (req.getAge() != null)           user.setAge(req.getAge());
        if (req.getWeight() != null)        user.setWeight(req.getWeight());
        if (req.getHeight() != null)        user.setHeight(req.getHeight());
        if (req.getGender() != null)        user.setGender(req.getGender());
        if (req.getActivityLevel() != null) user.setActivityLevel(req.getActivityLevel());
        if (req.getGoal() != null)          user.setGoal(req.getGoal());
        if (req.getFavoriteFoods() != null) user.setFavoriteFoods(joinList(req.getFavoriteFoods()));
        if (req.getHobbies() != null)       user.setHobbies(joinList(req.getHobbies()));
        user.setDailyCalorieTarget(user.getRecommendedCalories());
        return userRepository.save(user);
    }

    @Transactional
    public void addXp(Long userId, int xp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        user.setTotalXp(user.getTotalXp() + xp);
        user.setCurrentLevel(calculateLevel(user.getTotalXp()));
        userRepository.save(user);
    }

    private int calculateLevel(int totalXp) {
        int level = 1, needed = 500, remaining = totalXp;
        while (remaining >= needed) { remaining -= needed; level++; needed = 500 * level; }
        return level;
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.asList(csv.split(","));
    }

    private String joinList(List<String> list) {
        return list == null ? null : String.join(",", list);
    }
}
