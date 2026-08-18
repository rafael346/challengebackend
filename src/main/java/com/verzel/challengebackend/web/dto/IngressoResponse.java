package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IngressoResponse(UUID id, UUID eventoId, Integer fileira, Integer coluna, BigDecimal preco,
        StatusIngresso status, OffsetDateTime validoAte) {
    public static IngressoResponse from(Ingresso ingresso) {
        return new IngressoResponse(ingresso.getId(), ingresso.getEventoId(), ingresso.getFileira(),
                ingresso.getColuna(), ingresso.getPreco(), ingresso.getStatus(), ingresso.getValidoAte());
    }
}
