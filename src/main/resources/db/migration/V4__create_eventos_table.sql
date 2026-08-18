CREATE TABLE eventos (
    id UUID PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('FILME', 'SHOW', 'TEATRO')),
    descricao TEXT NOT NULL,
    local VARCHAR(255) NOT NULL,
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    forma_venda VARCHAR(10) NOT NULL CHECK (forma_venda IN ('PISTA', 'ASSENTOS')),
    fileiras INTEGER,
    colunas INTEGER,
    quantidade_total_ingressos INTEGER NOT NULL CHECK (quantidade_total_ingressos > 0),
    preco NUMERIC(10, 2) NOT NULL CHECK (preco > 0),
    organizer_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_forma_venda_consistencia CHECK (
        (forma_venda = 'ASSENTOS' AND fileiras IS NOT NULL AND colunas IS NOT NULL
            AND fileiras > 0 AND colunas > 0
            AND quantidade_total_ingressos = fileiras * colunas)
        OR
        (forma_venda = 'PISTA' AND fileiras IS NULL AND colunas IS NULL)
    )
);
