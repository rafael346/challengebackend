package com.verzel.challengebackend.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("eventos")
public class Evento implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String titulo;
    private final CategoriaEvento categoria;
    private final String descricao;
    private final String local;
    private final OffsetDateTime dataHora;
    private final FormaVenda formaVenda;
    private final Integer fileiras;
    private final Integer colunas;
    private final Integer quantidadeTotalIngressos;
    private final BigDecimal preco;
    private final UUID organizerId;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    public Evento(UUID id, String titulo, CategoriaEvento categoria, String descricao, String local,
            OffsetDateTime dataHora, FormaVenda formaVenda, Integer fileiras, Integer colunas,
            Integer quantidadeTotalIngressos, BigDecimal preco, UUID organizerId, OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.titulo = titulo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.local = local;
        this.dataHora = dataHora;
        this.formaVenda = formaVenda;
        this.fileiras = fileiras;
        this.colunas = colunas;
        this.quantidadeTotalIngressos = quantidadeTotalIngressos;
        this.preco = preco;
        this.organizerId = organizerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Evento marcarComoNovo() {
        this.isNew = true;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getTitulo() {
        return titulo;
    }

    public CategoriaEvento getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLocal() {
        return local;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public FormaVenda getFormaVenda() {
        return formaVenda;
    }

    public Integer getFileiras() {
        return fileiras;
    }

    public Integer getColunas() {
        return colunas;
    }

    public Integer getQuantidadeTotalIngressos() {
        return quantidadeTotalIngressos;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
