package com.chatapp.chat_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class MessageReaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Message message;

    @ManyToOne
    private User user;

    private String reaction; // LIKE, LOVE, LAUGH, etc.

    private LocalDateTime createdAt;
}
