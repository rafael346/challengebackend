package com.verzel.challengebackend.service.payment;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class StripePagamentoGateway implements PagamentoGateway {

    private final String secretKey;

    public StripePagamentoGateway(@Value("${stripe.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public Mono<PagamentoResultado> confirmarPagamento(String paymentMethodId, BigDecimal valor, String reservaId) {
        return Mono.fromCallable(() -> criarEConfirmar(paymentMethodId, valor, reservaId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private PagamentoResultado criarEConfirmar(String paymentMethodId, BigDecimal valor, String reservaId)
            throws StripeException {
        com.stripe.Stripe.apiKey = secretKey;
        long valorEmCentavos = valor.movePointRight(2).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(valorEmCentavos)
                .setCurrency("brl")
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .putMetadata("reservaId", reservaId)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            if ("succeeded".equals(intent.getStatus())) {
                return PagamentoResultado.sucesso(intent.getId());
            }
            return PagamentoResultado.recusado("Pagamento não confirmado: status " + intent.getStatus());
        } catch (CardException e) {
            return PagamentoResultado.recusado(e.getMessage());
        }
    }
}
