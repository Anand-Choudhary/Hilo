package com.chatapp.chat_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class CreateChatRoomRequestDto
{
    @NotBlank(message = "Room name is required")
    @Size(max = 100, message = "Room name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Room type is required")
    private String type; // private yaa group

    private String avatarUrl;

    private Long[] memberIds;
}
