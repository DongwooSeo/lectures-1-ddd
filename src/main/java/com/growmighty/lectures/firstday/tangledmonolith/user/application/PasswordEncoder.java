package com.growmighty.lectures.firstday.tangledmonolith.user.application;

public interface PasswordEncoder {
    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
