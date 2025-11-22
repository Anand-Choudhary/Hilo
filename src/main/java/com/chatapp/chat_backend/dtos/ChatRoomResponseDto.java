package com.chatapp.chat_backend.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponseDto
{
    private Long id;
    private String name;
    private String description;
    private String type;
    private String avatarUrl;
    private UserResponseDto creator;
    private List<UserResponseDto> members;
    private MessageResponseDto lastMessage;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
