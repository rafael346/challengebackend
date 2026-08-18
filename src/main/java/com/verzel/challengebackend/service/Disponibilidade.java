package com.verzel.challengebackend.service;

import java.util.List;

public record Disponibilidade(Integer fileiras, Integer colunas, List<Assento> assentosOcupados,
        Integer quantidadeDisponivel) {

    public record Assento(Integer fileira, Integer coluna) {
    }

    public static Disponibilidade paraAssentos(Integer fileiras, Integer colunas, List<Assento> assentosOcupados) {
        return new Disponibilidade(fileiras, colunas, assentosOcupados, null);
    }

    public static Disponibilidade paraPista(int quantidadeDisponivel) {
        return new Disponibilidade(null, null, null, quantidadeDisponivel);
    }
}
