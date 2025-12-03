package com.ticket.chatbot.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String email;

    private String name;
    private String profilePicture;

    // Add any additional user fields you need
}