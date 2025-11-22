package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.ChatRoom;
import com.chatapp.chat_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>
{
    List<ChatRoom> findByIsActiveTrue();

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members m WHERE m.id = :userId AND cr.isActive = true")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'PRIVATE' AND " +
            "SIZE(cr.members) = 2 AND :user1 MEMBER OF cr.members AND :user2 MEMBER OF cr.members")
    Optional<ChatRoom> findPrivateRoomBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'GROUP' AND " +
            "(LOWER(cr.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(cr.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "cr.isActive = true")
    List<ChatRoom> searchGroupRooms(@Param("search") String search);
}
