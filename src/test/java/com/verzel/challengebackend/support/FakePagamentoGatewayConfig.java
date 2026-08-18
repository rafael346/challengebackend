package com.verzel.challengebackend.support;

import com.verzel.challengebackend.service.payment.PagamentoGateway;
import com.verzel.challengebackend.service.payment.PagamentoResultado;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@TestConfiguration(proxyBeanMethods = false)
public class FakePagamentoGatewayConfig {

    public static final String PAYMENT_METHOD_SUCESSO = "pm_sucesso_teste";
    public static final String PAYMENT_METHOD_RECUSADO = "pm_recusado_teste";

    @Bean
    @Primary
    public PagamentoGateway pagamentoGateway() {
        return (paymentMethodId, valor, reservaId) -> {
            if (PAYMENT_METHOD_RECUSADO.equals(paymentMethodId)) {
                return Mono.just(PagamentoResultado.recusado("Cartão recusado (fake)"));
            }
            return Mono.just(PagamentoResultado.sucesso("pi_fake_" + reservaId));
        };
    }
}
