package com.healing.backend.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    public String name;
    public Integer age;
    public Float weight;
    public Float height;
    public String gender;
    public String activityLevel;
    public String goal;
    public List<String> favoriteFoods;
    public List<String> hobbies;
}
