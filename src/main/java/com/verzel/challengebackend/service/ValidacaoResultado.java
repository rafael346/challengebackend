package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Ingresso;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Resultado de uma tentativa de validação de ingresso na portaria — não é uma tabela, é um
 * read model retornado por ValidacaoService (mesmo papel de Disponibilidade). */
public record ValidacaoResultado(ResultadoValidacao resultado, UUID ingressoId, Integer fileira, Integer coluna,
        OffsetDateTime validadoEm, UUID validadoPorId) {

    public static ValidacaoResultado valido(UUID ingressoId, Integer fileira, Integer coluna,
            OffsetDateTime validadoEm, UUID validadoPorId) {
        return new ValidacaoResultado(ResultadoValidacao.VALIDO, ingressoId, fileira, coluna, validadoEm,
                validadoPorId);
    }

    public static ValidacaoResultado jaUtilizado(Ingresso ingresso) {
        return new ValidacaoResultado(ResultadoValidacao.JA_UTILIZADO, ingresso.getId(), ingresso.getFileira(),
                ingresso.getColuna(), ingresso.getValidadoEm(), ingresso.getValidadoPorId());
    }

    public static ValidacaoResultado eventoErrado(Ingresso ingresso) {
        return new ValidacaoResultado(ResultadoValidacao.EVENTO_ERRADO, ingresso.getId(), ingresso.getFileira(),
                ingresso.getColuna(), null, null);
    }

    public static ValidacaoResultado invalido(Ingresso ingresso) {
        return new ValidacaoResultado(ResultadoValidacao.INVALIDO, ingresso.getId(), ingresso.getFileira(),
                ingresso.getColuna(), null, null);
    }

    public static ValidacaoResultado expirado(Ingresso ingresso) {
        return new ValidacaoResultado(ResultadoValidacao.EXPIRADO, ingresso.getId(), ingresso.getFileira(),
                ingresso.getColuna(), null, null);
    }
}
