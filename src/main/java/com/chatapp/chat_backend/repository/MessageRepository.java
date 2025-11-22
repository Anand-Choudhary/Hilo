package com.chatapp.chat_backend.repository;


import com.chatapp.chat_backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>
{
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND " +
            "m.sender.id != :userId AND m.status != 'READ' AND m.isDeleted = false")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ' WHERE m.chatRoom.id = :chatRoomId AND " +
            "m.sender.id != :userId AND m.status != 'READ'")
    void markMessagesAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.isDeleted = false AND " +
            "(LOWER(m.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Message> searchMessages(@Param("chatRoomId") Long chatRoomId, @Param("search") String search);
}
