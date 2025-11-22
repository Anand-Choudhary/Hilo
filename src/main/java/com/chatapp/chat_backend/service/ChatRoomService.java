package com.chatapp.chat_backend.service;


import com.chatapp.chat_backend.dtos.ChatRoomResponseDto;
import com.chatapp.chat_backend.dtos.CreateChatRoomRequestDto;
import com.chatapp.chat_backend.dtos.MessageResponseDto;
import com.chatapp.chat_backend.dtos.UserResponseDto;
import com.chatapp.chat_backend.entity.ChatRoom;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.ChatRoomRepository;
import com.chatapp.chat_backend.repository.MessageRepository;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.utils.RoomType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService
{
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ChatRoomResponseDto createChatRoom(CreateChatRoomRequestDto request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(RoomType.valueOf(request.getType()))
                .avatarUrl(request.getAvatarUrl())
                .creator(creator)
                .isActive(true)
                .build();

        // Add creator as member
        chatRoom.addMember(creator);

        // Add other members for group chats
        if (request.getMemberIds() != null && request.getMemberIds().length > 0) {
            ChatRoom finalChatRoom = chatRoom;
            Arrays.stream(request.getMemberIds()).forEach(memberId -> {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));
                finalChatRoom.addMember(member);
            });
        }

        chatRoom = chatRoomRepository.save(chatRoom);
        return mapToChatRoomResponse(chatRoom, creatorId);
    }

    public ChatRoomResponseDto getChatRoomById(Long id, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // Verify user is a member of the chat room
        boolean isMember = chatRoom.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userId));

        if (!isMember) {
            throw new RuntimeException("You are not a member of this chat room");
        }

        return mapToChatRoomResponse(chatRoom, userId);
    }

    public List<ChatRoomResponseDto> getUserChatRooms(Long userId) {
        return chatRoomRepository.findByUserId(userId).stream()
                .map(room -> mapToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponseDto addMember(Long chatRoomId, Long userId, Long requesterId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // Check if requester is the creator or admin (for now, only creator can add members)
        if (!chatRoom.getCreator().getId().equals(requesterId)) {
            throw new RuntimeException("Only the creator can add members");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        chatRoom.addMember(user);
        chatRoom = chatRoomRepository.save(chatRoom);

        return mapToChatRoomResponse(chatRoom, requesterId);
    }

    @Transactional
    public void removeMember(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't allow removing the creator
        if (chatRoom.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Creator cannot leave the chat room");
        }

        chatRoom.removeMember(user);
        chatRoomRepository.save(chatRoom);
    }

    public List<ChatRoomResponseDto> searchGroupRooms(String search, Long userId) {
        return chatRoomRepository.searchGroupRooms(search).stream()
                .map(room -> mapToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponseDto getOrCreatePrivateRoom(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if private room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateRoomBetweenUsers(user1, user2);

        if (existingRoom.isPresent()) {
            return mapToChatRoomResponse(existingRoom.get(), user1Id);
        }

        // Create new private room
        ChatRoom chatRoom = ChatRoom.builder()
                .name(user2.getUsername()) // Name it after the other user
                .type(RoomType.PRIVATE)
                .creator(user1)
                .isActive(true)
                .build();

        chatRoom.addMember(user1);
        chatRoom.addMember(user2);

        chatRoom = chatRoomRepository.save(chatRoom);
        return mapToChatRoomResponse(chatRoom, user1Id);
    }

    @Transactional
    public ChatRoomResponseDto updateChatRoom(Long chatRoomId, CreateChatRoomRequestDto request, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // Only creator can update
        if (!chatRoom.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the creator can update this chat room");
        }

        if (request.getName() != null) {
            chatRoom.setName(request.getName());
        }

        if (request.getDescription() != null) {
            chatRoom.setDescription(request.getDescription());
        }

        if (request.getAvatarUrl() != null) {
            chatRoom.setAvatarUrl(request.getAvatarUrl());
        }

        chatRoom = chatRoomRepository.save(chatRoom);
        return mapToChatRoomResponse(chatRoom, userId);
    }

    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // Only creator can delete
        if (!chatRoom.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the creator can delete this chat room");
        }

        chatRoom.setIsActive(false);
        chatRoomRepository.save(chatRoom);
    }

    private ChatRoomResponseDto mapToChatRoomResponse(ChatRoom chatRoom, Long userId) {
        ChatRoomResponseDto response = modelMapper.map(chatRoom, ChatRoomResponseDto.class);

        // Set unread count
        Long unreadCount = messageRepository.countUnreadMessages(chatRoom.getId(), userId);
        response.setUnreadCount(unreadCount);

        // Set members
        List<UserResponseDto> members = chatRoom.getMembers().stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
        response.setMembers(members);

        // Set last message
        List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId());
        if (!messages.isEmpty()) {
            Message lastMessage = messages.get(0);
            response.setLastMessage(modelMapper.map(lastMessage, MessageResponseDto.class));
        }

        // Set creator
        response.setCreator(modelMapper.map(chatRoom.getCreator(), UserResponseDto.class));

        return response;
    }
}

