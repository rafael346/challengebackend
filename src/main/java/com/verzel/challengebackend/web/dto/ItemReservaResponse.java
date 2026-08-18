package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.Ingresso;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemReservaResponse(UUID ingressoId, Integer fileira, Integer coluna, BigDecimal preco) {
    public static ItemReservaResponse from(Ingresso ingresso) {
        return new ItemReservaResponse(ingresso.getId(), ingresso.getFileira(), ingresso.getColuna(),
                ingresso.getPreco());
    }
}
