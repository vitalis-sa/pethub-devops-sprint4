# PetHub · API veterinária com CI/CD no Azure DevOps 🐾

O **PetHub** é uma plataforma de cuidado veterinário contínuo que liga tutor (responsável), veterinário e clínica:
do agendamento de consultas ao prontuário, vacinas, exames e atendimento assistido por IA.

Este repositório é a entrega de **DevOps Tools & Cloud Computing, Sprint 4** (FIAP, Challenge 2026, 2º semestre).
Ele leva o **serviço clínico do PetHub** (API Java 21 + Spring Boot, da disciplina Java Advanced) para uma esteira
de **CI/CD no Azure DevOps**:

- cada push na `master` dispara o CI, que compila, roda os testes automatizados e publica o artefato;
- cada novo artefato dispara o CD, que faz o deploy automático no **Azure Web App**;
- a aplicação publicada usa o **Oracle Database da FIAP** como banco em nuvem.

> O código da aplicação veio do repositório [`vitalis-sa/pethub-java`](https://github.com/vitalis-sa/pethub-java),
> clonado **com o histórico de commits**.

---

## 👥 Integrantes

| RM | Nome completo | Turma |
|:---:|:---|:---:|
| RM561489 | Ana Flávia Camelo | 2TDSPV |
| RM562745 | Gustavo Kenji Terada | 2TDSPV |
| RM566234 | João Guilherme Carvalho Novaes | 2TDSPV |
| RM565154 | Pedro Chasci Puga | 2TDSPV |
| RM561342 | Lucas Figueiredo Vieira | 2TDSPV |

## 🔗 Links da entrega

| Item | Link |
|---|---|
| Repositório GitHub | https://github.com/vitalis-sa/pethub-devops-sprint4 |
| Projeto no Azure DevOps | https://dev.azure.com/RM565154/Sprint%204%20%E2%80%93%20Azure%20DevOps |
| Vídeo no YouTube | _(preencher)_ |
| Aplicação no Azure Web App (Swagger UI) | https://rm566234-pethub.azurewebsites.net/swagger-ui.html |

---

## 1. 📝 Descrição da solução

O PetHub é dividido em serviços. **Este** é o serviço **clínico**: uma **API REST** protegida por JWT e documentada
com Swagger. Ela cuida de:

- **Clínica:** cadastro de veterinários (com login) e das unidades veterinárias;
- **Pets e consultas:** agendamento com validação de agenda (o mesmo veterinário não atende dois pets no mesmo
  horário, e não se agenda no passado);
- **Prontuário:** diagnósticos, exames, pedidos médicos, vacinas e tratamentos;
- **IA na consulta:** a partir da transcrição do atendimento, sugere anamnese e receita, que o veterinário revisa e
  aprova;
- **Segurança:** JWT HS256, senhas com hash BCrypt e permissões por perfil (`VETERINARIO` e `RESPONSAVEL`).

O cadastro do tutor e os lembretes ficam em outro serviço do grupo (.NET), e as telas ficam no aplicativo móvel. Nesta
entrega, o que passa pela esteira e vai para a nuvem é a API Java.

### Stack

| Camada | Tecnologia |
|---|---|
| Aplicação | Java 21, Spring Boot 4.0.6, Spring Web, Spring Security (JWT HS256, BCrypt), Bean Validation, MapStruct, Lombok, Caffeine Cache, springdoc-openapi (Swagger) |
| Persistência | Spring Data JPA (Hibernate), **Flyway** (versionamento do schema), driver `ojdbc11` |
| Banco de dados em nuvem | **Oracle Database da FIAP** (`oracle.fiap.com.br:1521/orcl`), schema `RM566234` |
| Build e testes | Maven (wrapper `mvnw`), JUnit 5, Mockito, AssertJ: **142 testes automatizados em 29 classes** |
| Versionamento | Git + **GitHub** (branch `master`) |
| CI/CD | **Azure DevOps**: Pipelines (CI em YAML), Releases (CD clássico), Library (variáveis protegidas) |
| Nuvem | **Azure App Service**: Web App Linux, Java 21 SE, plano B1, região Brazil South |

---

## 2. 🏗️ Arquitetura e fluxo CI/CD

![Arquitetura e fluxo CI/CD do PetHub](docs/arquitetura-devops.png)

> Fonte editável: [`docs/arquitetura-devops.drawio`](docs/arquitetura-devops.drawio) (draw.io / diagrams.net).

Fluxo numerado (os mesmos números do diagrama):

1. O **desenvolvedor** altera o código na IDE e faz `git push` para a branch `master` no **GitHub**.
2. O push dispara a pipeline de **CI** no **Azure DevOps** (gatilho `trigger: master` do
   [`azure-pipelines.yml`](azure-pipelines.yml)).
3. Um agente hospedado (`ubuntu-latest`, JDK 21) **compila** o projeto com Maven.
4. O Maven **roda os testes automatizados**, e o resultado é publicado na aba **Tests** da execução.
5. O `.jar` executável é publicado como **artefato `pethub-jar`** no Azure DevOps.
6. O novo artefato aciona o **gatilho de implantação contínua** da Release **`pethub-cd`** (CD).
7. A Release lê as **variáveis protegidas** do variable group `pethub-segredos` e publica o `.jar` no **Azure Web App**
   `rm566234-pethub`, gravando as configurações da aplicação (app settings).
8. Na subida, a aplicação conecta ao **Oracle da FIAP** e o **Flyway** aplica as migrations pendentes.
9. O **usuário final** (veterinário, ou o professor pelo Swagger/REST Client) acessa a API publicada, e cada operação
   do CRUD é persistida no Oracle.

| Ferramenta | Onde atua | Passos |
|---|---|---|
| IDE (VS Code / IntelliJ) + Git | Desenvolvimento e `push` | 1 |
| GitHub | Repositório de código, origem do gatilho do CI | 1–2 |
| Azure Pipelines | CI: build, testes e artefato | 2–5 |
| Pipeline Artifacts (Azure DevOps) | Guarda o `.jar` de cada execução do CI | 5 |
| Azure Releases | CD: deploy automático no Web App | 6–7 |
| Azure DevOps Library | Variable group com usuário, senha e segredo protegidos | 7 |
| Azure App Service (Web App) | Hospeda a API Java 21 | 7–9 |
| Oracle da FIAP | Banco de dados em nuvem | 8–9 |

---

## 3. 🗄️ Banco de dados em nuvem

O banco é o **Oracle Database da FIAP** (aceito pelo enunciado), no schema `RM566234`, usado só por esta entrega.

- O schema é criado e versionado pelo **Flyway** a partir de
  [`V1__schema_inicial.sql`](src/main/resources/db/migration/V1__schema_inicial.sql): 10 tabelas, 10 sequences e
  16 chaves estrangeiras.
- O Hibernate não cria nem altera tabelas, só confere se as entidades batem com o banco (`ddl-auto=validate`).
- Na subida, a aplicação cadastra as duas unidades da clínica (Paulista e Aclimação), se ainda não existirem.

Os `SELECT` de evidência do CRUD estão em [`database/consultas-video.sql`](database/consultas-video.sql) (veja a
[seção de demonstração](#-demonstração-do-crud-com-evidência-no-banco)).

---

## 4. ⚙️ Projeto no Azure DevOps

| Campo | Valor |
|---|---|
| Project name | `Sprint 4 – Azure DevOps` |
| Description | Projeto para entrega da Sprint 4 do professor João Menk. Integrantes: RM561489 - Ana Flávia Camelo - 2TDSPV · RM562745 - Gustavo Kenji Terada - 2TDSPV · RM566234 - João Guilherme Carvalho Novaes - 2TDSPV · RM565154 - Pedro Chasci Puga - 2TDSPV · RM561342 - Lucas Figueiredo Vieira - 2TDSPV |
| Visibility | Private |
| Version control | Git |
| Work item process | Scrum |

## 5. 🔑 Acesso do professor

O professor foi convidado com nível de acesso **Basic** (Organization Settings › Users) e adicionado como
**Project administrator** (Project Settings › Overview), como no exemplo do enunciado.

---

## 6. 🚀 Pipelines CI/CD

### CI: build + testes + artefato ([`azure-pipelines.yml`](azure-pipelines.yml))

É uma pipeline YAML, `pethub-ci`, conectada a este repositório do GitHub por uma *service connection*. Ela dispara a
cada push na `master`.

| Task | O que faz |
|---|---|
| `Maven@4` (`clean package`) | Compila com JDK 21, **roda os testes automatizados** e empacota o `.jar` (Spring Boot) |
| ↳ `publishJUnitResults` | Publica os relatórios JUnit (`surefire-reports`) na aba **Tests** |
| `CopyFiles@2` | Separa o `.jar` executável em `$(Build.ArtifactStagingDirectory)` |
| `PublishPipelineArtifact@1` | **Publica o artefato `pethub-jar`** no Azure DevOps |

Os **142 testes** das 29 classes rodam no CI. Só um teste fica de fora: `PethubApplicationTests`, que sobe o contexto
inteiro e precisa do Oracle. Ele tem a tag JUnit `integracao` e o CI o exclui com `-DexcludedGroups=integracao`.
Assim, nenhuma credencial de banco chega ao agente de build.

### CD: deploy automático (Release clássica `pethub-cd`)

| Configuração | Valor |
|---|---|
| Artefato | Build da pipeline `pethub-ci`, alias `_pethub-ci`, versão padrão *Latest* |
| Gatilho | **Continuous deployment trigger** ligado, com filtro de branch `master`: cada artefato novo cria uma release |
| Stage | `Produção (Azure Web App)`, agente `ubuntu-latest` |
| Task | **Azure Web App** (`AzureWebApp@1`): assinatura pela *service connection* `azure-rm566234`, tipo *Web App on Linux*, app `rm566234-pethub`, pacote `$(System.DefaultWorkingDirectory)/_pethub-ci/pethub-jar/*.jar`, runtime `JAVA\|21-java21` |
| App settings | `-DB_USER $(DB_USER) -DB_PASSWORD $(DB_PASSWORD) -JWT_SECRET $(JWT_SECRET) -SERVER_PORT 80 -ARMAZENAMENTO_MODO local` |
| Variáveis | Variable group `pethub-segredos` vinculado à release |

### Regras da pipeline atendidas

| Regra do enunciado | Como foi atendida |
|---|---|
| I. Conectada ao repositório da aplicação | CI ligado a este repositório do GitHub por service connection |
| II. CI dispara a cada alteração na `master` | `trigger: branches: include: [master]` no YAML |
| III. CD dispara após novo artefato | Continuous deployment trigger da Release no artefato `pethub-jar` |
| IV. Variáveis sensíveis protegidas | Variable group `pethub-segredos` com `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` marcadas como *secret* (cadeado). Nada no código nem no YAML |
| V. Artefato gerado e publicado no Azure DevOps | `PublishPipelineArtifact@1` → `pethub-jar` |
| VI. Etapa de testes no CI | `Maven@4` roda os 142 testes e publica os resultados |
| VII. Deploy em Azure Web App ou ACI | `AzureWebApp@1` → Web App `rm566234-pethub` |

---

## 🔒 Variáveis de ambiente e segurança

| Variável | Sensível | Onde fica |
|---|:---:|---|
| `DB_USER` | sim | Variable group `pethub-segredos` (secret) → app setting do Web App |
| `DB_PASSWORD` | sim | Variable group `pethub-segredos` (secret) → app setting do Web App |
| `JWT_SECRET` | sim | Variable group `pethub-segredos` (secret, ≥ 32 caracteres) → app setting do Web App |
| `SERVER_PORT` | não | `80`: porta que o App Service Linux encaminha para a aplicação |
| `ARMAZENAMENTO_MODO` | não | `local`: arquivos em disco, sem depender do Google Cloud Storage |

- **Nenhuma credencial** está no código, no YAML ou nos scripts. Localmente as variáveis vêm de um `.env`, que é
  ignorado pelo Git; o [`.env.example`](.env.example) lista quais existem, sem valores.
- A senha dos usuários é gravada com hash **BCrypt** e nunca volta em nenhuma resposta da API.
- O Web App aceita só HTTPS e tem o FTP desligado ([`azure/01-criar-webapp.sh`](azure/01-criar-webapp.sh)).

O serviço também se integra a Groq (IA), RabbitMQ, Brevo (e-mail), Google Cloud Storage e ao serviço .NET. Essas
integrações **não** são configuradas nesta entrega. Sem elas, a API sobe e o CRUD funciona normalmente: as rotas de
IA respondem `503` e os avisos ao tutor não saem.

---

## 💻 How-to: reproduzir a esteira (passo a passo)

### Pré-requisitos

- [Git](https://git-scm.com/) e **JDK 21** (o Maven vem no wrapper `mvnw`)
- [Azure CLI](https://learn.microsoft.com/pt-br/cli/azure/install-azure-cli), com `az login` feito na assinatura
- Organização no Azure DevOps e acesso a um schema Oracle (usuário e senha)

### 1. Clonar o repositório

```bash
git clone https://github.com/vitalis-sa/pethub-devops-sprint4.git
cd pethub-devops-sprint4
```

### 2. Criar os recursos na Azure

Os scripts de [`azure/`](azure/) usam a Azure CLI. Eles **criam e apagam recursos pagos**.

```bash
bash azure/01-criar-webapp.sh   # Resource Group, App Service Plan B1 Linux e Web App Java 21
bash azure/02-status.sh         # estado, URL e teste do /api-docs (só leitura)
```

### 3. Configurar o Azure DevOps

1. **Service connections** (Project settings): uma do tipo **GitHub** e uma **Azure Resource Manager** com acesso ao
   Resource Group `rg-rm566234-sprint4`.
2. **Library:** criar o variable group `pethub-segredos` com `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`, clicando no
   **cadeado** de cada uma.
3. **Pipelines › New pipeline:** GitHub › este repositório › *Existing Azure Pipelines YAML file* ›
   `/azure-pipelines.yml`. Renomear para `pethub-ci`.
4. **Releases › New › Empty job** (`pethub-cd`): adicionar o artefato da `pethub-ci` com o gatilho de implantação
   contínua, a task **Azure Web App** e o vínculo com `pethub-segredos`, como na [seção 6](#cd-deploy-automático-release-clássica-pethub-cd).
   Organizações novas vêm com *Disable creation of classic release pipelines* ligado e escondem o menu Releases.
   Desligue a opção em Organization Settings › Pipelines › Settings e também nas configurações do projeto.
5. Fazer um push na `master`. O CI roda, o CD publica, e `bash azure/02-status.sh` confirma que a API está no ar.

### 4. Rodar e testar localmente (opcional)

```bash
cp .env.example .env                              # preencha DB_USER, DB_PASSWORD e JWT_SECRET
./mvnw -B clean package -DexcludedGroups=integracao   # o mesmo comando do CI (não precisa de banco)
./mvnw spring-boot:run                            # sobe em http://localhost:8080/swagger-ui.html
```

No Windows, `.\run.ps1` carrega o `.env`, avisa se faltar variável obrigatória e sobe a aplicação.

### 5. Limpeza (depois da correção)

```bash
bash azure/99-cleanup.sh        # apaga o Resource Group inteiro (pede confirmação)
```

---

## 🧪 Demonstração do CRUD com evidência no banco

O CRUD é feito pelo **Swagger UI** da aplicação publicada e, depois de cada operação, um `SELECT` no Oracle mostra a
persistência. As duas entidades da demonstração, **Unidade veterinária** e **Veterinário**, não dependem de nenhum
serviço externo. As mesmas chamadas, com os corpos JSON prontos, estão em [`docs/demo-crud.http`](docs/demo-crud.http).
Esse arquivo abre no VS Code com a extensão *REST Client*: cada bloco `###` vira um botão "Send Request".

| Passo | Chamada | Evidência no Oracle ([`consultas-video.sql`](database/consultas-video.sql)) |
|---|---|---|
| 0 | `POST /api/auth/registrar/veterinario`: cria o operador e devolve o token JWT | `flyway_schema_history` mostra a V1 aplicada |
| 1.1 | `POST /api/unidades`: **inserir** unidade | a linha aparece em `TB_UNIDADE_VETERINARIO` |
| 1.2 | `GET /api/unidades/{id}`: **consultar** | mesma linha |
| 1.3 | `PUT /api/unidades/{id}`: **atualizar** nome e número | a linha muda |
| 1.4 | `DELETE /api/unidades/{id}`: **deletar** | o SELECT volta vazio |
| 2.1–2.4 | O mesmo ciclo em `/api/veterinarios` | `TB_VETERINARIO`, com a senha gravada em hash BCrypt |

Para usar o Swagger UI:

1. Abra https://rm566234-pethub.azurewebsites.net/swagger-ui.html.
2. Em **Autenticação**, execute `POST /api/auth/registrar/veterinario` (ou `/login`) e copie o `token`.
3. Clique em **Authorize** e cole **só o token**, sem escrever `Bearer`.
4. A partir daí, todo *Try it out* já envia o cabeçalho `Authorization`.
5. Nos `POST` e `PUT`, troque o exemplo do Swagger pelos corpos de [`docs/demo-crud.http`](docs/demo-crud.http). O
   exemplo traz `unidadeId`/`veterinarioId` com valor `0`, que não existe.

---

## 🗺️ Rotas da API

Toda rota `/api/**` fora de `/api/auth/**` exige o token JWT (`Authorization: Bearer <token>`); sem ele a resposta é
`401`. O token vale 8 horas. Na coluna **Acesso**, `público` dispensa token, `ambos` aceita os dois perfis e `VET`
exige o perfil `VETERINARIO`.

### Autenticação e clínica (usadas no CRUD do vídeo)

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/auth/registrar/veterinario` | Cadastra o veterinário e já devolve o token | público |
| POST | `/api/auth/login` | Autentica e devolve o token | público |
| GET | `/api/unidades` · `/api/unidades/{id}` | Lista (paginado) ou busca unidade | ambos |
| POST · PUT · DELETE | `/api/unidades` · `/api/unidades/{id}` | Cria, atualiza ou apaga unidade | VET |
| GET | `/api/veterinarios` · `/api/veterinarios/{id}` | Lista (paginado) ou busca veterinário | ambos |
| POST · PUT · DELETE | `/api/veterinarios` · `/api/veterinarios/{id}` | Cria, atualiza ou apaga veterinário | VET |

### Pets, consultas e prontuário

Cada recurso abaixo tem o CRUD completo: `GET` (lista paginada e por id), `POST`, `PUT /{id}` e `DELETE /{id}`.

| Recurso | Rota base | Observação |
|---|---|---|
| Pets | `/api/pets` | Também `GET /api/pets/responsavel?cpf=`; criar pet consulta o serviço .NET |
| Consultas | `/api/consultas` | Agendamento com validação de agenda (`409` se o horário estiver ocupado ou no passado) |
| Exames | `/api/exames` | Escrita só para VET |
| Diagnósticos | `/api/diagnosticos` | Escrita só para VET |
| Vacinas e tratamentos | `/api/vacinas-tratamentos` | Escrita só para VET |
| Pedidos médicos | `/api/pedidos-medicos` | Escrita só para VET |

Rotas que dependem das integrações não configuradas nesta entrega: análise da consulta com IA
(`/api/consultas/{id}/analise-ia`, `/receita`), resumo de exame (`/api/exames/{id}/resumo-ia`) e envio de arquivos
(`/api/exames/{id}/arquivo`, `/api/pets/{id}/foto`). A lista completa, com os modelos de requisição e resposta, está
no Swagger UI.

### Respostas de erro

| Status | Significado |
|---|---|
| `400` | Falha de validação; a resposta lista o erro campo a campo |
| `401` | Sem token, token expirado ou credencial errada |
| `403` | Autenticado, mas o perfil não tem permissão |
| `404` | Recurso inexistente |
| `409` | Regra de negócio recusou a operação (agenda ocupada, e-mail já cadastrado etc.) |
| `503` | Dependência externa fora do ar ou não configurada (IA, serviço .NET) |

---

## 🛠️ Artefatos DevOps entregues

| Artefato | Descrição |
|---|---|
| [`azure-pipelines.yml`](azure-pipelines.yml) | CI em YAML: gatilho na `master`, build, testes, resultados JUnit e artefato `pethub-jar` |
| [`azure/00-vars.sh`](azure/00-vars.sh) | Nomes dos recursos e região, centralizados (sem segredos) |
| [`azure/01-criar-webapp.sh`](azure/01-criar-webapp.sh) | Cria Resource Group, App Service Plan B1 Linux e Web App Java 21 (Always On, só HTTPS, logs) |
| [`azure/02-status.sh`](azure/02-status.sh) | Estado do Web App e teste do `/api-docs` (só leitura) |
| [`azure/99-cleanup.sh`](azure/99-cleanup.sh) | Apaga o Resource Group inteiro, com confirmação |
| [`docs/arquitetura-devops.png`](docs/arquitetura-devops.png) · [`.drawio`](docs/arquitetura-devops.drawio) | Diagrama da arquitetura e do fluxo CI/CD |
| [`docs/demo-crud.http`](docs/demo-crud.http) | Roteiro do CRUD contra a aplicação publicada |
| [`database/consultas-video.sql`](database/consultas-video.sql) | SELECTs de evidência de cada operação do CRUD |
| [`src/main/resources/db/migration/`](src/main/resources/db/migration/) | Migration do Flyway (schema versionado) |
| [`.env.example`](.env.example) | Modelo das variáveis de ambiente, **sem nenhum valor real** |

### Estrutura do código

```
src/main/java/fiap/pethub
├── PethubApplication.java   → classe principal e metadados do Swagger (título e versão)
├── config/                  → segurança, cache, CORS, .env e unidades iniciais
├── security/                → JWT, filtros de autenticação e regras de posse
├── controller/              → endpoints REST
├── service/                 → regras de negócio (agenda, prontuário, cascatas)
├── dto/  mapper/            → DTOs de entrada/saída e conversão com MapStruct
├── entity/  repository/     → entidades JPA e repositórios
├── enums/                   → perfis, status e tipos do domínio
├── exception/               → tratamento global de erros
└── ia/  email/  messaging/  integracao/  armazenamento/   → integrações externas
src/test/java/fiap/pethub    → 142 testes automatizados (JUnit 5 + Mockito)
```
