package com.chatapp.chat_backend.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class PinnedMessage
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Message message;

    @ManyToOne
    private ChatRoom chatRoom;

    @ManyToOne
    private User pinnedBy;

    private LocalDateTime pinnedAt;
}
