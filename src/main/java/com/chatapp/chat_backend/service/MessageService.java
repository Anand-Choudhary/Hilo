package com.chatapp.chat_backend.service;



import com.chatapp.chat_backend.dtos.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService
{
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    /**
     * Send a new message to a chat room
     */
    @Transactional
    public MessageResponseDto sendMessage(Long chatRoomId, SendMessageRequestDto request, Long senderId) {
        log.info("Sending message to chat room {} by user {}", chatRoomId, senderId);

        // Validate chat room exists
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with id: " + chatRoomId));

        // Validate sender exists
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found with id: " + senderId));

        // Verify sender is a member of the chat room
        boolean isMember = chatRoom.getMembers().stream()
                .anyMatch(member -> member.getId().equals(senderId));

        if (!isMember) {
            throw new RuntimeException("You are not a member of this chat room");
        }

        // Create message
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

        // Handle reply to another message
        if (request.getReplyToId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found with id: " + request.getReplyToId()));
            message.setReplyTo(replyTo);
        }

        // Save message
        message = messageRepository.save(message);
        log.info("Message saved successfully with id: {}", message.getId());

        return mapToMessageResponse(message);
    }

    /**
     * Get paginated messages for a chat room
     */
    @Transactional(readOnly = true)
    public PageResponseDto<MessageResponseDto> getChatRoomMessages(Long chatRoomId, int page, int size) {
        log.info("Fetching messages for chat room {} - page: {}, size: {}", chatRoomId, page, size);

        // Validate chat room exists
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with id: " + chatRoomId));

        // Create pageable with sorting (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Fetch messages
        Page<Message> messagePage = messageRepository.findByChatRoomId(chatRoomId, pageable);

        // Map to response DTOs
        List<MessageResponseDto> messages = messagePage.getContent().stream()
                .map(this::mapToMessageResponse)
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

    /**
     * Get a specific message by ID
     */
    @Transactional(readOnly = true)
    public MessageResponseDto getMessageById(Long messageId) {
        log.info("Fetching message with id: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        return mapToMessageResponse(message);
    }

    /**
     * Edit an existing message
     */
    @Transactional
    public MessageResponseDto editMessage(Long messageId, EditMessageRequestDto request, Long userId) {
        log.info("Editing message {} by user {}", messageId, userId);

        // Fetch message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Check ownership
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        // Check if message is deleted
        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit a deleted message");
        }

        // Update message content
        message.setContent(request.getContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        // Save changes
        message = messageRepository.save(message);
        log.info("Message {} edited successfully", messageId);

        return mapToMessageResponse(message);
    }

    /**
     * Delete a message (soft delete)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        log.info("Deleting message {} by user {}", messageId, userId);

        // Fetch message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Check ownership
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Soft delete
        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setContent("[This message was deleted]");

        // Clear file attachments
        message.setFileUrl(null);
        message.setFileName(null);
        message.setFileSize(null);

        messageRepository.save(message);
        log.info("Message {} deleted successfully", messageId);
    }

    /**
     * Mark all unread messages as read for a user in a chat room
     */
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        log.info("Marking messages as read in chat room {} for user {}", chatRoomId, userId);

        // Validate chat room exists
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with id: " + chatRoomId));

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Mark messages as read
        messageRepository.markMessagesAsRead(chatRoomId, userId);
        log.info("Messages marked as read successfully");
    }

    /**
     * Search messages in a chat room
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDto> searchMessages(Long chatRoomId, String search) {
        log.info("Searching messages in chat room {} with query: {}", chatRoomId, search);

        // Validate chat room exists
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with id: " + chatRoomId));

        // Search messages
        List<Message> messages = messageRepository.searchMessages(chatRoomId, search);

        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread message count for a user in a chat room
     */
    @Transactional(readOnly = true)
    public Long getUnreadMessageCount(Long chatRoomId, Long userId) {
        log.info("Getting unread count for chat room {} and user {}", chatRoomId, userId);

        // Validate chat room exists
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with id: " + chatRoomId));

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return messageRepository.countUnreadMessages(chatRoomId, userId);
    }

    /**
     * Add a reaction to a message
     * Note: This is a placeholder implementation. In production, you should create
     * a separate MessageReaction entity to store reactions properly.
     */
    @Transactional
    public MessageResponseDto addReaction(Long messageId, Long userId, String reaction) {
        log.info("Adding reaction {} to message {} by user {}", reaction, messageId, userId);

        // Validate message exists
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // TODO: In production, create a MessageReaction entity and save it
        // For now, just return the message
        // Example:
        // MessageReaction messageReaction = new MessageReaction();
        // messageReaction.setMessage(message);
        // messageReaction.setUser(user);
        // messageReaction.setReaction(reaction);
        // messageReactionRepository.save(messageReaction);

        log.info("Reaction added successfully (placeholder implementation)");
        return mapToMessageResponse(message);
    }

    /**
     * Remove a reaction from a message
     * Note: This is a placeholder implementation.
     */
    @Transactional
    public MessageResponseDto removeReaction(Long messageId, Long userId) {
        log.info("Removing reaction from message {} by user {}", messageId, userId);

        // Validate message exists
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // TODO: In production, delete the MessageReaction entity
        // Example:
        // messageReactionRepository.deleteByMessageIdAndUserId(messageId, userId);

        log.info("Reaction removed successfully (placeholder implementation)");
        return mapToMessageResponse(message);
    }

    /**
     * Forward a message to another chat room
     */
    @Transactional
    public MessageResponseDto forwardMessage(Long messageId, Long targetChatRoomId, Long userId) {
        log.info("Forwarding message {} to chat room {} by user {}", messageId, targetChatRoomId, userId);

        // Validate original message
        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Validate target chat room
        ChatRoom targetChatRoom = chatRoomRepository.findById(targetChatRoomId)
                .orElseThrow(() -> new RuntimeException("Target chat room not found with id: " + targetChatRoomId));

        // Validate user
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Verify user is a member of the target chat room
        boolean isMember = targetChatRoom.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userId));

        if (!isMember) {
            throw new RuntimeException("You are not a member of the target chat room");
        }

        // Cannot forward deleted messages
        if (originalMessage.getIsDeleted()) {
            throw new RuntimeException("Cannot forward a deleted message");
        }

        // Create forwarded message
        Message forwardedMessage = Message.builder()
                .content(originalMessage.getContent())
                .type(originalMessage.getType())
                .status(MessageStatus.SENT)
                .sender(sender)
                .chatRoom(targetChatRoom)
                .fileUrl(originalMessage.getFileUrl())
                .fileName(originalMessage.getFileName())
                .fileSize(originalMessage.getFileSize())
                .isEdited(false)
                .isDeleted(false)
                .build();

        forwardedMessage = messageRepository.save(forwardedMessage);
        log.info("Message forwarded successfully with new id: {}", forwardedMessage.getId());

        return mapToMessageResponse(forwardedMessage);
    }

    /**
     * Pin a message in a chat room
     * Note: This is a placeholder implementation. In production, you should add
     * a pinnedMessages field to ChatRoom or create a separate PinnedMessage entity.
     */
    @Transactional
    public MessageResponseDto pinMessage(Long messageId, Long chatRoomId, Long userId) {
        log.info("Pinning message {} in chat room {} by user {}", messageId, chatRoomId, userId);

        // Validate message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Validate chat room
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found with id: " + chatRoomId));

        // Verify message belongs to this chat room
        if (!message.getChatRoom().getId().equals(chatRoomId)) {
            throw new RuntimeException("Message does not belong to this chat room");
        }

        // Verify user is creator or admin (for now, only creator can pin)
        if (!chatRoom.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the chat room creator can pin messages");
        }

        // Cannot pin deleted messages
        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot pin a deleted message");
        }

        // TODO: In production, implement proper pinning logic
        // Example:
        // PinnedMessage pinnedMessage = new PinnedMessage();
        // pinnedMessage.setMessage(message);
        // pinnedMessage.setChatRoom(chatRoom);
        // pinnedMessage.setPinnedBy(user);
        // pinnedMessageRepository.save(pinnedMessage);

        log.info("Message pinned successfully (placeholder implementation)");
        return mapToMessageResponse(message);
    }

    /**
     * Unpin a message in a chat room
     * Note: This is a placeholder implementation.
     */
    @Transactional
    public void unpinMessage(Long messageId, Long chatRoomId, Long userId) {
        log.info("Unpinning message {} in chat room {} by user {}", messageId, chatRoomId, userId);

        // Validate message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // Validate chat room
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found with id: " + chatRoomId));

        // Verify message belongs to this chat room
        if (!message.getChatRoom().getId().equals(chatRoomId)) {
            throw new RuntimeException("Message does not belong to this chat room");
        }

        // Verify user is creator or admin
        if (!chatRoom.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the chat room creator can unpin messages");
        }

        // TODO: In production, delete the PinnedMessage entity
        // Example:
        // pinnedMessageRepository.deleteByMessageIdAndChatRoomId(messageId, chatRoomId);

        log.info("Message unpinned successfully (placeholder implementation)");
    }

    /**
     * Map Message entity to MessageResponse DTO
     */
    private MessageResponseDto mapToMessageResponse(Message message) {
        MessageResponseDto response = modelMapper.map(message, MessageResponseDto.class);

        // Map sender
        if (message.getSender() != null) {
            response.setSender(modelMapper.map(message.getSender(), UserResponseDto.class));
        }

        // Map chatRoomId
        if (message.getChatRoom() != null) {
            response.setChatRoomId(message.getChatRoom().getId());
        }

        // Map reply-to message (without nested replies to avoid infinite recursion)
        if (message.getReplyTo() != null) {
            MessageResponseDto replyToResponse = new MessageResponseDto();
            replyToResponse.setId(message.getReplyTo().getId());
            replyToResponse.setContent(message.getReplyTo().getContent());
            replyToResponse.setType(message.getReplyTo().getType().name());
            replyToResponse.setCreatedAt(message.getReplyTo().getCreatedAt());

            if (message.getReplyTo().getSender() != null) {
                replyToResponse.setSender(modelMapper.map(message.getReplyTo().getSender(), UserResponseDto.class));
            }

            response.setReplyTo(replyToResponse);
        }

        return response;
    }
}
