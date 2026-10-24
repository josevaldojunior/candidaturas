# Candidaturas

Sistema web para acompanhar candidaturas a vagas de emprego, com foco em vagas de desenvolvedor Java remotas.
Tem CRUD com filtros, quadro Kanban, dashboard com funil, follow-ups, marcação automática de "sem retorno",
exportação/importação de CSV e uma integração para o agente que registra candidaturas automaticamente.

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 3 (Web, Data JPA, Validation, Actuator), Flyway, springdoc-openapi, Commons CSV |
| Banco | PostgreSQL 16 (Docker) · H2 em arquivo no perfil `dev` · H2 em memória nos testes |
| Frontend | Angular 18 (componentes standalone), Angular Material, CDK drag-drop |
| Infra | Docker Compose, GitHub Actions (CI do backend e do frontend) |

## Como rodar

### Com Docker Compose (tudo com um comando)

Pré-requisito: Docker Desktop rodando.

```bash
docker compose up -d --build
```

| Endereço | O que é |
|---|---|
| http://localhost:4200 | Aplicação (frontend) |
| http://localhost:8080/swagger-ui.html | Documentação da API (Swagger) |
| http://localhost:8080/actuator/health | Saúde da API (deve responder `UP`) |

Os dados ficam no volume `candidaturas_postgres-data`. Para parar: `docker compose down`. Para apagar também os dados: `docker compose down -v`.

Variáveis opcionais (copie `.env.example` para `.env`):

| Variável | Padrão | Uso |
|---|---|---|
| `DB_PASSWORD` | `candidaturas` | Senha do PostgreSQL |
| `INTEGRACAO_TOKEN` | vazio | Se definido, a integração exige o header `X-Integracao-Token` |

### Modo desenvolvimento (sem Docker)

Pré-requisitos: JDK 21, Maven 3.9+ e Node 18.19+ (recomendado Node 20).

```bash
# terminal 1 – API com H2 em arquivo (./backend/data), sem precisar de PostgreSQL
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
# terminal 2 – frontend com proxy de /api para a porta 8080
cd frontend
npm install
npm start
```

No perfil `dev` o console do H2 fica em http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:file:./data/candidaturas`, usuário `sa`, sem senha).

### Testes

```bash
cd backend && mvn verify      # testes unitários (Mockito) + integração (MockMvc + H2)
cd frontend && npx ng build   # build de produção
```

## Funcionalidades

- **Candidaturas**: CRUD com filtros por status, empresa, plataforma, stack, prioridade, período e busca por texto; detalhe com interações, contatos e histórico de status.
- **Pipeline**: `INTERESSE → CANDIDATADO → TRIAGEM_RH → TESTE_TECNICO → ENTREVISTA_TECNICA → ENTREVISTA_FINAL → PROPOSTA → ACEITA | RECUSADA | SEM_RETORNO`. Toda mudança de status grava uma linha em `historico_status` e uma interação `MUDANCA_STATUS`.
- **Quadro (Kanban)**: arrastar e soltar muda o status; no celular a rolagem é horizontal e o arraste começa com toque longo.
- **Dashboard**: total, em andamento, taxa de resposta, funil por etapa, candidaturas por plataforma e por semana (com visualização em tabela).
  - *Taxa de resposta* = candidaturas enviadas (que passaram de `INTERESSE`) que chegaram pelo menos a `TRIAGEM_RH` ou foram `RECUSADA`, dividido pelas enviadas.
  - *Funil*: quantas candidaturas alcançaram cada etapa, considerando o histórico de status.
- **Follow-ups**: pendentes vencidos e dos próximos 7 dias, com botão de concluir.
- **Sem retorno automático**: candidaturas de `CANDIDATADO` a `ENTREVISTA_FINAL` sem interação há mais de X dias (padrão 21, ajustável em *Configurações*) viram `SEM_RETORNO`. O job roda todo dia às 06:00, e o botão "Executar agora" roda na hora.
- **Pendências**: campo opcional na candidatura, com selo "Pendente" na lista e no quadro, filtro "Só pendentes" e contador no dashboard. Para remover, apague o texto na edição.
- **Tema claro e escuro**: seletor no rodapé do menu (ou na barra superior no celular) com Automático, que segue o sistema, Claro e Escuro. A escolha fica salva no navegador.
- **CSV**: exportação (respeita os filtros da listagem) e importação com o mesmo cabeçalho, de modo que exportar, editar e reimportar não duplica.

## Integração com o agente

### `POST /api/integracao/candidaturas`

Recebe um **array JSON** (um objeto sozinho também é aceito). Campos obrigatórios para criar: `empresa` e `cargo`.

```json
[
  {
    "chaveExterna": "linkedin-4012345678",
    "empresa": "ACME Tecnologia",
    "cargo": "Desenvolvedor Java Pleno",
    "linkVaga": "https://www.linkedin.com/jobs/view/4012345678",
    "plataforma": "linkedin",
    "modalidade": "Remoto",
    "tipoContrato": "PJ",
    "faixaSalarial": "R$ 10.000 – 13.000",
    "pretensaoSalarial": "R$ 12.000",
    "nota": 8,
    "dataCandidatura": "2026-10-24",
    "status": "Candidatado",
    "prioridade": "Alta",
    "curriculoVersao": "v3-java-backend",
    "observacoes": "Vaga 100% remota",
    "stack": ["Java", "Spring Boot", "Kafka"],
    "contatoNome": "Ana Souza",
    "contatoEmail": "ana.souza@acme.com",
    "contatoTelefone": "(11) 99999-0000",
    "tipoInteracao": "e-mail",
    "descricaoInteracao": "Candidatura enviada pelo LinkedIn",
    "proximoPasso": "Cobrar retorno",
    "dataFollowUp": "2026-10-31"
  }
]
```

Regras:

- **Upsert**: a chave é `chaveExterna` ou, se vier vazia, `linkVaga`. Se já existir uma candidatura com essa `chave_externa` ou esse `link_vaga`, só os campos enviados são atualizados. Se o status mudar, a mudança vai para o histórico. Sem chave e sem link, a busca é por empresa + cargo, sem diferenciar maiúsculas. Quando nada é encontrado, a candidatura é criada com origem `AGENTE` e status padrão `CANDIDATADO`.
- **Enums em texto livre**: a comparação ignora acento, caixa, espaço e hífen (`"Híbrido"`→`HIBRIDO`, `"Triagem RH"`→`TRIAGEM_RH`, `"e-mail"`→`EMAIL`, `"linkedin"`→`LINKEDIN`). Plataforma, tipo de contrato e tipo de interação desconhecidos viram `OUTRO`. Status, modalidade ou prioridade inválidos geram erro naquele item.
- **Contato**: é reaproveitado por e-mail ou telefone; se não existir, é criado como `RECRUTADOR` e vinculado.
- **Interação**: é criada se vier `descricaoInteracao` ou `dataFollowUp`. Reenviar o mesmo item não duplica a interação.
- **Pendência**: se `observacoes` começar com `PENDENTE:` (sem diferenciar maiúsculas), o texto depois do prefixo vira a pendência da candidatura. Reenviar as mesmas observações não recria uma pendência que você já limpou na tela. Isso também vale para a coluna `observacoes` do CSV.
- **Transação por item**: um erro num item não afeta os outros.

Resposta:

```json
{ "criadas": 1, "atualizadas": 0, "ignoradas": 0, "erros": ["Item 2: empresa é obrigatória"] }
```

`ignoradas` conta os itens que já existiam e não trouxeram nenhuma mudança.

### `GET /api/integracao/candidaturas/existe`

Use `?link=...` (procura por chave externa ou link) ou `?empresa=...&cargo=...` (sem link, sem diferenciar maiúsculas).

```json
{ "existe": true, "candidatura": { "id": 1, "empresa": "ACME Tecnologia", "cargo": "Desenvolvedor Java Pleno", "status": "CANDIDATADO", "...": "..." } }
```

### Token opcional

Se a variável `INTEGRACAO_TOKEN` estiver definida, os dois endpoints exigem o header `X-Integracao-Token` com o mesmo valor; sem ele a resposta é `401`. Sem a variável, os endpoints ficam liberados.

### Exemplos com curl

Bash / Git Bash. Salve o JSON num arquivo UTF-8 e envie com `--data-binary`, para os acentos chegarem corretos:

```bash
curl -X POST http://localhost:8080/api/integracao/candidaturas \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @candidaturas.json
```

```bash
curl -X POST http://localhost:8080/api/integracao/candidaturas \
  -H "Content-Type: application/json" \
  -d '[{"chaveExterna":"li-1","empresa":"ACME","cargo":"Dev Java","modalidade":"Remoto","status":"Candidatado","stack":["Java","Spring"]}]'
```

```bash
curl "http://localhost:8080/api/integracao/candidaturas/existe?link=https://www.linkedin.com/jobs/view/4012345678"
```

```bash
curl -G "http://localhost:8080/api/integracao/candidaturas/existe" --data-urlencode "empresa=acme" --data-urlencode "cargo=dev java"
```

Com token:

```bash
curl -X POST http://localhost:8080/api/integracao/candidaturas -H "X-Integracao-Token: meu-token" -H "Content-Type: application/json" --data-binary @candidaturas.json
```

PowerShell:

```powershell
$corpo = Get-Content .\candidaturas.json -Raw -Encoding UTF8
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/integracao/candidaturas -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($corpo))
```

## Pasta inbox

Outra forma de o agente alimentar o sistema é deixar arquivos `*.csv` na pasta `inbox/` deste projeto.
No Docker Compose ela é montada em `/app/inbox` no container do backend; no modo dev a API lê a mesma pasta `inbox/` da raiz. O caminho pode ser trocado pela variável `INBOX_DIR`.

- A cada **5 minutos** o backend lê os `*.csv`, importa com a mesma regra de upsert da integração (origem `AGENTE`) e move cada arquivo:
  - para `inbox/processados/` se todos os itens entraram;
  - para `inbox/erros/` se houve algum erro. Ao lado fica um `<arquivo>.erros.txt` com as mensagens (os itens válidos do arquivo já foram importados).
- Os arquivos movidos ganham um prefixo com data e hora (`20261024-153000_lote.csv`).
- Para processar na hora: *Configurações → Processar agora* ou `POST /api/inbox/processar`.

Formato (UTF-8, com ou sem BOM; o separador pode ser `,` ou `;`):

```csv
chave_externa,empresa,cargo,link_vaga,plataforma,modalidade,tipo_contrato,faixa_salarial,pretensao_salarial,nota,data_candidatura,status,prioridade,curriculo_versao,stack,observacoes,contato_nome,contato_email,contato_telefone,tipo_interacao,descricao_interacao,proximo_passo,data_follow_up
li-4012345678,ACME Tecnologia,Desenvolvedor Java Pleno,https://www.linkedin.com/jobs/view/4012345678,LinkedIn,Remoto,PJ,,R$ 12.000,8,24/10/2026,Candidatado,Alta,v3,Java;Spring Boot;Kafka,,Ana Souza,ana.souza@acme.com,,e-mail,Candidatura enviada,Cobrar retorno,2026-10-31
```

- `stack` é separada por `;`. Quando o separador do arquivo também for `;`, coloque a stack entre aspas.
- As datas podem vir como `yyyy-MM-dd` ou `dd/MM/yyyy`.
- A exportação (`GET /api/csv/exportar` ou o botão **CSV** na listagem) usa exatamente este cabeçalho.

## API (resumo)

A lista completa está no Swagger.

| Método | Caminho | Descrição |
|---|---|---|
| GET/POST | `/api/candidaturas` | Lista (filtros + paginação) / cria |
| GET/PUT/DELETE | `/api/candidaturas/{id}` | Detalhe / atualiza / exclui |
| PATCH | `/api/candidaturas/{id}/status` | Muda o status (histórico + interação) |
| GET | `/api/candidaturas/kanban` | Candidaturas agrupadas por status |
| POST | `/api/candidaturas/{id}/interacoes` | Registra interação |
| PATCH | `/api/interacoes/{id}/follow-up` | Conclui follow-up |
| GET/POST/PUT/DELETE | `/api/contatos` | Contatos |
| GET | `/api/dashboard` | Indicadores |
| GET | `/api/follow-ups` | Follow-ups vencidos e dos próximos 7 dias |
| GET/PUT | `/api/sem-retorno/configuracao` | Prazo do sem retorno |
| POST | `/api/sem-retorno/executar` | Executa o sem retorno agora |
| GET/POST | `/api/csv/exportar`, `/api/csv/importar` | CSV |
| POST | `/api/inbox/processar` | Processa a pasta inbox agora |
| POST/GET | `/api/integracao/candidaturas`, `/existe` | Integração com o agente |

Erros seguem um corpo padronizado:

```json
{ "timestamp": "2026-10-24T10:00:00-03:00", "status": 400, "erro": "Bad Request", "mensagem": "Dados inválidos", "caminho": "/api/candidaturas", "campos": [{ "campo": "empresa", "mensagem": "must not be blank" }] }
```

## Estrutura

```
candidaturas/
├─ backend/                 Spring Boot (config, domain, repository, dto, service, web, util)
│  └─ src/main/resources/db/migration   migrations Flyway
├─ frontend/                Angular (core, shared, pages/*)
├─ inbox/                   CSVs do agente (processados/ e erros/)
├─ docker-compose.yml
└─ .github/workflows/       CI do backend e do frontend
```

## Fluxo de desenvolvimento

- Uma branch por funcionalidade (`feat/...`, `fix/...`, `chore/...`) e Pull Request para a `main`
- Mensagens no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português
- O CI roda `mvn verify` e `ng build` em cada PR
