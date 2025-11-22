package com.chatapp.chat_backend.controller;



import com.chatapp.chat_backend.dtos.ApiResponse;
import com.chatapp.chat_backend.dtos.AuthResponseDto;
import com.chatapp.chat_backend.dtos.LoginRequestDto;
import com.chatapp.chat_backend.dtos.RegisterRequestDto;
import com.chatapp.chat_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
