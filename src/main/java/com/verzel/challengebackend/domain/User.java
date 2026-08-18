package com.verzel.challengebackend.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Implements {@link Persistable} because {@code id} is a client-assigned UUID, not a
 * database-generated one. Without this, Spring Data R2DBC sees a non-null id on a brand new
 * instance and assumes it already exists, issuing an UPDATE (which matches zero rows and
 * silently no-ops) instead of an INSERT.
 */
@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String nome;
    private final String sobrenome;
    private final TipoAcesso tipoAcesso;
    private final String email;
    private final String senha;
    private final OffsetDateTime createdAt;

    @Transient
    private final boolean isNew;

    public User(UUID id, String nome, String sobrenome, TipoAcesso tipoAcesso, String email, String senha,
            OffsetDateTime createdAt) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.tipoAcesso = tipoAcesso;
        this.email = email;
        this.senha = senha;
        this.createdAt = createdAt;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public TipoAcesso getTipoAcesso() {
        return tipoAcesso;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
