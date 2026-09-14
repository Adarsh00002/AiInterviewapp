package com.example.MyFirstApp.controller;


import com.example.MyFirstApp.DTO.LoginDTO.LoginRequestDTO;
import com.example.MyFirstApp.DTO.LoginDTO.LoginResponseDTO;
import com.example.MyFirstApp.DTO.UserDTO.UpdateProfileRequest;
import com.example.MyFirstApp.DTO.UserDTO.UserDTO;
import com.example.MyFirstApp.DTO.UserDTO.UserResponse;
import com.example.MyFirstApp.Entity.UserEntity;
import com.example.MyFirstApp.service.CloudinaryService;
import com.example.MyFirstApp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private  final CloudinaryService cloudinaryService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUsers(@RequestBody UserDTO userDTO){

        UserDTO registerUser=userService.registerUser(userDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUser);

    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody LoginRequestDTO loginRequestDTO
            ){
        String token=userService.login(loginRequestDTO);

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication){
        String email=authentication.getName();
        return  ResponseEntity.ok(userService.getProfile(email));
    }

    @PostMapping("/profile/image")
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ){

        String email=authentication.getName();

        String imageUrl=cloudinaryService.uploadImage(image);

        UserEntity user=userService.updateProfileImage(email,imageUrl);
        UserResponse userResponse=userService.getProfile(email);
        return ResponseEntity.ok(userResponse);

    }

    @PutMapping("/profile/updateprofile")
    public  ResponseEntity<UserResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication
            ){

        String email=authentication.getName();

        return  ResponseEntity.ok(
                userService.updateProfile(email,request)
        );
    }
}
