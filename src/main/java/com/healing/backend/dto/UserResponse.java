package com.healing.backend.dto;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private Float weight;
    private Float height;
    private String gender;
    private String activityLevel;
    private String goal;
    private List<String> favoriteFoods;
    private List<String> hobbies;
    // Sync spec fields
    private Integer totalXp;
    private Integer currentLevel;
    private String levelTitle;
    private Integer dailyCalorieTarget;
    private Integer recommendedCalories;
}
