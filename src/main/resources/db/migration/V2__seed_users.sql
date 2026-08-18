-- Senha em texto plano para todos os usuários de teste: senha123
-- Hash gerado com BCrypt, custo 10.
INSERT INTO users (id, nome, sobrenome, tipo_acesso, email, senha, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Ana', 'Organizadora', 'ORGANIZADOR', 'organizador@verzel.com', '$2y$10$GLwz83ZYBEVwxdfv0t0EXeKucwPcuu1VYSONkeZnHmvOAoz69vQ62', now()),
    ('22222222-2222-2222-2222-222222222222', 'Carlos', 'Cliente', 'CLIENTE', 'cliente@verzel.com', '$2y$10$GLwz83ZYBEVwxdfv0t0EXeKucwPcuu1VYSONkeZnHmvOAoz69vQ62', now()),
    ('33333333-3333-3333-3333-333333333333', 'Paula', 'Portaria', 'PORTARIA', 'portaria@verzel.com', '$2y$10$GLwz83ZYBEVwxdfv0t0EXeKucwPcuu1VYSONkeZnHmvOAoz69vQ62', now());
