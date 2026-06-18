package com.healing.backend.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank public String name;
    @Email @NotBlank public String email;
    @NotBlank @Size(min = 6) public String password;
    public Integer age;
    public Float weight;
    public Float height;
    public String gender;
    public String activityLevel;
    public String goal;
    public List<String> favoriteFoods;
    public List<String> hobbies;
}
