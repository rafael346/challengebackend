package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.service.IngressoCompartilhado;
import java.util.UUID;

public record IngressoPublicoResponse(UUID ingressoId, UUID eventoId, String eventoNome, Integer fileira,
        Integer coluna, StatusIngresso status) {

    public static IngressoPublicoResponse from(IngressoCompartilhado compartilhado) {
        return new IngressoPublicoResponse(compartilhado.ingresso().getId(), compartilhado.evento().getId(),
                compartilhado.evento().getTitulo(), compartilhado.ingresso().getFileira(),
                compartilhado.ingresso().getColuna(), compartilhado.ingresso().getStatus());
    }
}
