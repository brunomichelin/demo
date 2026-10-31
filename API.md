# API do Sistema de Gestão de Obras

Os endpoints REST novos seguem o prefixo `/api/v1` e exigem JWT, exceto
`POST /auth/login`, `POST /auth/register` e `POST /auth/refresh`. O JWT de acesso
pode ser enviado como `Authorization: Bearer <token>` ou pelo cookie `access_token`.
O login também define um cookie `refresh_token`, restrito a `/auth/refresh`.
Configure `API_SECURITY_TOKEN_SECRET` com pelo menos 32 bytes antes de autenticar.
Para provisionar um usuário inicial em container, configure as variáveis de
ambiente `APP_BOOTSTRAP_USER_EMAIL`, `APP_BOOTSTRAP_USER_NAME`,
`APP_BOOTSTRAP_USER_PASSWORD_HASH` (hash BCrypt), e `APP_BOOTSTRAP_USER_ROLE`
(por exemplo, `ADMIN`) no runtime. `APP_BOOTSTRAP_USER_PHONE` é opcional.
O provisionamento ocorre na inicialização, somente se o e-mail ainda não
existir, e não altera contas existentes. Não passe o hash como argumento de
build nem o grave na imagem Docker.

Listagens aceitam a paginação do Spring Data (`page`, `size`, `sort`). Relatórios
aceitam `formato=json|pdf|xlsx`; para PDF/XLSX, a resposta é um arquivo para download.

## Endpoints principais

| Método | Endpoint | Papéis principais |
|---|---|---|
| POST | `/auth/login`, `/auth/refresh`, `/auth/register` | Público |
| GET/POST/PUT/DELETE | `/api/v1/usuarios[/{id}]` | ADMIN |
| PATCH | `/api/v1/usuarios/{id}/aprovar` | ADMIN |
| GET/POST/PUT/DELETE | `/api/v1/insumos[/{id}]` | Leitura: equipe; escrita: ADMIN/ORCAMENTISTA |
| GET/POST/PUT/DELETE | `/api/v1/fornecedores[/{id}]` | Leitura: equipe; escrita: ADMIN/ORCAMENTISTA/FINANCEIRO |
| GET/POST/PUT/DELETE | `/api/v1/obras[/{id}]` | Leitura: equipe vinculada; escrita: ADMIN/GESTOR |
| GET/POST | `/api/v1/obras/{id}/etapas` | Leitura: equipe vinculada; escrita: ADMIN/GESTOR |
| GET/PUT/DELETE/PATCH | `/api/v1/etapas/{id}` e `/api/v1/etapas/{id}/ordem` | ADMIN/GESTOR para escrita |
| GET/POST | `/api/v1/etapas/{id}/tarefas` | Leitura: equipe (inclui FINANCEIRO); escrita: ADMIN/GESTOR/MESTRE_OBRA/TECNICO_OBRA |
| PUT/PATCH/DELETE | `/api/v1/tarefas/{id}` e `/api/v1/tarefas/{id}/status` | Escrita: equipe operacional/gestão |
| GET/POST | `/api/v1/obras/{id}/orcamento/versoes` | ADMIN/GESTOR/ORCAMENTISTA |
| POST | `/api/v1/etapas/{id}/itens-orcamento?versaoId={id}` | ADMIN/GESTOR/ORCAMENTISTA |
| POST/GET | `/api/v1/etapas/{id}/lancamentos` | Escrita: ADMIN/MESTRE_OBRA/TECNICO_OBRA/FINANCEIRO |
| PATCH | `/api/v1/lancamentos/{id}/validacao` | ADMIN/FINANCEIRO |
| POST/PATCH | `/api/v1/etapas/{id}/entrega` e `/aprovacao` | Registro: equipe operacional; aprovação: ADMIN/GESTOR |
| GET | `/api/v1/obras/{id}/relatorios/{custo-por-etapa,insumos-por-etapa,tarefas-por-etapa,prazos-entrega}` | Conforme perfil e obra |
| GET | `/api/v1/auditoria` | ADMIN |
| GET/POST/PUT/DELETE | `/api/v1/livros[/{id}]` | Usuário autenticado; cada usuário acessa apenas sua própria biblioteca |
| PATCH | `/api/v1/livros/{id}/status` | Atualiza o status de leitura na biblioteca do usuário autenticado |
| POST | `/api/v1/livros/catalogo` | Usuário autenticado; consulta o catálogo ISBN da CBL |

O preço unitário de um item de orçamento usa o preço de referência do insumo se
`valorUnitario` for omitido. Lançamentos iniciam como `PENDENTE` e só entram no
custo realizado depois de serem marcados `VALIDADO`; podem informar `tarefaId`
opcional, que deve pertencer à etapa do lançamento. Itens e lançamentos exigem
quantidades e valores positivos. Uma versão congelada não aceita novos itens; para
revisões, crie uma nova versão.

Uma entrega aprovada exige que todas as tarefas da etapa estejam concluídas.
O sistema registra auditoria de criação/revisão/congelamento de orçamento,
lançamentos e aprovações de entrega. Exclusões de cadastros existentes são lógicas.
O relatório de custos considera a versão mais recente do orçamento por padrão;
informe `versaoId` para selecionar outra.

## Telas web

- `index.xhtml`: início da aplicação; o atalho para a lista administrativa de obras é exibido somente para ADMIN e GESTOR.
- `gestao.xhtml`: painel inicial com as obras vinculadas ao usuário e atalhos conforme o perfil.
- A navegação global das telas retorna a `index.xhtml` ou `gestao.xhtml`; listas de etapas e tarefas também mantêm o retorno ao próprio módulo.
- `obras.xhtml` e `detailObra.xhtml` são restritas a ADMIN e GESTOR; os módulos operacionais retornam ao painel, evitando exigir acesso à tela de detalhes da obra.
- `cadastro.xhtml`: permite solicitar um papel; novos cadastros ficam pendentes e não podem autenticar até a aprovação de um ADMIN em `usuarios.xhtml`.
- `orcamento.xhtml?obraId={id}`: versões, itens por etapa e congelamento do orçamento.
- `custos.xhtml?obraId={id}`: registro e validação de lançamentos; aceita também `etapaId`.
- `entregas.xhtml?obraId={id}`: registro e decisão das entregas; aceita também `etapaId`.
- `relatorios.xhtml?obraId={id}`: visualização JSON e exportação PDF/XLSX.
- `desempenho.xhtml?obraId={id}`: gráficos de avanço e cronograma, além de orçamento versus custos validados por etapa e tarefa; usa a versão mais recente do orçamento.
- `usuarios.xhtml` e `auditoria.xhtml`: administração, disponíveis somente para ADMIN.
- `livros.xhtml`: busca no catálogo da CBL e gerenciamento da biblioteca pessoal (lidos e desejados).

As telas operacionais usam os endpoints protegidos de `/api/v1` e, portanto,
respeitam os papéis e os vínculos do usuário autenticado.
