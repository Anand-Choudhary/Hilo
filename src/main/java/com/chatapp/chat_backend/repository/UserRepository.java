package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<User> findByUsername(String username);

}
