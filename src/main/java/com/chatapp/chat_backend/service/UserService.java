package com.chatapp.chat_backend.service;


import com.chatapp.chat_backend.dtos.UpdateUserRequestDto;
import com.chatapp.chat_backend.dtos.UserResponseDto;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.utils.UserStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return modelMapper.map(user, UserResponseDto.class);
    }

    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return modelMapper.map(user, UserResponseDto.class);
    }

    public List<UserResponseDto> searchUsers(String search) {
        return userRepository.searchUsers(search).stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
    }

    public List<UserResponseDto> getOnlineUsers() {
        return userRepository.findOnlineUsers().stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(request.getStatus()));
        }

        user = userRepository.save(user);
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setStatus(status);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setIsActive(false);
        userRepository.save(user);
    }
}
