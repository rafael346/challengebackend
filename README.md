# Como rodar o backend localmente

Guia passo a passo para colocar a API no ar na sua máquina.

## Pré-requisitos

| Ferramenta | Versão | Obrigatório? |
|---|---|---|
| [Java](https://adoptium.net/) | 21 | Sim |
| [Docker](https://www.docker.com/) | qualquer recente | Sim — sobe o Postgres (e é usado pelos testes via Testcontainers) |
| Maven | — | Não — o projeto já traz o Maven Wrapper (`./mvnw`) |

Não precisa instalar Postgres nem Maven manualmente.

## Passo a passo

### 1. Clonar o repositório

```bash
git clone <[url-do-repositorio](https://github.com/rafael346/challengebackend.git)>
cd challengebackend
```

### 2. Subir o banco de dados (Postgres via Docker)

O `compose.yaml` já sobe um Postgres configurado com as credenciais padrão que a aplicação espera:

```bash
docker compose up -d
```

Isso inicia um container `postgres:latest` com o banco `mydatabase`, usuário `myuser` e senha `secret`. As migrations do Flyway (`src/main/resources/db/migration`) rodam automaticamente quando a aplicação subir, criando as tabelas e populando alguns usuários de teste.

> Se preferir usar um Postgres próprio, ajuste as variáveis de ambiente do passo 3 em vez de usar o `compose.yaml`.

### 3. Configurar variáveis de ambiente (opcional em dev)

A aplicação já roda "do jeito que está" com valores padrão pensados para desenvolvimento local (ver `src/main/resources/application.properties`). Você só precisa exportar variáveis quando quiser testar uma integração externa de verdade ou mudar algo do padrão:

| Variável | Padrão (dev) | Para que serve |
|---|---|---|
| `PORT` | `8080` | Porta HTTP da aplicação |
| `DB_HOST` | `localhost` | Host do Postgres |
| `DB_PORT` | `5432` | Porta do Postgres |
| `DB_NAME` | `mydatabase` | Nome do banco |
| `DB_USER` | `myuser` | Usuário do banco |
| `DB_PASSWORD` | `secret` | Senha do banco |
| `JWT_SECRET` | chave de exemplo fixa | Chave HMAC usada para assinar/validar os JWTs |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do token de login |
| `RESERVA_HOLD_DURATION_SECONDS` | `600` | Tempo que uma reserva fica "segurada" antes de expirar |
| `STRIPE_SECRET_KEY` | `sk_test_placeholder` | Chave secreta de teste do Stripe |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origens liberadas no CORS |
| `TMDB_API_KEY` | vazio | Chave da API do TMDB (necessária para a sincronização de filmes funcionar) |
| `TMDB_SYNC_ON_STARTUP` | `true` | Se `true`, roda a sincronização com o TMDB assim que a aplicação sobe |
| `TMDB_SYNC_CRON` | `0 0 3 * * *` | Agendamento (cron) da sincronização diária |

Exemplo de arquivo `.env` (o `.gitignore` já ignora `.env` e `.env.*`):

```bash
JWT_SECRET=uma-chave-bem-grande-e-secreta
STRIPE_SECRET_KEY=sk_test_sua_chave_de_teste_aqui
TMDB_API_KEY=sua_chave_do_tmdb
```

Depois é só exportar antes de subir a aplicação: `export $(cat .env | xargs)` (ou use o suporte a `.env` da sua IDE).

### 4. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Na primeira vez, o Flyway cria o schema e insere os dados de seed automaticamente.

### 5. Conferir se está no ar

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 6. Testar o login com um usuário de teste

O seed (`V2__seed_users.sql` e migrations seguintes) já cria usuários prontos para uso — a senha de todos é `senha123`:

| Email | Papel (`tipoAcesso`) |
|---|---|
| `organizador@verzel.com` | `ORGANIZADOR` — cria/edita/remove eventos |
| `cliente@verzel.com` | `CLIENTE` — reserva, compra e compartilha ingressos |
| `portaria@verzel.com` | `PORTARIA` — valida ingressos na entrada do evento |
| `outro-organizador@verzel.com` | `ORGANIZADOR` |
| `outra-cliente@verzel.com` | `CLIENTE` |

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"organizador@verzel.com","senha":"senha123"}'
```

A resposta traz um `token` JWT — use-o no header `Authorization: Bearer <token>` nas chamadas seguintes.

### 7. Rodar os testes

```bash
./mvnw test
```

Os testes de integração usam [Testcontainers](https://testcontainers.com/) para subir um Postgres real em container — por isso o **Docker precisa estar rodando** mesmo só para testar (não é usado o Postgres do `docker compose up`, o Testcontainers sobe o dele próprio).

## Solução de problemas

- **`Connection refused` ao subir a aplicação** — o Postgres do `docker compose` ainda não está pronto ou não foi iniciado. Rode `docker compose ps` para conferir e `docker compose up -d` novamente.
- **Porta `8080` já em uso** — suba com outra porta: `PORT=8081 ./mvnw spring-boot:run`.
- **Testes falhando com erro de Docker/Testcontainers** — confirme que o Docker Desktop (ou daemon equivalente) está rodando antes de `./mvnw test`.
- **Login retornando 401** — confirme que está usando a senha `senha123` e que o Flyway rodou o seed (`docker compose down -v` e subir de novo apaga e recria o banco do zero, se precisar resetar).
- **Sincronização do TMDB não cria eventos** — sem `TMDB_API_KEY` configurada, a sincronização falha silenciosamente (fica só no log). Isso não impede o resto da aplicação de funcionar.

## Deploy

O projeto já inclui um [`Dockerfile`](Dockerfile) (build multi-stage) e um [`render.yaml`](render.yaml) prontos para deploy no [Render](https://render.com/), com health check em `/actuator/health` e segredos (`JWT_SECRET`, `STRIPE_SECRET_KEY`, `TMDB_API_KEY`, `CORS_ALLOWED_ORIGINS`) configurados como variáveis de ambiente — nunca hardcoded.
