package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.service.exception.EventoAccessDeniedException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidEventoException;
import com.verzel.challengebackend.web.dto.EventoRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public Mono<Evento> criar(EventoRequest request, UUID organizerId) {
        return Mono.defer(() -> {
            int quantidadeTotalIngressos = calcularQuantidadeTotalIngressos(request);
            OffsetDateTime agora = OffsetDateTime.now();
            Evento evento = new Evento(UUID.randomUUID(), request.titulo(), request.categoria(), request.descricao(),
                    request.local(), request.dataHora(), request.formaVenda(), fileirasOuNulo(request),
                    colunasOuNulo(request), quantidadeTotalIngressos, request.preco(), organizerId, agora, agora)
                    .marcarComoNovo();
            return eventoRepository.save(evento);
        });
    }

    public Mono<Evento> buscarPorId(UUID id) {
        return eventoRepository.findById(id)
                .switchIfEmpty(Mono.error(new EventoNotFoundException()));
    }

    public reactor.core.publisher.Flux<Evento> listar() {
        return eventoRepository.findAll();
    }

    public Mono<Evento> editar(UUID id, EventoRequest request, UUID organizerId) {
        return buscarPorId(id)
                .flatMap(existente -> {
                    if (!existente.getOrganizerId().equals(organizerId)) {
                        return Mono.error(new EventoAccessDeniedException());
                    }
                    int quantidadeTotalIngressos = calcularQuantidadeTotalIngressos(request);
                    Evento atualizado = new Evento(id, request.titulo(), request.categoria(), request.descricao(),
                            request.local(), request.dataHora(), request.formaVenda(), fileirasOuNulo(request),
                            colunasOuNulo(request), quantidadeTotalIngressos, request.preco(), organizerId,
                            existente.getCreatedAt(), OffsetDateTime.now());
                    return eventoRepository.save(atualizado);
                });
    }

    public Mono<Void> remover(UUID id, UUID organizerId) {
        return buscarPorId(id)
                .flatMap(existente -> {
                    if (!existente.getOrganizerId().equals(organizerId)) {
                        return Mono.error(new EventoAccessDeniedException());
                    }
                    return eventoRepository.delete(existente);
                });
    }

    private int calcularQuantidadeTotalIngressos(EventoRequest request) {
        if (request.formaVenda() == FormaVenda.ASSENTOS) {
            if (request.fileiras() == null || request.colunas() == null) {
                throw new InvalidEventoException(
                        "fileiras e colunas são obrigatórios para forma de venda ASSENTOS");
            }
            return request.fileiras() * request.colunas();
        }
        if (request.fileiras() != null || request.colunas() != null) {
            throw new InvalidEventoException("fileiras e colunas não se aplicam para forma de venda PISTA");
        }
        if (request.quantidadeTotalIngressos() == null) {
            throw new InvalidEventoException("quantidadeTotalIngressos é obrigatório para forma de venda PISTA");
        }
        return request.quantidadeTotalIngressos();
    }

    private Integer fileirasOuNulo(EventoRequest request) {
        return request.formaVenda() == FormaVenda.ASSENTOS ? request.fileiras() : null;
    }

    private Integer colunasOuNulo(EventoRequest request) {
        return request.formaVenda() == FormaVenda.ASSENTOS ? request.colunas() : null;
    }
}
