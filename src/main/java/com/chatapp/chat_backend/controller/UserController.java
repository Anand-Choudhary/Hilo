package com.chatapp.chat_backend.controller;



import com.chatapp.chat_backend.dtos.ApiResponse;
import com.chatapp.chat_backend.dtos.UpdateUserRequestDto;
import com.chatapp.chat_backend.dtos.UserResponseDto;
import com.chatapp.chat_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController
{
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        UserResponseDto user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> searchUsers(@RequestParam String q) {
        List<UserResponseDto> users = userService.searchUsers(q);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/online")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getOnlineUsers() {
        List<UserResponseDto> users = userService.getOnlineUsers();
        return ResponseEntity.ok(ApiResponse.success("Online users retrieved successfully", users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequestDto request,
            Authentication authentication
    ) {
        UserResponseDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }
}
