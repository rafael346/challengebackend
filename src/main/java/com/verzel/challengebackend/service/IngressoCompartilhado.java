package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.Ingresso;

/** Read model combinando o ingresso e o evento associado, para a página pública de
 * compartilhamento (ver CompartilhamentoService.buscarPorToken). */
public record IngressoCompartilhado(Ingresso ingresso, Evento evento) {
}
