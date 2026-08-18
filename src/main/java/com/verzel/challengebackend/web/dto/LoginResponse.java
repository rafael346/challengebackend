package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.TipoAcesso;

public record LoginResponse(String token, TipoAcesso tipoAcesso) {
}
