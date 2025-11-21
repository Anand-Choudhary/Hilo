package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long>
{

}
