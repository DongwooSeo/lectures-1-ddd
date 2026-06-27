package com.growmighty.lectures.firstday.tangledmonolith.user.application.dto;

public record LoginCommand(String email, String rawPassword) {
}
