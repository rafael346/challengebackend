package com.verzel.challengebackend.web.dto;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EventoRequest(
        @NotBlank(message = "título é obrigatório") String titulo,
        @NotNull(message = "categoria é obrigatória") CategoriaEvento categoria,
        @NotBlank(message = "descrição é obrigatória") String descricao,
        @NotBlank(message = "local é obrigatório") String local,
        @NotNull(message = "data e hora são obrigatórias")
        @Future(message = "deve ser uma data futura") OffsetDateTime dataHora,
        @NotNull(message = "forma de venda é obrigatória") FormaVenda formaVenda,
        @Positive(message = "deve ser positivo") Integer fileiras,
        @Positive(message = "deve ser positivo") Integer colunas,
        @Positive(message = "deve ser positivo") Integer quantidadeTotalIngressos,
        @NotNull(message = "preço é obrigatório")
        @DecimalMin(value = "0.01", message = "deve ser maior que zero") BigDecimal preco) {
}
