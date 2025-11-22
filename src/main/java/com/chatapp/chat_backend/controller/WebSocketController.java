package com.chatapp.chat_backend.controller;



import com.chatapp.chat_backend.dtos.MessageResponseDto;
import com.chatapp.chat_backend.dtos.SendMessageRequestDto;
import com.chatapp.chat_backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController
{
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequestDto request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Extract user ID from principal
        Long senderId = Long.parseLong(principal.getName());

        // Save message
        MessageResponseDto message = messageService.sendMessage(roomId, request, senderId);

        // Broadcast to all users in the chat room
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
    }

    @MessageMapping("/chat/{roomId}/typing")
    public void handleTyping(
            @DestinationVariable Long roomId,
            Principal principal
    ) {
        // Broadcast typing indicator
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/typing",
                principal.getName()
        );
    }
}
