package com.verzel.challengebackend.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ReservaRequest(
        @Valid List<AssentoRequest> assentos,
        @Positive(message = "deve ser positivo") Integer quantidade) {
}
