package com.chatapp.chat_backend.controller;



import com.chatapp.chat_backend.dtos.*;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms/{chatRoomId}/messages")
@RequiredArgsConstructor
public class MessageController
{
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message to a chat room
     * POST /api/chatrooms/{chatRoomId}/messages
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponseDto>> sendMessage(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody SendMessageRequestDto request,
            Authentication authentication
    ) {
        Long senderId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.sendMessage(chatRoomId, request, senderId);

        // Broadcast the message via WebSocket to all users in the chat room
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, message);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", message));
    }

    /**
     * Get paginated messages for a chat room
     * GET /api/chatrooms/{chatRoomId}/messages?page=0&size=50
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<MessageResponseDto>>> getChatRoomMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        // Verify user has access to this chat room
        getUserIdFromAuthentication(authentication);

        PageResponseDto<MessageResponseDto> messages = messageService.getChatRoomMessages(chatRoomId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    /**
     * Get a specific message by ID
     * GET /api/chatrooms/{chatRoomId}/messages/{messageId}
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponseDto>> getMessageById(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.getMessageById(messageId);
        return ResponseEntity.ok(ApiResponse.success("Message retrieved successfully", message));
    }

    /**
     * Edit a message
     * PUT /api/chatrooms/{chatRoomId}/messages/{messageId}
     */
    @PutMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponseDto>> editMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequestDto request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.editMessage(messageId, request, userId);

        // Broadcast the edited message via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/edit", message);

        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", message));
    }

    /**
     * Delete a message (soft delete)
     * DELETE /api/chatrooms/{chatRoomId}/messages/{messageId}
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        messageService.deleteMessage(messageId, userId);

        // Broadcast the message deletion via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/delete", messageId);

        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    /**
     * Mark all messages as read in a chat room
     * POST /api/chatrooms/{chatRoomId}/messages/read
     */
    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable Long chatRoomId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        messageService.markMessagesAsRead(chatRoomId, userId);

        // Notify other users that messages have been read
        messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId + "/read",
                userId
        );

        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    /**
     * Search messages in a chat room
     * GET /api/chatrooms/{chatRoomId}/messages/search?q=hello
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> searchMessages(
            @PathVariable Long chatRoomId,
            @RequestParam String q,
            Authentication authentication
    ) {
        getUserIdFromAuthentication(authentication);
        List<MessageResponseDto> messages = messageService.searchMessages(chatRoomId, q);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    /**
     * Get unread message count for a chat room
     * GET /api/chatrooms/{chatRoomId}/messages/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable Long chatRoomId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        Long count = messageService.getUnreadMessageCount(chatRoomId, userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", count));
    }

    /**
     * React to a message (like, love, etc.)
     * POST /api/chatrooms/{chatRoomId}/messages/{messageId}/react?reaction=LIKE
     */
    @PostMapping("/{messageId}/react")
    public ResponseEntity<ApiResponse<MessageResponseDto>> reactToMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            @RequestParam String reaction,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.addReaction(messageId, userId, reaction);

        // Broadcast the reaction via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/reaction", message);

        return ResponseEntity.ok(ApiResponse.success("Reaction added successfully", message));
    }

    /**
     * Remove reaction from a message
     * DELETE /api/chatrooms/{chatRoomId}/messages/{messageId}/react
     */
    @DeleteMapping("/{messageId}/react")
    public ResponseEntity<ApiResponse<MessageResponseDto>> removeReaction(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.removeReaction(messageId, userId);

        // Broadcast the reaction removal via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/reaction", message);

        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully", message));
    }

    /**
     * Forward a message to another chat room
     * POST /api/chatrooms/{chatRoomId}/messages/{messageId}/forward?targetChatRoomId=2
     */
    @PostMapping("/{messageId}/forward")
    public ResponseEntity<ApiResponse<MessageResponseDto>> forwardMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            @RequestParam Long targetChatRoomId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.forwardMessage(messageId, targetChatRoomId, userId);

        // Broadcast to target chat room
        messagingTemplate.convertAndSend("/topic/chat/" + targetChatRoomId, message);

        return ResponseEntity.ok(ApiResponse.success("Message forwarded successfully", message));
    }

    /**
     * Pin a message in a chat room
     * POST /api/chatrooms/{chatRoomId}/messages/{messageId}/pin
     */
    @PostMapping("/{messageId}/pin")
    public ResponseEntity<ApiResponse<MessageResponseDto>> pinMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        MessageResponseDto message = messageService.pinMessage(messageId, chatRoomId, userId);

        // Broadcast the pin action via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/pin", message);

        return ResponseEntity.ok(ApiResponse.success("Message pinned successfully", message));
    }

    /**
     * Unpin a message in a chat room
     * DELETE /api/chatrooms/{chatRoomId}/messages/{messageId}/pin
     */
    @DeleteMapping("/{messageId}/pin")
    public ResponseEntity<ApiResponse<Void>> unpinMessage(
            @PathVariable Long chatRoomId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        messageService.unpinMessage(messageId, chatRoomId, userId);

        // Broadcast the unpin action via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/unpin", messageId);

        return ResponseEntity.ok(ApiResponse.success("Message unpinned successfully", null));
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}

