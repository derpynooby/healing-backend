package com.healing.backend.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    public Long id;
    public String name;
    public String email;
    public Integer age;
    public Float weight;
    public Float height;
    public String gender;
    public String activityLevel;
    public String goal;
    public List<String> favoriteFoods;
    public List<String> hobbies;
    public Integer totalXp;
    public Integer currentLevel;
    public String levelTitle;
    public Integer dailyCalorieTarget;
    public Integer recommendedCalories;
}
