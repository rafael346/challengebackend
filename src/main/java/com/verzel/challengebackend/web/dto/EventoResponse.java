package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String titulo,
        CategoriaEvento categoria,
        String descricao,
        String local,
        OffsetDateTime dataHora,
        FormaVenda formaVenda,
        Integer fileiras,
        Integer colunas,
        Integer quantidadeTotalIngressos,
        BigDecimal preco,
        UUID organizerId,
        OffsetDateTime createdAt) {

    public static EventoResponse from(Evento evento) {
        return new EventoResponse(evento.getId(), evento.getTitulo(), evento.getCategoria(), evento.getDescricao(),
                evento.getLocal(), evento.getDataHora(), evento.getFormaVenda(), evento.getFileiras(),
                evento.getColunas(), evento.getQuantidadeTotalIngressos(), evento.getPreco(),
                evento.getOrganizerId(), evento.getCreatedAt());
    }
}
