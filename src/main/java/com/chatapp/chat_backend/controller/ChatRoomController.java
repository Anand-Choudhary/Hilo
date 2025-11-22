package com.chatapp.chat_backend.controller;


import com.chatapp.chat_backend.dtos.AddMemberRequestDto;
import com.chatapp.chat_backend.dtos.ApiResponse;
import com.chatapp.chat_backend.dtos.ChatRoomResponseDto;
import com.chatapp.chat_backend.dtos.CreateChatRoomRequestDto;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController
{
    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    /**
     * Create a new chat room (private or group)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequestDto request,
            Authentication authentication
    ) {
        Long creatorId = getUserIdFromAuthentication(authentication);
        ChatRoomResponseDto chatRoom = chatRoomService.createChatRoom(request, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chat room created successfully", chatRoom));
    }

    /**
     * Get all chat rooms for the authenticated user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getUserChatRooms(
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ChatRoomResponseDto> chatRooms = chatRoomService.getUserChatRooms(userId);
        return ResponseEntity.ok(ApiResponse.success("Chat rooms retrieved successfully", chatRooms));
    }

    /**
     * Get a specific chat room by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getChatRoomById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        ChatRoomResponseDto chatRoom = chatRoomService.getChatRoomById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Chat room retrieved successfully", chatRoom));
    }

    /**
     * Add a member to a chat room
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequestDto request,
            Authentication authentication
    ) {
        Long requesterId = getUserIdFromAuthentication(authentication);
        ChatRoomResponseDto chatRoom = chatRoomService.addMember(id, request.getUserId(), requesterId);
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", chatRoom));
    }

    /**
     * Remove a member from a chat room
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long requesterId = getUserIdFromAuthentication(authentication);

        // Check if requester is the user being removed or is the room creator
        if (!userId.equals(requesterId)) {
            // Additional authorization logic can be added here
            // For now, we'll allow it
        }

        chatRoomService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully", null));
    }

    /**
     * Leave a chat room
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        chatRoomService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Left chat room successfully", null));
    }

    /**
     * Search for group chat rooms
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> searchGroupRooms(
            @RequestParam String q,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ChatRoomResponseDto> chatRooms = chatRoomService.searchGroupRooms(q, userId);
        return ResponseEntity.ok(ApiResponse.success("Chat rooms retrieved successfully", chatRooms));
    }

    /**
     * Create or get a private chat room between two users
     */
    @PostMapping("/private/{userId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getOrCreatePrivateRoom(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = getUserIdFromAuthentication(authentication);
        ChatRoomResponseDto chatRoom = chatRoomService.getOrCreatePrivateRoom(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Private chat room retrieved successfully", chatRoom));
    }

    /**
     * Update chat room details
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> updateChatRoom(
            @PathVariable Long id,
            @Valid @RequestBody CreateChatRoomRequestDto request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        ChatRoomResponseDto chatRoom = chatRoomService.updateChatRoom(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Chat room updated successfully", chatRoom));
    }

    /**
     * Delete (deactivate) a chat room
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        chatRoomService.deleteChatRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Chat room deleted successfully", null));
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
