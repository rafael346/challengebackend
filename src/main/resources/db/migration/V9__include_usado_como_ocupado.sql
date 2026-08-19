DROP INDEX ux_ingressos_assento_ativo;
CREATE UNIQUE INDEX ux_ingressos_assento_ativo ON ingressos (evento_id, fileira, coluna)
    WHERE fileira IS NOT NULL AND coluna IS NOT NULL AND status IN ('RESERVADO', 'VENDIDO', 'USADO');
