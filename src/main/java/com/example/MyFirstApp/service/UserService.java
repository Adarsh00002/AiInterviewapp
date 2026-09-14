package com.example.MyFirstApp.service;


import com.example.MyFirstApp.DTO.LoginDTO.LoginRequestDTO;

import com.example.MyFirstApp.DTO.UserDTO.UpdateProfileRequest;
import com.example.MyFirstApp.DTO.UserDTO.UserDTO;
import com.example.MyFirstApp.DTO.UserDTO.UserResponse;
import com.example.MyFirstApp.Entity.UserEntity;
import com.example.MyFirstApp.JwtUtil.JwtUtil;
import com.example.MyFirstApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserDTO registerUser(UserDTO userDTO){
        if(userDTO.getEmail() == null || userDTO.getEmail().isBlank()){
            throw new RuntimeException("Email Cannot be empty");
        }

        userDTO.setEmail(userDTO.getEmail().trim());

        if(! userDTO.getEmail().contains("@")){
            throw new RuntimeException("Invalid email formate:" + userDTO.getEmail());

        }

        if(userRepository.findByEmail(userDTO.getEmail()).isPresent()){
            throw new RuntimeException("Email already registerd");

        }

        UserEntity newUserEntity=toEntity(userDTO);
        newUserEntity=userRepository.save(newUserEntity);

        return toDTO(newUserEntity);

    }

    public UserEntity toEntity(@NotNull UserDTO userDTO ){
        return UserEntity.builder()
                .name(userDTO.getName())
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .profileImage(userDTO.getProfileImage())
                .build();

    }

    public UserDTO toDTO(@NotNull UserEntity userEntity){

        return UserDTO.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .profileImage(userEntity.getProfileImage())
                .build();
    }


    public String login(LoginRequestDTO loginRequestDTO){

        UserEntity userEntity=userRepository.findByEmail(loginRequestDTO.getEmail()).orElseThrow(()-> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(loginRequestDTO.getPassword(),userEntity.getPassword())){
            throw new RuntimeException("Invalid password");
        }

        return  jwtUtil.generateToken(userEntity.getEmail());
    }

      public UserResponse getProfile(String email){
        UserEntity userEntity=userRepository.findByEmail(email).orElseThrow(() ->new RuntimeException("User not found"));

        return UserResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .email(userEntity.getEmail())
                .profileImage(userEntity.getProfileImage())
                .bio(userEntity.getBio())
                .experienceLevel(userEntity.getExperienceLevel())
                .totalExperience(userEntity.getTotalExperience())
                .currentRole(userEntity.getCurrentRole())
                .build();
      }

    public UserEntity updateProfileImage(String email,String imageUrl){

        UserEntity user=userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User Not Found"));

        user.setProfileImage(imageUrl);

        return userRepository.save(user);
    }

    public UserResponse updateProfile(String email,
                                      UpdateProfileRequest request){

        UserEntity user=userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setBio(request.getBio());
        user.setExperienceLevel(request.getExperienceLevel());
        user.setTotalExperience(request.getTotalExperience());
        user.setCurrentRole(request.getCurrentRole());

        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .experienceLevel(user.getExperienceLevel())
                .totalExperience(user.getTotalExperience())
                .currentRole(user.getCurrentRole())
                .build();
    }


}
