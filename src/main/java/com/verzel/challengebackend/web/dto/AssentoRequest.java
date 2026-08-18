package com.verzel.challengebackend.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssentoRequest(
        @NotNull(message = "fileira é obrigatória") @Positive(message = "deve ser positivo") Integer fileira,
        @NotNull(message = "coluna é obrigatória") @Positive(message = "deve ser positivo") Integer coluna) {
}
