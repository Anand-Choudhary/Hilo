package com.chatapp.chat_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class EditMessageRequestDto
{
    @NotBlank(message = "Content is required")
    private String content;
}
