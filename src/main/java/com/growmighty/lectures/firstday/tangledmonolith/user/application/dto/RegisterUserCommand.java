package com.growmighty.lectures.firstday.tangledmonolith.user.application.dto;

public record RegisterUserCommand(
        String email,
        String rawPassword,
        String name,
        String phoneNumber
) {
}
