package com.example.MyFirstApp.DTO.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    private String name;
    private String bio;

    private String experienceLevel;

    private String totalExperience;

    private String currentRole;

}
