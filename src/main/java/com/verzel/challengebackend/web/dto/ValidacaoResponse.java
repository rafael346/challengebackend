package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.service.ResultadoValidacao;
import com.verzel.challengebackend.service.ValidacaoResultado;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ValidacaoResponse(ResultadoValidacao resultado, UUID ingressoId, Integer fileira, Integer coluna,
        OffsetDateTime validadoEm, UUID validadoPorId) {

    public static ValidacaoResponse from(ValidacaoResultado resultado) {
        return new ValidacaoResponse(resultado.resultado(), resultado.ingressoId(), resultado.fileira(),
                resultado.coluna(), resultado.validadoEm(), resultado.validadoPorId());
    }
}
