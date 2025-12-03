package com.ticket.chatbot.service;

import com.ticket.chatbot.model.User;
import com.ticket.chatbot.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User findOrCreateUser(String email, String name) {
        // Check if user exists
        User user = userRepository.findByEmail(email);

        if (user == null) {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user = userRepository.save(user);
        }

        return user;
    }
}