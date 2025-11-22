package com.chatapp.chat_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDto
{
    @NotBlank(message = "Content is required")
    private String content;

    private String type; // TEXT, IMAGE, FILE, etc.

    private Long replyToId;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
