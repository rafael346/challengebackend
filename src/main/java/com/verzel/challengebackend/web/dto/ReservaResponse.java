package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.Ingresso;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservaResponse(UUID reservaId, List<ItemReservaResponse> itens, BigDecimal valorTotal,
        OffsetDateTime expiraEm) {
    public static ReservaResponse from(List<Ingresso> ingressos) {
        UUID reservaId = ingressos.get(0).getReservaId();
        OffsetDateTime expiraEm = ingressos.get(0).getExpiraEm();
        BigDecimal valorTotal = ingressos.stream().map(Ingresso::getPreco).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ItemReservaResponse> itens = ingressos.stream().map(ItemReservaResponse::from).toList();
        return new ReservaResponse(reservaId, itens, valorTotal, expiraEm);
    }
}
