package com.chatapp.chat_backend.service;



import com.chatapp.chat_backend.dtos.EditMessageRequestDto;
import com.chatapp.chat_backend.dtos.MessageResponseDto;
import com.chatapp.chat_backend.dtos.PageResponseDto;
import com.chatapp.chat_backend.dtos.SendMessageRequestDto;
import com.chatapp.chat_backend.entity.ChatRoom;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.ChatRoomRepository;
import com.chatapp.chat_backend.repository.MessageRepository;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.utils.MessageStatus;
import com.chatapp.chat_backend.utils.MessageType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService
{
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public MessageResponseDto sendMessage(Long chatRoomId, SendMessageRequestDto request, Long senderId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Message message = Message.builder()
                .content(request.getContent())
                .type(MessageType.valueOf(request.getType() != null ? request.getType() : "TEXT"))
                .status(MessageStatus.SENT)
                .sender(sender)
                .chatRoom(chatRoom)
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .isEdited(false)
                .isDeleted(false)
                .build();

        // Handle reply
        if (request.getReplyToId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found"));
            message.setReplyTo(replyTo);
        }

        message = messageRepository.save(message);
        return modelMapper.map(message, MessageResponseDto.class);
    }

    public PageResponseDto<MessageResponseDto> getChatRoomMessages(Long chatRoomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByChatRoomId(chatRoomId, pageable);

        List<MessageResponseDto> messages = messagePage.getContent().stream()
                .map(msg -> modelMapper.map(msg, MessageResponseDto.class))
                .collect(Collectors.toList());

        return PageResponseDto.<MessageResponseDto>builder()
                .content(messages)
                .pageNumber(messagePage.getNumber())
                .pageSize(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .last(messagePage.isLast())
                .build();
    }

    @Transactional
    public MessageResponseDto editMessage(Long messageId, EditMessageRequestDto request, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        message = messageRepository.save(message);
        return modelMapper.map(message, MessageResponseDto.class);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        messageRepository.markMessagesAsRead(chatRoomId, userId);
    }
}
