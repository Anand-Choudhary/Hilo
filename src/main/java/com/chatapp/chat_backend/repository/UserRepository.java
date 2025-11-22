package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchUsers(@Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.status = 'ONLINE' AND u.isActive = true")
    List<User> findOnlineUsers();

}
