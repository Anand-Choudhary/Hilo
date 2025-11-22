package com.chatapp.chat_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class AddMemberRequestDto
{
    @NotNull(message = "User ID is required")
    private Long userId;
}
