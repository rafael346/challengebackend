CREATE TABLE ingressos (
    id UUID PRIMARY KEY,
    evento_id UUID NOT NULL REFERENCES eventos (id),
    reserva_id UUID NOT NULL,
    comprador_id UUID NOT NULL REFERENCES users (id),
    fileira INTEGER,
    coluna INTEGER,
    preco NUMERIC(10, 2) NOT NULL CHECK (preco > 0),
    status VARCHAR(10) NOT NULL CHECK (status IN ('RESERVADO', 'VENDIDO', 'CANCELADA', 'EXPIRADA')),
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    valido_ate TIMESTAMP WITH TIME ZONE NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_ingressos_assento_ativo ON ingressos (evento_id, fileira, coluna)
    WHERE fileira IS NOT NULL AND coluna IS NOT NULL AND status IN ('RESERVADO', 'VENDIDO');

CREATE INDEX ix_ingressos_reserva_id ON ingressos (reserva_id);
CREATE INDEX ix_ingressos_evento_id_status ON ingressos (evento_id, status);
