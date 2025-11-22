package com.chatapp.chat_backend.service;


import com.chatapp.chat_backend.dtos.ChatRoomResponseDto;
import com.chatapp.chat_backend.dtos.CreateChatRoomRequestDto;
import com.chatapp.chat_backend.dtos.UserResponseDto;
import com.chatapp.chat_backend.entity.ChatRoom;
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

        chatRoom.removeMember(user);
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

        return response;
    }
}
