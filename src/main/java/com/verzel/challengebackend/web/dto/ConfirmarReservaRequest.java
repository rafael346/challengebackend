package com.verzel.challengebackend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarReservaRequest(@NotBlank(message = "paymentMethodId é obrigatório") String paymentMethodId) {
}
