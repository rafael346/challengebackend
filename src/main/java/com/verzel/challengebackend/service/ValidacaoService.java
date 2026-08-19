package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.IngressoNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class ValidacaoService {

    private final IngressoRepository ingressoRepository;

    public ValidacaoService(IngressoRepository ingressoRepository) {
        this.ingressoRepository = ingressoRepository;
    }

    /**
     * Valida o check-in de um ingresso na portaria. A ordem das checagens define qual
     * resultado vence quando mais de uma condição se aplica: evento errado > já utilizado >
     * inválido > expirado > válido. A garantia de não-validar-duas-vezes vem de um único
     * UPDATE condicional e atômico (IngressoRepository.validarUso), não de "ler depois
     * escrever" — evita a janela de corrida entre duas portarias escaneando o mesmo ingresso
     * ao mesmo tempo.
     */
    @Transactional
    public Mono<ValidacaoResultado> validar(UUID eventoId, UUID ingressoId, UUID portariaId) {
        return ingressoRepository.findById(ingressoId)
                .switchIfEmpty(Mono.error(new IngressoNotFoundException()))
                .flatMap(ingresso -> avaliar(ingresso, eventoId, portariaId));
    }

    private Mono<ValidacaoResultado> avaliar(Ingresso ingresso, UUID eventoId, UUID portariaId) {
        if (!ingresso.getEventoId().equals(eventoId)) {
            return Mono.just(ValidacaoResultado.eventoErrado(ingresso));
        }
        if (ingresso.getStatus() == StatusIngresso.USADO) {
            return Mono.just(ValidacaoResultado.jaUtilizado(ingresso));
        }
        if (ingresso.getStatus() != StatusIngresso.VENDIDO) {
            return Mono.just(ValidacaoResultado.invalido(ingresso));
        }
        if (OffsetDateTime.now().isAfter(ingresso.getValidoAte())) {
            return Mono.just(ValidacaoResultado.expirado(ingresso));
        }
        return tentarMarcarComoUsado(ingresso, portariaId);
    }

    private Mono<ValidacaoResultado> tentarMarcarComoUsado(Ingresso ingresso, UUID portariaId) {
        OffsetDateTime agora = OffsetDateTime.now();
        return ingressoRepository.validarUso(ingresso.getId(), portariaId, agora)
                .flatMap(linhasAfetadas -> {
                    if (linhasAfetadas == 0) {
                        // Perdeu a corrida: outra validação venceu entre a leitura e o update.
                        return ingressoRepository.findById(ingresso.getId())
                                .map(ValidacaoResultado::jaUtilizado);
                    }
                    return Mono.just(ValidacaoResultado.valido(ingresso.getId(), ingresso.getFileira(),
                            ingresso.getColuna(), agora, portariaId));
                });
    }
}
