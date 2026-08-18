package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.TipoAcesso;
import java.util.UUID;

public record MeResponse(UUID id, String nome, String sobrenome, String email, TipoAcesso tipoAcesso) {
}
