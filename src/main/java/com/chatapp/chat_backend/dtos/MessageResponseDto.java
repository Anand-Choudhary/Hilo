package com.chatapp.chat_backend.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDto
{
    private Long id;
    private String content;
    private String type;
    private String status;
    private UserResponseDto sender;
    private Long chatRoomId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private MessageResponseDto replyTo;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
}
