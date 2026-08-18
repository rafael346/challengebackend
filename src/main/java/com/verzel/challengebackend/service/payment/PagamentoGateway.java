package com.verzel.challengebackend.service.payment;

import java.math.BigDecimal;
import reactor.core.publisher.Mono;

/** Abstrai o provedor de pagamento — o domínio não depende do SDK do Stripe, e os testes
 * usam um fake (ver FakePagamentoGatewayConfig) em vez de chamar o Stripe de verdade. */
public interface PagamentoGateway {
    Mono<PagamentoResultado> confirmarPagamento(String paymentMethodId, BigDecimal valor, String reservaId);
}
