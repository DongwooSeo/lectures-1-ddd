package com.growmighty.lectures.firstday.tangledmonolith.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    public static User register(String email, String encodedPassword, String name, String phoneNumber) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.phoneNumber = phoneNumber;
        return user;
    }
}
