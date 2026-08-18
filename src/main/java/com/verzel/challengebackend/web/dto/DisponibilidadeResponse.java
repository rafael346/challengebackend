package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.service.Disponibilidade;
import java.util.List;

public record DisponibilidadeResponse(Integer fileiras, Integer colunas,
        List<AssentoOcupadoResponse> assentosOcupados, Integer quantidadeDisponivel) {

    public static DisponibilidadeResponse from(Disponibilidade disponibilidade) {
        List<AssentoOcupadoResponse> ocupados = disponibilidade.assentosOcupados() == null ? null
                : disponibilidade.assentosOcupados().stream()
                        .map(a -> new AssentoOcupadoResponse(a.fileira(), a.coluna()))
                        .toList();
        return new DisponibilidadeResponse(disponibilidade.fileiras(), disponibilidade.colunas(), ocupados,
                disponibilidade.quantidadeDisponivel());
    }

    public record AssentoOcupadoResponse(Integer fileira, Integer coluna) {
    }
}
