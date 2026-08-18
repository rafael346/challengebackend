package com.verzel.challengebackend.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;


@Table("ingressos")
public class Ingresso implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final UUID eventoId;
    private final UUID reservaId;
    private final UUID compradorId;
    private final Integer fileira;
    private final Integer coluna;
    private final BigDecimal preco;
    private final StatusIngresso status;
    private final OffsetDateTime expiraEm;
    private final OffsetDateTime validoAte;
    private final String stripePaymentIntentId;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    public Ingresso(UUID id, UUID eventoId, UUID reservaId, UUID compradorId, Integer fileira, Integer coluna,
            BigDecimal preco, StatusIngresso status, OffsetDateTime expiraEm, OffsetDateTime validoAte,
            String stripePaymentIntentId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.eventoId = eventoId;
        this.reservaId = reservaId;
        this.compradorId = compradorId;
        this.fileira = fileira;
        this.coluna = coluna;
        this.preco = preco;
        this.status = status;
        this.expiraEm = expiraEm;
        this.validoAte = validoAte;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Ingresso marcarComoNovo() {
        this.isNew = true;
        return this;
    }

    public Ingresso vendido(String stripePaymentIntentId, OffsetDateTime agora) {
        return new Ingresso(id, eventoId, reservaId, compradorId, fileira, coluna, preco, StatusIngresso.VENDIDO,
                expiraEm, validoAte, stripePaymentIntentId, createdAt, agora);
    }

 
    public Ingresso cancelado(OffsetDateTime agora) {
        return new Ingresso(id, eventoId, reservaId, compradorId, fileira, coluna, preco, StatusIngresso.CANCELADA,
                expiraEm, validoAte, stripePaymentIntentId, createdAt, agora);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public UUID getCompradorId() {
        return compradorId;
    }

    public Integer getFileira() {
        return fileira;
    }

    public Integer getColuna() {
        return coluna;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public StatusIngresso getStatus() {
        return status;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    public OffsetDateTime getValidoAte() {
        return validoAte;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
