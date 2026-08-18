package com.verzel.challengebackend.service.payment;

public record PagamentoResultado(boolean sucesso, String paymentIntentId, String motivoRecusa) {

    public static PagamentoResultado sucesso(String paymentIntentId) {
        return new PagamentoResultado(true, paymentIntentId, null);
    }

    public static PagamentoResultado recusado(String motivo) {
        return new PagamentoResultado(false, null, motivo);
    }
}
