# Sobre o projeto
Backend de uma plataforma de venda de ingressos: organizadores publicam eventos (shows, peças, sessões de cinema), clientes reservam e compram ingressos, e a portaria valida a entrada no dia do evento. Inclui ainda sincronização automática de filmes em cartaz via TMDB e simulação de pagamento via Stripe (modo teste).

Este documento apresenta a aplicação e explica as principais escolhas técnicas feitas ao longo do desenvolvimento — o "porquê", não só o "o quê". Para instruções de como rodar, veja o [README.md](README.md).

## Comentarios sobre o processo de desenvolvimento
Para a linguagem eu optei por Java por ter trablhado com ela mais recentemente e estar mais fresca na memoria, tambem atuei com node e python  mas em maior volume com Java. Creio que ambas as tecnlogias para um caso real atendam bem se esse fosse um projeto real e com escalabilidade.

Para o banco de dados eu optei por postgree, pois ele é o que atende mais ao escopo do projeto, de acordo com o modelo PACELC (eu na maioria dos casos uso ele para definir qual banco vou usar).

Para o metodo de pagamento, esse foi o que  tive que mudar no meio do processo, inicialmente eu pretendia criar um microsserviço de pagamento usando o ambiente de testes do pagbank, mas tive alguns problemas  e nao consegui pegar api key a tempo entao tive que improvisar com o stripe. Pode parecer ser mais complexo mas como  eu recentemente tinha feito uma integração usando, estava bem fresco na memoria e seria bem mais rapido para fazer.

Sobre a integração com a TMDB e Ticketmaster discovery, eu nao consegui uma api key a tempo da ticketmaster entao so integrei com a TMDB. Essa foi a parte onde fiquei com mais duvidas, mas nao pela questao tecnica, e sim de como usar essa api junto da aplicacao, acabei escolhendo fazer uma populacao dos dados baseado no que vinha da api e permitir que o organizador pudesse editar no front end, nao sei se era esse o objetivo, mas foi o que eu consegui entender com base nos requisitos ( caso nao seja isso seria bem tranquilo de resolver).


Para o deploy eu optei pelo render como plataforma, sei que se tivesse usado Node teria feito mais rapido pela vercel, mas como ja conhecia o Render  consegui realizar o deploy  da api de forma tranquila. Meu plano inicial era fazer o deploy em um EC2 na amazon mas isso poderia gerar cobranças, entao acabei desistindo. Em um caso real, eu colocaria um 
EKS.


Sobre o uso de IA, sim eu acabei usando,mas nao como um vibe coder, que simplesmente  joga um comando pede pra ia nao errar e nao alucinar e aguarda o resultado, eu planejei, dividi os requisitos em pequenas tasks e apos montar todo o fluxo, decisões, etc.
Utilizando um conjunto de skills + Context Enengineering, nao pude explorar tanto a IA pois estava com poucos tokens, entao tive que me virar  e usar algumas implementações que eu ja tinha feito anteriormente baseado em experiencias passadas. Acabei usando os testes unitarios e de integração para validar que estava tudo indo de acordo com o que eu havia planejado e pensado para aplicação.

No mais  pedi para que IA escrevesse um resumo mais detalhado de tudo que foi feito e de algumas escolhas que fiz afim de facilitar o entendimento da estrutura da aplicação.


## Domínio e regras de negócio

Três papéis de usuário (`TipoAcesso`): `ORGANIZADOR`, `CLIENTE`, `PORTARIA`. Um evento (`Evento`) tem uma `FormaVenda`:

- **`ASSENTOS`** — evento com mapa de fileiras/colunas (ex.: cinema, teatro); a reserva escolhe assentos específicos.
- **`PISTA`** — evento sem lugar marcado; a reserva pede uma quantidade.

O fluxo de compra é em duas etapas, com um **hold temporário** no meio:

1. `POST /eventos/{id}/reservas` — reserva os ingressos (status `RESERVADO`) e trava o preço, com expiração configurável (`RESERVA_HOLD_DURATION_SECONDS`, padrão 10 min).
2. `POST /reservas/{id}/confirmar` — cobra no Stripe e, se aprovado, marca os ingressos como `VENDIDO`. Se a reserva expirar antes da confirmação, os ingressos voltam a ficar disponíveis.

Ingressos vendidos podem ser **compartilhados** (link público, sem exigir login de quem recebe) e são **validados** na portaria, virando `USADO`.


## Arquitetura

Camadas convencionais, separadas por pacote:

```
web/            Controllers REST + DTOs + tratamento global de erros
service/        Regras de negócio (um service por caso de uso/agregado)
service/payment Gateway de pagamento, isolado por interface
service/tmdb    Cliente HTTP do TMDB + serviço de sincronização
repository/     Acesso a dados (R2DBC)
domain/         Entidades e enums
security/       JWT (geração, parsing, filtro reativo) e configuração de segurança
config/         Configurações transversais (CORS, etc.)
```

Os services não conhecem Spring MVC nem detalhes de transporte; os controllers são finos (extraem o usuário autenticado do `SecurityContext` reativo e delegam). Erros de negócio são exceções de domínio (`EventoNotFoundException`, `PagamentoRecusadoException`, …), mapeadas para HTTP no `GlobalExceptionHandler` — assim o corpo de erro é padronizado (`ErrorResponse`) em toda a API.

## Decisões de design que valem explicação

**Concorrência na reserva de ingressos.** `ReservaService.criar` trava a linha do evento (`buscarComLockPorId`) antes de liberar holds vencidos e inserir os novos ingressos, tudo em uma única transação. Isso serializa toda tentativa concorrente de reserva sobre o *mesmo evento* — evita duas pessoas reservarem o mesmo assento ao mesmo tempo — sem precisar de lock otimista nem retry.

**Concorrência na validação de portaria.** Diferente da reserva, aqui não se usa lock de linha: `ValidacaoService` faz um **UPDATE condicional atômico** (`validarUso`, que só marca `USADO` se o status ainda for `VENDIDO`) em vez de "ler, checar em memória, depois escrever". Isso fecha a janela de corrida entre duas portarias escaneando o mesmo ingresso ao mesmo tempo — quem perde a corrida recebe "já utilizado", não um estado inconsistente.

**Falha de pagamento não libera o ingresso na hora.** Se o Stripe recusa o cartão, a reserva continua `RESERVADO` — o comprador pode tentar outro cartão até o hold expirar. Decisão de produto deliberada: evita perder o lugar por causa de uma tentativa de pagamento ruim.

**Compartilhamento de ingresso é idempotente e sem revogação.** Gerar o link duas vezes devolve o mesmo token, em vez de invalidar o link já em circulação. Não existe endpoint para revogar um link compartilhado — trade-off simples aceito para o escopo atual (ver "possíveis evoluções" abaixo).

**Sincronização do TMDB é resiliente a falha parcial.** `TmdbSyncService` processa filme a filme; erro em um item vira log e contador de erro, não aborta os demais. Filmes já sincronizados (por `tmdbId`) são pulados, então rodar a sincronização de novo (seja no cron diário ou no boot) é seguro. Quando a API do TMDB está indisponível ou sem chave configurada, a falha é engolida (log de warning) — a aplicação nunca deixa de subir por causa dessa integração externa.

**Segurança declarativa por rota.** `SecurityConfig` mapeia cada rota para um papel exigido (ex.: só `ORGANIZADOR` cria evento, só `CLIENTE` reserva, só `PORTARIA` valida) direto na cadeia de filtros do WebFlux, em vez de checagem manual dentro de cada controller. Logout é suportado com uma tabela de tokens revogados (`revoked_tokens`), já que JWT não tem estado no servidor por padrão.

**Configuração via variável de ambiente, sem segredo hardcoded.** Todos os valores sensíveis (`JWT_SECRET`, `STRIPE_SECRET_KEY`, `TMDB_API_KEY`, credenciais de banco) têm um default só para desenvolvimento local em `application.properties`, e são injetados por env var em produção (ver `render.yaml`).

## Tratamento de erros

Erros de negócio viram códigos HTTP semânticos, não `500` genérico: `404` (recurso não encontrado), `403` (sem permissão), `409` (conflito — assento ocupado, quantidade indisponível), `410` (reserva expirada), `402` (pagamento recusado), `400` (validação de entrada). Todo erro segue o mesmo formato (`ErrorResponse`: status, tipo, mensagem, path), o que facilita o consumo por qualquer frontend.

## Testes

Testes de integração sobem um Postgres real via Testcontainers (não mocks/H2), o que permite testar diretamente os pontos de concorrência descritos acima — por exemplo, o teste de validação de portaria dispara validações simultâneas contra o mesmo ingresso para provar que não há double check-in.

## Possíveis evoluções (algumas ideias que eu teria implementado mas ficaram pelo caminho)

- Paginação e filtros em `GET /eventos` (hoje lista tudo).
- Refresh token / renovação de sessão (hoje o JWT expira e exige novo login).
- Endpoint para revogar um link de compartilhamento já gerado.
- Integração de pagamento separada da aplicação em um microsserviço.
- Idempotency key na confirmação de pagamento (hoje uma dupla submissão da confirmação depende só do estado `RESERVADO` do ingresso para evitar cobrança duplicada).
