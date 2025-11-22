package com.chatapp.chat_backend.dtos;


import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto
{
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}
