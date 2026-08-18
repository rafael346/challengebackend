package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.AssentoIndisponivelException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.PagamentoRecusadoException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.service.exception.ReservaAccessDeniedException;
import com.verzel.challengebackend.service.exception.ReservaExpiradaException;
import com.verzel.challengebackend.service.exception.ReservaNotFoundException;
import com.verzel.challengebackend.service.payment.PagamentoGateway;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ConfirmarReservaRequest;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReservaService {

    private final EventoRepository eventoRepository;
    private final IngressoRepository ingressoRepository;
    private final PagamentoGateway pagamentoGateway;
    private final long holdDurationSeconds;

    public ReservaService(EventoRepository eventoRepository, IngressoRepository ingressoRepository,
            PagamentoGateway pagamentoGateway, @Value("${reserva.hold-duration-seconds}") long holdDurationSeconds) {
        this.eventoRepository = eventoRepository;
        this.ingressoRepository = ingressoRepository;
        this.pagamentoGateway = pagamentoGateway;
        this.holdDurationSeconds = holdDurationSeconds;
    }

    /**
     * Cria uma reserva (hold) de um ou mais ingressos. Trava a linha do evento
     * ({@code buscarComLockPorId}), o que serializa toda tentativa concorrente de reserva
     * sobre aquele evento; libera holds vencidos; e só então valida e insere os novos
     * ingressos — tudo em uma única transação.
     */
    @Transactional
    public Mono<List<Ingresso>> criar(UUID eventoId, ReservaRequest request, UUID compradorId) {
        return eventoRepository.buscarComLockPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException()))
                .flatMap(evento -> ingressoRepository.expirarReservasVencidas(eventoId)
                        .then(Mono.defer(() -> reservarItens(evento, request, compradorId))));
    }

    private Mono<List<Ingresso>> reservarItens(Evento evento, ReservaRequest request, UUID compradorId) {
        if (evento.getFormaVenda() == FormaVenda.ASSENTOS) {
            return reservarAssentos(evento, request, compradorId);
        }
        return reservarPista(evento, request, compradorId);
    }

    private Mono<List<Ingresso>> reservarAssentos(Evento evento, ReservaRequest request, UUID compradorId) {
        if (request.assentos() == null || request.assentos().isEmpty()) {
            return Mono.error(new InvalidReservaException("assentos é obrigatório para forma de venda ASSENTOS"));
        }
        if (request.quantidade() != null) {
            return Mono.error(new InvalidReservaException("quantidade não se aplica para forma de venda ASSENTOS"));
        }
        for (AssentoRequest assento : request.assentos()) {
            if (assento.fileira() < 1 || assento.fileira() > evento.getFileiras() || assento.coluna() < 1
                    || assento.coluna() > evento.getColunas()) {
                return Mono.error(new InvalidReservaException(
                        "assento fileira=%d coluna=%d fora do grid do evento".formatted(assento.fileira(),
                                assento.coluna())));
            }
        }
        UUID reservaId = UUID.randomUUID();
        return Flux.fromIterable(request.assentos())
                .concatMap(assento -> ingressoRepository
                        .buscarAssentoAtivo(evento.getId(), assento.fileira(), assento.coluna())
                        .flatMap(ocupado -> Mono.<AssentoRequest>error(new AssentoIndisponivelException()))
                        .switchIfEmpty(Mono.just(assento)))
                .collectList()
                .flatMap(assentosLivres -> ingressoRepository
                        .saveAll(novosIngressosAssentos(evento, compradorId, reservaId, assentosLivres))
                        .collectList());
    }

    private Mono<List<Ingresso>> reservarPista(Evento evento, ReservaRequest request, UUID compradorId) {
        if (request.quantidade() == null || request.quantidade() < 1) {
            return Mono.error(new InvalidReservaException(
                    "quantidade é obrigatória e deve ser positiva para forma de venda PISTA"));
        }
        if (request.assentos() != null && !request.assentos().isEmpty()) {
            return Mono.error(new InvalidReservaException("assentos não se aplica para forma de venda PISTA"));
        }
        UUID reservaId = UUID.randomUUID();
        return ingressoRepository.contarAtivosPorEvento(evento.getId())
                .flatMap(ativos -> {
                    if (ativos + request.quantidade() > evento.getQuantidadeTotalIngressos()) {
                        return Mono.error(new QuantidadeIndisponivelException());
                    }
                    return ingressoRepository
                            .saveAll(novosIngressosPista(evento, compradorId, reservaId, request.quantidade()))
                            .collectList();
                });
    }

    private List<Ingresso> novosIngressosAssentos(Evento evento, UUID compradorId, UUID reservaId,
            List<AssentoRequest> assentos) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime expiraEm = agora.plusSeconds(holdDurationSeconds);
        OffsetDateTime validoAte = evento.getDataHora().plusHours(1);
        return assentos.stream()
                .map(assento -> new Ingresso(UUID.randomUUID(), evento.getId(), reservaId, compradorId,
                        assento.fileira(), assento.coluna(), evento.getPreco(), StatusIngresso.RESERVADO, expiraEm,
                        validoAte, null, agora, agora)
                        .marcarComoNovo())
                .toList();
    }

    private List<Ingresso> novosIngressosPista(Evento evento, UUID compradorId, UUID reservaId, int quantidade) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime expiraEm = agora.plusSeconds(holdDurationSeconds);
        OffsetDateTime validoAte = evento.getDataHora().plusHours(1);
        return IntStream.range(0, quantidade)
                .mapToObj(i -> new Ingresso(UUID.randomUUID(), evento.getId(), reservaId, compradorId, null, null,
                        evento.getPreco(), StatusIngresso.RESERVADO, expiraEm, validoAte, null, agora, agora)
                        .marcarComoNovo())
                .toList();
    }

    /**
     * Confirma o pagamento de uma reserva ativa. Em caso de recusa, a reserva permanece
     * {@code RESERVADO} — o comprador pode tentar outro cartão até os 10 minutos acabarem
     * (decisão de produto: falha de pagamento não libera o ingresso na hora).
     */
    @Transactional
    public Mono<List<Ingresso>> confirmar(UUID reservaId, ConfirmarReservaRequest request, UUID compradorId) {
        return ingressoRepository.findByReservaId(reservaId).collectList()
                .flatMap(itens -> validarPosseEAtivos(itens, compradorId).then(Mono.defer(() -> {
                    BigDecimal valorTotal = itens.stream().map(Ingresso::getPreco).reduce(BigDecimal.ZERO,
                            BigDecimal::add);
                    return pagamentoGateway.confirmarPagamento(request.paymentMethodId(), valorTotal,
                                    reservaId.toString())
                            .flatMap(resultado -> {
                                if (!resultado.sucesso()) {
                                    return Mono.error(new PagamentoRecusadoException(resultado.motivoRecusa()));
                                }
                                OffsetDateTime agora = OffsetDateTime.now();
                                List<Ingresso> vendidos = itens.stream()
                                        .map(item -> item.vendido(resultado.paymentIntentId(), agora))
                                        .toList();
                                return ingressoRepository.saveAll(vendidos).collectList();
                            });
                })));
    }

    /** Libera uma reserva ativa antes da expiração natural. */
    @Transactional
    public Mono<Void> cancelar(UUID reservaId, UUID compradorId) {
        return ingressoRepository.findByReservaId(reservaId).collectList()
                .flatMap(itens -> validarPosseEAtivos(itens, compradorId).then(Mono.defer(() -> {
                    OffsetDateTime agora = OffsetDateTime.now();
                    List<Ingresso> cancelados = itens.stream().map(item -> item.cancelado(agora)).toList();
                    return ingressoRepository.saveAll(cancelados).then();
                })));
    }

    private Mono<Void> validarPosseEAtivos(List<Ingresso> itens, UUID compradorId) {
        if (itens.isEmpty()) {
            return Mono.error(new ReservaNotFoundException());
        }
        OffsetDateTime agora = OffsetDateTime.now();
        for (Ingresso item : itens) {
            if (!item.getCompradorId().equals(compradorId)) {
                return Mono.error(new ReservaAccessDeniedException());
            }
            if (item.getStatus() != StatusIngresso.RESERVADO || item.getExpiraEm().isBefore(agora)) {
                return Mono.error(new ReservaExpiradaException());
            }
        }
        return Mono.empty();
    }

    public Mono<Disponibilidade> disponibilidade(UUID eventoId) {
        return eventoRepository.findById(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException()))
                .flatMap(evento -> {
                    if (evento.getFormaVenda() == FormaVenda.ASSENTOS) {
                        return ingressoRepository.buscarAssentosOcupados(eventoId)
                                .map(i -> new Disponibilidade.Assento(i.getFileira(), i.getColuna()))
                                .collectList()
                                .map(ocupados -> Disponibilidade.paraAssentos(evento.getFileiras(),
                                        evento.getColunas(), ocupados));
                    }
                    return ingressoRepository.contarAtivosPorEvento(eventoId)
                            .map(ativos -> Disponibilidade
                                    .paraPista(evento.getQuantidadeTotalIngressos() - ativos.intValue()));
                });
    }
}
