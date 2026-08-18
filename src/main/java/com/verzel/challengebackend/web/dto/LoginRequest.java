package com.verzel.challengebackend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email é obrigatório") @Email(message = "deve ser um email válido") String email,
        @NotBlank(message = "senha é obrigatória") String senha) {
}
