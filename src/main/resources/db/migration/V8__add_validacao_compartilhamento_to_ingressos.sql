ALTER TABLE ingressos DROP CONSTRAINT ingressos_status_check;
ALTER TABLE ingressos ADD CONSTRAINT ingressos_status_check
    CHECK (status IN ('RESERVADO', 'VENDIDO', 'CANCELADA', 'EXPIRADA', 'USADO'));

ALTER TABLE ingressos ADD COLUMN validado_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE ingressos ADD COLUMN validado_por_id UUID REFERENCES users (id);
ALTER TABLE ingressos ADD COLUMN compartilhamento_token UUID UNIQUE;
