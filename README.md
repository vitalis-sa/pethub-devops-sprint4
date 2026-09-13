# PetHub API

Uma solução completa para o cuidado veterinário, conectando pets, responsáveis e clínicas em uma jornada inteligente: do agendamento de consultas ao controle de vacinas, exames, diagnósticos e lembretes, tudo em um só lugar.

Projeto entregue para a disciplina **Java Advanced** (FIAP, Challenge 2026 — 2º semestre,
Sprint 3), cobrindo a API REST do domínio veterinário do PetHub: frontend com Thymeleaf,
versionamento de schema com Flyway e autenticação/autorização com Spring Security.

## Entrega

| Artefato | Link |
|---|---|
| Repositório GitHub | https://github.com/vitalis-sa/pethub-java |
| Vídeo de demonstração | https://www.youtube.com/watch?v=iKAm3RBc3nI |

## Grupo

| Nome | RM |
|---|---|
| Ana Flávia Camelo | RM561489 |
| Gustavo Kenji Terada | RM562745 |
| João Guilherme Carvalho Novaes | RM566234 |
| Pedro Chasci Puga | RM565154 |
| Lucas Figueiredo Vieira | RM561342 |

## Tecnologias

| Tecnologia | Descrição |
|---|---|
| Java 21 | Linguagem principal do projeto, usando recursos modernos como `var`, text blocks e `switch` de expressão |
| Spring Boot 4.0.6 | Framework base que auto-configura o servidor embutido (Tomcat), gerencia beans e simplifica a criação da API REST |
| Maven | Gerenciador de dependências e build do projeto; responsável por baixar bibliotecas e compilar o código |
| Spring Data JPA | Abstrai o acesso ao banco de dados, gerando automaticamente as queries SQL a partir de interfaces como JpaRepository |
| Oracle Database (FIAP) | Banco de dados relacional utilizado para persistir os registros de pets, consultas, exames e outros dados da clínica |
| Thymeleaf | Motor de templates para renderização do Frontend, criando as telas de login e dashboards para Tutor e Vet |
| Flyway | Controle de versão do schema: aplica as migrations de `db/migration` no boot e registra o que já rodou na tabela `flyway_schema_history` |
| Spring Security | Autenticação e autorização da API: filtro JWT stateless, regras de acesso por rota e perfil, e BCrypt para o hash das senhas |
| JJWT (jjwt-api/impl/jackson) | Emissão e verificação dos tokens JWT assinados em HS256 |
| Caffeine Cache | Cache in-memory com expiração de 10 minutos e limite de 500 entradas para otimizar leituras frequentes |
| Lombok | Elimina código repetitivo (boilerplate) gerando automaticamente getters, setters, construtores e toString via anotações como @Data, @RequiredArgsConstructor |
| MapStruct | Gera o código de conversão entre entidades e DTOs (entrada/saída) sem precisar escrever mapeamentos manualmente |
| Spring Validation | Valida os dados recebidos nas requisições com anotações como @NotBlank, @NotNull e @Size, retornando erros 400 automaticamente quando inválidos |
| Springdoc OpenAPI | Gera documentação automaticamente e fornece interface Swagger UI para testar os endpoints |
| Spring Scheduling | Agenda a rotina diária que apura o consumo de água de cada pet e dispara alerta de desidratação |

## Instalação

Pré-requisitos:

- **JDK 21** (`java -version` deve responder 21)
- Acesso ao **Oracle da FIAP** (usuário RM e senha), ou a outro Oracle informado em `DB_URL`
- Maven não precisa ser instalado: o projeto traz o wrapper (`mvnw` / `mvnw.cmd`)

```bash
git clone https://github.com/vitalis-sa/pethub-java.git
cd pethub-java
cp .env.example .env
```

Preencha o `.env` recém-criado:

| Variável | Obrigatória | Para que serve |
|---|---|---|
| `DB_USER` | sim | Usuário do Oracle (o RM) |
| `DB_PASSWORD` | sim | Senha do Oracle |
| `JWT_SECRET` | sim | Segredo que assina os tokens. **Mínimo de 32 caracteres**, senão o HS256 recusa a chave e a aplicação não sobe. Gere um com `openssl rand -base64 32` |
| `DB_URL` | não | Só para apontar para outro banco. Sem ela, usa `jdbc:oracle:thin:@oracle.fiap.com.br:1521/orcl` |

O `.env` **não é versionado** (está no `.gitignore`); o `.env.example` é a referência de quais
variáveis existem. Nenhuma credencial fica no código-fonte.

## Execução

```powershell
.\run.ps1
```

O script carrega o `.env`, avisa se faltar alguma variável obrigatória e chama o Maven. Também
funciona rodar direto — a classe `ConfiguracaoDotenv` lê o `.env` da raiz no boot, então tanto o
Maven quanto a IDE encontram as variáveis sozinhos:

```bash
./mvnw spring-boot:run
```

Para empacotar e rodar o jar, ou subir em container:

```bash
./mvnw clean package
java -jar target/pethub-0.0.1-SNAPSHOT.jar
```

Há também um `docker-compose.yml` que sobe a aplicação **e** um Oracle XE local, lendo o mesmo
`.env`. Nesse caminho, preencha também `ORACLE_PWD` e aponte
`DB_URL=jdbc:oracle:thin:@//oracle-db:1521/XEPDB1`, senão o container continuará falando com o
Oracle da FIAP:

```bash
docker compose up --build
```

Ao subir, a aplicação:

1. aplica as migrations do Flyway no schema (ver [Migrations](#migrations-flyway));
2. valida se as entidades JPA batem com as tabelas (`ddl-auto=validate`);
3. sobe em `http://localhost:8080`.

| Recurso | URL |
|---|---|
| **Aplicação web (frontend)** | **`http://localhost:8080`** |
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

## Acesso à aplicação

O PetHub tem duas portas de entrada, com formas de autenticação diferentes:

| | Como entra | Para quem |
|---|---|---|
| **Telas web** (`/`, `/tutor/**`, `/vet/**`) | Formulário de login, sessão por cookie | Pessoas, no navegador |
| **API REST** (`/api/**`) | Token JWT no cabeçalho `Authorization` | Aplicativo móvel e integrações |

### Pelo navegador — caminho mais curto

1. Abra **`http://localhost:8080`**. Sem sessão, a aplicação leva você para a tela de login.
2. Em um banco limpo não existe usuário pré-cadastrado: clique em **"Sou Tutor"** ou
   **"Sou Veterinário"** e crie a sua conta — ou carregue os
   [dados de demonstração](#carregar-os-dados-de-demonstração-opcional) e entre com um dos
   logins prontos.
3. Depois de entrar, cada perfil cai no seu próprio painel:

| Perfil | Painel | O que faz por ali |
|---|---|---|
| Tutor (`RESPONSAVEL`) | `/tutor/dashboard` | Cadastra pets, lê o prontuário de cada um e acompanha os lembretes |
| Veterinário | `/vet/dashboard` | Agenda consultas e registra o prontuário |

Para ver o sistema inteiro funcionando, crie **as duas contas**: o veterinário agenda a
consulta e preenche o prontuário; o tutor, na conta dele, vê o diagnóstico, o exame pedido
e os lembretes que o sistema criou sozinho.

> Tentar abrir `/vet/**` logado como tutor (ou o contrário) devolve a página de acesso
> negado — é o controle de acesso por perfil em ação.

### Carregar os dados de demonstração (opcional)

Em vez de cadastrar contas na mão, é possível carregar um cenário pronto — o mesmo usado no
[vídeo de demonstração](#entrega) desta entrega: uma clínica, um veterinário, dois tutores com
pets distintos, uma consulta já realizada e outra agendada. Basta rodar `docs/seed_demo_video.sql`
no schema da aplicação. Ele cria estes acessos, todos com a senha `123456`:

| Login | Perfil | O que essa conta permite fazer |
|---|---|---|
| `vet_video@pethub.com` | `VETERINARIO` | Agendar consulta e registrar o prontuário |
| `joao_video@demo.com` | `RESPONSAVEL` | Ver o prontuário e os lembretes do próprio pet |
| `maria_video@demo.com` | `RESPONSAVEL` | Provar o isolamento entre tutores: este login não enxerga o pet do outro |

Esse seed é material de demonstração e por isso **não** é uma migration do Flyway: dado de
exemplo não deve viajar junto com o schema para todo ambiente que rodar o projeto.

### Pela API

**Toda rota `/api/**` fora de `/api/auth/**` exige um token JWT.** Sem o cabeçalho
`Authorization`, a resposta é `401`.

### 1. Criar uma conta

Não existe usuário pré-cadastrado — o primeiro acesso passa por um dos dois cadastros públicos:

```bash
curl -X POST http://localhost:8080/api/auth/registrar/veterinario \
  -H "Content-Type: application/json" \
  -d '{"nome":"Dra. Ana","crmv":"SP-12345","email":"ana@pethub.com","senha":"senha123","especialidade":"Clínica geral","ativo":true}'
```

```bash
curl -X POST http://localhost:8080/api/auth/registrar/responsavel \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","cpf":"12345678901","email":"joao@email.com","senha":"senha123","ativo":true}'
```

O cadastro já devolve o token, dispensando um login em seguida. A senha é gravada com hash BCrypt
e nunca volta em nenhuma resposta.

### 2. Autenticar

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@pethub.com","senha":"senha123"}'
```

Resposta (`200`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "perfil": "VETERINARIO",
  "nome": "Dra. Ana",
  "id": 1
}
```

Credencial errada ou cadastro inativo devolve `401`.

### 3. Chamar a API com o token

O token vale **8 horas** (`pethub.jwt.validade=PT8H`) e vai no cabeçalho `Authorization`, com o
prefixo `Bearer`:

```bash
curl http://localhost:8080/api/pets \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 4. Pelo Swagger UI

1. Abra `http://localhost:8080/swagger-ui.html`
2. Em **Autenticação**, execute `POST /api/auth/login` e copie o valor de `token`
3. Clique no botão **Authorize** (canto superior direito) e cole **só o token**, sem escrever `Bearer`
4. A partir daí todo *Try it out* já envia o cabeçalho sozinho

### O que cada resposta de erro significa

| Status | Significado | O que fazer |
|---|---|---|
| `400` | Falha de validação nos dados enviados | A resposta lista o erro campo a campo |
| `401` | Sem token, token expirado ou assinatura inválida | Refaça o login |
| `403` | Token válido, mas o perfil não tem permissão para a operação | Use uma conta de veterinário |
| `404` | Recurso inexistente **ou** de outro responsável | Confira o id; um responsável não enxerga dado alheio |
| `409` | Regra de negócio recusou a operação | Leia a mensagem: agenda ocupada, prontuário já registrado ou e-mail em uso |

## Perfis e permissões

São dois tipos de usuário, e a diferença entre eles segue o domínio: **o veterinário produz o
prontuário, o responsável o consulta.**

| | `VETERINARIO` | `RESPONSAVEL` |
|---|---|---|
| Cadastro | `POST /api/auth/registrar/veterinario` | `POST /api/auth/registrar/responsavel` |
| Prontuário (consultas, diagnósticos, exames, pedidos médicos, vacinas) | cria, edita e apaga | apenas lê |
| Pets, veterinários e unidades | cria, edita e apaga | apenas lê |
| Leituras do wearable | cria, edita e apaga | apenas lê |
| Lembretes | leitura e escrita | leitura e escrita, só os próprios |
| Dados pessoais do responsável (contatos, endereços) | apenas lê, para vincular um pet ao tutor | é o único que edita |
| Alcance das listagens | todos os registros da clínica | somente os pets sob sua responsabilidade |

A proteção acontece em duas camadas, e as duas importam:

- **Por rota e método**, em [`SecurityConfig`](src/main/java/fiap/pethub/config/SecurityConfig.java):
  quem não tem o perfil exigido recebe `403` antes de a requisição chegar ao controller. Manter as
  regras num arquivo só permite ler de cima a baixo quem pode fazer o quê.
- **Por posse do recurso**, em [`EscopoDoUsuario`](src/main/java/fiap/pethub/security/EscopoDoUsuario.java):
  um `RESPONSAVEL` autenticado que peça o pet de outro tutor recebe `404`, não `403` — a API não
  confirma nem nega a existência de um recurso alheio. As listagens são filtradas pelo responsável
  do token, e a chave de cache inclui o escopo do usuário, para que um acerto de cache nunca
  devolva a um tutor o dado carregado por outro.

## Migrations (Flyway)

O schema é versionado pelo Flyway, não gerado pelo Hibernate. As migrations ficam em
`src/main/resources/db/migration` e rodam sozinhas no start da aplicação.

| Arquivo | Conteúdo |
|---|---|
| `V1__schema_inicial.sql` | 13 sequences, 13 tabelas e 20 chaves estrangeiras — o schema completo do PetHub |

Configuração relevante em `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.datasource.hikari.maximum-pool-size=2
```

- `ddl-auto=validate` — o Hibernate deixa de criar e alterar tabela; ele apenas **confere** se as
  entidades batem com o que o Flyway aplicou. Se divergirem, a aplicação não sobe, e é justamente
  esse o ponto: a divergência aparece no boot, não em produção.
- `baseline-on-migrate` — o schema da FIAP já existia, criado pelo `ddl-auto=update` das sprints
  anteriores. Sem isso, o Flyway encontraria tabelas fora do seu controle e recusaria trabalhar.
  Com a baseline na versão 1, ele adota o schema atual como ponto de partida.
- `maximum-pool-size=2` — o pool da FIAP é limitado e o projeto o mantém no mínimo viável, mas
  **1 não basta**: no boot o Flyway segura uma conexão para aplicar a migration e pede outra para
  gravar o histórico. Com o pool em 1, ele espera os 30s do `connection-timeout` e a aplicação
  morre sem subir.

No start, o log mostra o Flyway trabalhando — é assim que se confirma que as migrations rodaram:

```
o.f.core.internal.command.DbValidate  : Successfully validated 1 migration
o.f.core.internal.command.DbMigrate   : Current version of schema "RM566234": 1
o.f.core.internal.command.DbMigrate   : Schema "RM566234" is up to date. No migration necessary.
```

**Para criar uma nova migration**, adicione um arquivo `V2__descricao_curta.sql` na mesma pasta.
Migration já aplicada nunca deve ser editada — o Flyway compara o checksum e falha se o conteúdo
mudou. Corrija sempre com uma versão nova.

## Fluxos completos (não-CRUD)

A rubrica pede **dois fluxos completos que não sejam CRUD**. Os dois primeiros abaixo são esses:
cada um tem regra de negócio própria, recusa o que não faz sentido e envolve mais de uma
entidade. Os demais rodam sozinhos, sem ninguém chamar um endpoint de lembrete.

### 1. Agendamento de consulta com validação de agenda

Marcar uma consulta não é gravar uma linha. Em
[`ConsultaService.create`](src/main/java/fiap/pethub/service/ConsultaService.java) o agendamento
passa por duas guardas antes de existir, e só então gera o lembrete do tutor:

| Guarda | Regra | Se violar |
|---|---|---|
| Agenda livre | O mesmo veterinário não atende dois pets no mesmo horário. Consulta cancelada não ocupa agenda | `409` — "Este veterinário já tem uma consulta marcada neste horário." |
| Horário futuro | Só vale para o que está sendo **agendado**; lançar uma consulta já `REALIZADA` no passado continua permitido, porque registrar o que aconteceu é legítimo | `409` — "Não é possível agendar uma consulta para uma data que já passou." |

Passadas as guardas, a consulta é salva **e** o lembrete do responsável é criado na mesma
operação. Na web, o fluxo é a tela `/vet/consultas/nova`.

### 2. Registro do prontuário e fechamento da consulta

[`ProntuarioService.registrar`](src/main/java/fiap/pethub/service/ProntuarioService.java) fecha
uma consulta em **três escritas que só fazem sentido juntas**, dentro de uma única transação:

1. grava o diagnóstico;
2. emite o pedido médico, se o veterinário pediu exame ou medicamento — com prazo padrão de 7 dias;
3. dá a consulta por `REALIZADA`.

Ou as três entram, ou nenhuma entra. Antes disso há uma guarda de idempotência: se a consulta já
está realizada ou já tem diagnóstico, a operação é recusada com `409`. Sem ela, um F5 depois de
salvar empilharia um segundo diagnóstico na mesma consulta. Na web, é a tela
`/vet/consultas/{id}`.

### 4. Lembrete automático de próxima dose

Ao registrar uma vacina ou tratamento com o campo `proximaDose` preenchido, o sistema cria o
lembrete daquela data, vinculado ao pet e ao responsável. Sem `proximaDose`, nada é criado — é o
próprio dado clínico que decide.

### 5. Lembrete automático de consulta agendada

Toda consulta criada gera um lembrete para o responsável do pet, na data e hora do agendamento.

### 6. Lembrete automático de pedido médico

Ao emitir um pedido médico, o tipo do lembrete acompanha o tipo do pedido — `EXAME` ou
`MEDICAMENTO` — e a data limite vira a data agendada, com as instruções do veterinário no texto.

Todos os lembretes caem na mesma entidade `Lembrete`, com `referenciaTipo` e `referenciaId`
apontando para a origem. O tutor os acompanha em `/tutor/lembretes`, e a API os expõe em
`GET /api/lembretes`, com baixa em `PATCH /api/lembretes/{id}/status`.

## Mapa das páginas (frontend Thymeleaf)

Todas as telas da camada de visualização, com o link direto para a aplicação rodando localmente.
São 15 templates em `src/main/resources/templates`, e as capturas de cada uma estão em
[Prints das telas](#prints-das-telas-frontend-thymeleaf).

### Públicas

| Página | Link local | Template | O que faz |
|---|---|---|---|
| Login | [`/login`](http://localhost:8080/login) | `login.html` | Formulário de entrada dos dois perfis |
| Cadastro de tutor | [`/registrar/tutor`](http://localhost:8080/registrar/tutor) | `registrar-tutor.html` | Cria a conta `RESPONSAVEL` |
| Cadastro de veterinário | [`/registrar/vet`](http://localhost:8080/registrar/vet) | `registrar-vet.html` | Cria a conta `VETERINARIO` |

`/` não tem tela própria: redireciona para o painel do perfil logado, ou para `/login` sem sessão.

### Tutor (`RESPONSAVEL`)

| Página | Link local | Template | O que faz |
|---|---|---|---|
| Painel | [`/tutor/dashboard`](http://localhost:8080/tutor/dashboard) | `tutor/dashboard.html` | Lista os pets sob sua responsabilidade |
| Novo pet | [`/tutor/pets/novo`](http://localhost:8080/tutor/pets/novo) | `tutor/novo-pet.html` | Cadastra um pet |
| Prontuário do pet | `/tutor/pets/{id}` | `tutor/pet-detalhes.html` | Histórico: consultas, diagnósticos, exames e pedidos |
| Editar pet | `/tutor/pets/{id}/editar` | `tutor/editar-pet.html` | Edita e exclui o pet |
| Lembretes | [`/tutor/lembretes`](http://localhost:8080/tutor/lembretes) | `tutor/lembretes.html` | Lembretes gerados pelo sistema, com baixa |
| Meu perfil | [`/tutor/perfil`](http://localhost:8080/tutor/perfil) | `tutor/perfil.html` | Atualiza e exclui a própria conta |

### Veterinário (`VETERINARIO`)

| Página | Link local | Template | O que faz |
|---|---|---|---|
| Painel | [`/vet/dashboard`](http://localhost:8080/vet/dashboard) | `vet/dashboard.html` | Agenda de consultas do veterinário |
| Nova consulta | [`/vet/consultas/nova`](http://localhost:8080/vet/consultas/nova) | `vet/nova-consulta.html` | Agendamento com validação de agenda |
| Consulta | `/vet/consultas/{id}` | `vet/consulta-detalhes.html` | Registra o prontuário e conclui a consulta |
| Meu perfil | [`/vet/perfil`](http://localhost:8080/vet/perfil) | `vet/perfil.html` | Atualiza e exclui a própria conta |

### Compartilhados

| Template | Para que serve |
|---|---|
| `error.html` | Página de erro, incluindo o `403` de perfil errado |
| `fragments/campos.html` | Fragmento reaproveitado pelos formulários, para não repetir marcação (DRY) |

## Endpoints

Legenda da coluna **Acesso**: `público` dispensa token; `ambos` aceita os dois perfis; `VET` exige
o perfil `VETERINARIO`.

### Autenticação

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | /api/auth/login | Autenticar e receber o token JWT | público |
| POST | /api/auth/registrar/responsavel | Cadastrar responsável e já receber o token | público |
| POST | /api/auth/registrar/veterinario | Cadastrar veterinário e já receber o token | público |

### Pets e prontuário

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | /api/pets | Listar todos os pets (paginado) | ambos |
| GET | /api/pets/{id} | Buscar pet por ID | ambos |
| GET | /api/pets/responsavel?cpf= | Buscar pets por CPF do responsável | ambos |
| POST | /api/pets | Criar novo pet | VET |
| PUT | /api/pets/{id} | Atualizar pet por ID | VET |
| DELETE | /api/pets/{id} | Deletar pet por ID | VET |
| GET | /api/consultas | Listar todas as consultas (paginado) | ambos |
| GET | /api/consultas/{id} | Buscar consulta por ID | ambos |
| POST | /api/consultas | Criar nova consulta | VET |
| PUT | /api/consultas/{id} | Atualizar consulta | VET |
| DELETE | /api/consultas/{id} | Deletar consulta | VET |
| GET | /api/exames | Listar todos os exames (paginado) | ambos |
| GET | /api/exames/{id} | Buscar exame por ID | ambos |
| POST | /api/exames | Criar novo exame | VET |
| PUT | /api/exames/{id} | Atualizar exame | VET |
| DELETE | /api/exames/{id} | Deletar exame | VET |
| GET | /api/diagnosticos | Listar todos os diagnósticos (paginado) | ambos |
| GET | /api/diagnosticos/{id} | Buscar diagnóstico por ID | ambos |
| POST | /api/diagnosticos | Criar novo diagnóstico | VET |
| PUT | /api/diagnosticos/{id} | Atualizar diagnóstico | VET |
| DELETE | /api/diagnosticos/{id} | Deletar diagnóstico | VET |
| GET | /api/vacinas-tratamentos | Listar todas as vacinas/tratamentos (paginado) | ambos |
| GET | /api/vacinas-tratamentos/{id} | Buscar vacina/tratamento por ID | ambos |
| POST | /api/vacinas-tratamentos | Criar nova vacina/tratamento | VET |
| PUT | /api/vacinas-tratamentos/{id} | Atualizar vacina/tratamento | VET |
| DELETE | /api/vacinas-tratamentos/{id} | Deletar vacina/tratamento | VET |
| GET | /api/pedidos-medicos | Listar todos os pedidos médicos (paginado) | ambos |
| GET | /api/pedidos-medicos/{id} | Buscar pedido médico por ID | ambos |
| POST | /api/pedidos-medicos | Criar novo pedido médico | VET |
| PUT | /api/pedidos-medicos/{id} | Atualizar pedido médico | VET |
| DELETE | /api/pedidos-medicos/{id} | Deletar pedido médico | VET |

### Wearable e lembretes

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | /api/leituras-wearable | Listar leituras wearable (paginado) | ambos |
| GET | /api/leituras-wearable/{id} | Buscar leitura wearable por ID | ambos |
| POST | /api/leituras-wearable | Criar nova leitura wearable | VET |
| PUT | /api/leituras-wearable/{id} | Atualizar leitura wearable | VET |
| DELETE | /api/leituras-wearable/{id} | Deletar leitura wearable | VET |
| GET | /api/lembretes | Listar lembretes (paginado) | ambos |
| GET | /api/lembretes/{id} | Buscar lembrete por ID | ambos |
| POST | /api/lembretes | Criar novo lembrete | ambos |
| PUT | /api/lembretes/{id} | Atualizar lembrete | ambos |
| PATCH | /api/lembretes/{id}/status?status= | Atualizar status do lembrete | ambos |
| DELETE | /api/lembretes/{id} | Deletar lembrete | ambos |

### Clínica e responsáveis

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | /api/veterinarios | Listar todos os veterinários (paginado) | ambos |
| GET | /api/veterinarios/{id} | Buscar veterinário por ID | ambos |
| POST | /api/veterinarios | Criar novo veterinário | VET |
| PUT | /api/veterinarios/{id} | Atualizar veterinário | VET |
| DELETE | /api/veterinarios/{id} | Deletar veterinário | VET |
| GET | /api/unidades | Listar todas as unidades (paginado) | ambos |
| GET | /api/unidades/{id} | Buscar unidade por ID | ambos |
| POST | /api/unidades | Criar nova unidade | VET |
| PUT | /api/unidades/{id} | Atualizar unidade | VET |
| DELETE | /api/unidades/{id} | Deletar unidade | VET |
| GET | /api/responsaveis | Listar responsáveis (paginado) | ambos |
| GET | /api/responsaveis/{id} | Buscar responsável por ID | ambos |
| GET | /api/responsaveis/buscar?cpf= | Buscar responsável por CPF | ambos |
| POST | /api/responsaveis | Criar novo responsável | ambos |
| PUT | /api/responsaveis/{id} | Atualizar responsável | só o titular |
| DELETE | /api/responsaveis/{id} | Deletar responsável | só o titular |
| GET | /api/responsaveis/{id}/contatos | Listar contatos do responsável | ambos |
| GET | /api/responsaveis/{id}/contatos/{contatoId} | Buscar contato por ID | ambos |
| POST | /api/responsaveis/{id}/contatos | Adicionar contato | só o titular |
| PUT | /api/responsaveis/{id}/contatos/{contatoId} | Atualizar contato | só o titular |
| DELETE | /api/responsaveis/{id}/contatos/{contatoId} | Remover contato | só o titular |
| GET | /api/responsaveis/{id}/enderecos | Listar endereços do responsável | ambos |
| GET | /api/responsaveis/{id}/enderecos/{enderecoId} | Buscar endereço por ID | ambos |
| POST | /api/responsaveis/{id}/enderecos | Adicionar endereço | só o titular |
| PUT | /api/responsaveis/{id}/enderecos/{enderecoId} | Atualizar endereço | só o titular |
| DELETE | /api/responsaveis/{id}/enderecos/{enderecoId} | Remover endereço | só o titular |

## Estrutura do Projeto

```
fiap.pethub
├── PethubApplication.java       → Classe principal, OpenAPI e @EnableScheduling
│
├── config/                      → Configurações da aplicação
│   ├── CacheConfig.java         → Cache Caffeine
│   ├── ConfiguracaoDotenv.java  → Carrega o .env como fonte de configuração
│   ├── SecurityConfig.java      → Regras de acesso por rota e perfil, BCrypt
│   └── WebConfig.java           → Configurações Web e CORS
│
├── security/                    → Autenticação JWT
│   ├── JwtService.java          → Emite e verifica os tokens
│   ├── JwtAuthenticationFilter.java → Lê o cabeçalho Authorization a cada requisição
│   ├── UsuarioDetailsService.java   → Carrega veterinário ou responsável pelo e-mail
│   ├── UsuarioAutenticado.java  → Principal com id e perfil
│   └── EscopoDoUsuario.java     → Regra de posse: o que o usuário pode enxergar
│
├── controller/                  → Controladores (endpoints REST)
│   ├── web/                     → Controladores Web (Thymeleaf)
│   │   ├── WebAuthController.java
│   │   ├── WebTutorController.java
│   │   └── WebVetController.java
│   ├── AuthController.java
│   ├── PetsController.java
│   ├── ConsultasController.java
│   ├── ExamesController.java
│   ├── DiagnosticosController.java
│   ├── VacinasTratamentosController.java
│   ├── PedidosMedicosController.java
│   ├── LeiturasWearableController.java
│   ├── LembretesController.java
│   ├── VeterinariosController.java
│   ├── UnidadesVeterinarioController.java
│   └── ResponsaveisController.java
│
├── dto/                         → Data Transfer Objects
│   ├── request/                 → DTOs de entrada (validação)
│   │   ├── LoginRequest.java
│   │   ├── CreatePetRequest.java
│   │   └── ...
│   └── response/                → DTOs de saída
│       ├── LoginResponse.java
│       ├── PetResponse.java
│       └── ...
│
├── entity/                      → Entidades JPA
│   ├── Pet.java
│   ├── Consulta.java
│   ├── Exame.java
│   ├── Diagnostico.java
│   ├── VacinaTratamento.java
│   ├── PedidoMedico.java
│   ├── LeituraWearable.java
│   ├── Lembrete.java
│   ├── Veterinario.java
│   ├── UnidadeVeterinario.java
│   ├── Responsavel.java
│   ├── ResponsavelContato.java
│   └── ResponsavelEndereco.java
│
├── enums/                       → Enumerações
│   ├── Perfil.java              → VETERINARIO e RESPONSAVEL
│   ├── StatusConsulta.java
│   ├── StatusLembrete.java
│   ├── StatusPedidoMedico.java
│   ├── TipoAlertaHidratacao.java
│   ├── TipoConsulta.java
│   ├── TipoLembrete.java
│   ├── TipoPedidoMedico.java
│   └── TipoVacinaTratamento.java
│
├── exception/                   → Tratamento de exceções
│   ├── GlobalExceptionHandler.java  → Handler centralizado
│   ├── AcessoNegadoException.java
│   ├── EmailJaCadastradoException.java
│   ├── RegraDeNegocioException.java → Recusa de regra de negócio (409)
│   ├── ResourceNotFoundException.java
│   ├── ErrorResponse.java
│   ├── ValidationErrorResponse.java
│   └── FieldErrorDetail.java
│
├── mapper/                      → Mapeadores MapStruct
│   ├── PetMapper.java
│   ├── ConsultaMapper.java
│   └── ...
│
├── repository/                  → Repositórios JPA
│   ├── PetRepository.java
│   ├── ConsultaRepository.java
│   └── ...
│
├── service/                     → Lógica de negócio
│   ├── AuthService.java
│   ├── ConsultaService.java     → Agendamento e suas guardas de agenda
│   ├── ProntuarioService.java   → Fecha a consulta numa transação só
│   ├── LeituraWearableService.java → Rotina diária de hidratação
│   ├── RemocaoEmCascata.java    → Remoção coerente dos dependentes
│   ├── PetService.java
│   └── ...
│
└── resources/
    ├── application.properties   → Configurações da aplicação
    ├── db/migration/            → Migrations do Flyway
    │   └── V1__schema_inicial.sql
    ├── static/
    │   └── css/app.css          → Folha de estilo das telas
    └── templates/               → Telas do Frontend (Thymeleaf)
        ├── login.html
        ├── registrar-tutor.html
        ├── registrar-vet.html
        ├── error.html           → Página de erro, inclui o 403 de perfil errado
        ├── fragments/
        │   └── campos.html      → Fragmento reusado pelos formulários
        ├── tutor/
        │   ├── dashboard.html
        │   ├── novo-pet.html
        │   ├── editar-pet.html
        │   ├── pet-detalhes.html
        │   ├── lembretes.html
        │   └── perfil.html
        └── vet/
            ├── dashboard.html
            ├── nova-consulta.html
            ├── consulta-detalhes.html
            └── perfil.html
```

## Requisitos Técnicos Atendidos

### Sprint 3

| Requisito | Status | Evidência |
|---|---|---|
| Flyway para versionamento do banco | ✅ | `V1__schema_inicial.sql` com 13 tabelas, 13 sequences e 20 FKs; `ddl-auto=validate` |
| Spring Security — dois tipos de usuário | ✅ | Perfis `VETERINARIO` e `RESPONSAVEL`, autenticação JWT com senha em BCrypt |
| Spring Security — rotas protegidas por perfil | ✅ | `SecurityConfig` por rota e método, mais posse por recurso em `EscopoDoUsuario` |
| Dois fluxos completos além do CRUD | ✅ | [Agendamento com validação de agenda](#1-agendamento-de-consulta-com-validação-de-agenda) (`ConsultaService`) e [fechamento transacional do prontuário](#2-registro-do-prontuário-e-fechamento-da-consulta) (`ProntuarioService`), mais a rotina agendada de hidratação e três lembretes automáticos |
| Validações nos dados de entrada | ✅ | Bean Validation nos DTOs (erro 400 por campo) e regras de negócio em serviço (erro 409) |
| Frontend com UI/UX | ✅ | 15 templates Thymeleaf (14 telas mais um fragmento reusado), todos listados e com print em [Mapa das páginas](#mapa-das-páginas-frontend-thymeleaf) |

### Sprints anteriores

| Requisito | Status | Evidência |
|---|---|---|
| Validação de campos com Bean Validation | ✅ | @NotNull, @NotBlank, @Size em DTOs |
| Paginação de resultados | ✅ | Page<T> + Pageable em endpoints |
| Ordenação de resultados | ✅ | sort parameter em Pageable |
| Busca com parâmetros | ✅ | 12+ filtros (nome, cpf, status, etc) |
| Uso de cache | ✅ | Caffeine Cache com @Cacheable |
| Tratamento de erros/exceções | ✅ | GlobalExceptionHandler com 6+ handlers |
| Utilização de DTOs | ✅ | 15+ DTOs request/response + MapStruct |
| Documentação com Swagger | ✅ | OpenAPI 3.0 + Swagger UI |

## Prints das telas (frontend Thymeleaf)

Capturas da aplicação rodando em `http://localhost:8080`, com os dados de demonstração
carregados. Elas comprovam a integração do Spring Boot com o Thymeleaf: cadastro, login por
sessão (Spring Security), navegação protegida por perfil e os fluxos de negócio.

### Públicas

**01 — Login** (`/login`)

![Tela de login](docs/img/01-login.png)

**02 — Cadastro de tutor** (`/registrar/tutor`)

![Cadastro de tutor](docs/img/02-registrar-tutor.png)

**03 — Cadastro de veterinário** (`/registrar/vet`)

![Cadastro de veterinário](docs/img/03-registrar-vet.png)

### Tutor

**04 — Painel do tutor** (`/tutor/dashboard`) — lista só os pets sob sua responsabilidade

![Painel do tutor](docs/img/04-tutor-dashboard.png)

**05 — Novo pet** (`/tutor/pets/novo`)

![Formulário de novo pet](docs/img/05-tutor-novo-pet.png)

**06 — Prontuário do pet** (`/tutor/pets/{id}`) — o tutor lê o que o veterinário registrou

![Prontuário do pet](docs/img/06-tutor-pet-detalhes.png)

**07 — Editar e excluir pet** (`/tutor/pets/{id}/editar`)

![Edição de pet](docs/img/07-tutor-editar-pet.png)

**08 — Lembretes** (`/tutor/lembretes`) — gerados pelo sistema, não cadastrados à mão

![Lembretes do tutor](docs/img/08-tutor-lembretes.png)

**09 — Perfil do tutor** (`/tutor/perfil`) — atualizar e excluir a própria conta

![Perfil do tutor](docs/img/09-tutor-perfil.png)

**10 — Controle de acesso por perfil** — tutor logado tentando abrir `/vet/dashboard`

![Acesso negado 403](docs/img/10-acesso-negado.png)

### Veterinário

**11 — Painel do veterinário** (`/vet/dashboard`) — a agenda de consultas

![Painel do veterinário](docs/img/11-vet-dashboard.png)

**12 — Agendar consulta** (`/vet/consultas/nova`) — [fluxo 1](#1-agendamento-de-consulta-com-validação-de-agenda), com validação de agenda

![Nova consulta](docs/img/12-vet-nova-consulta.png)

**13 — Registrar prontuário** (`/vet/consultas/{id}`) — [fluxo 2](#2-registro-do-prontuário-e-fechamento-da-consulta): diagnóstico, pedido médico e conclusão numa transação só

![Formulário de prontuário](docs/img/13-vet-prontuario-form.png)

**14 — Consulta já concluída** — a mesma tela recusa um segundo prontuário

![Consulta concluída](docs/img/14-vet-consulta-fechada.png)

**15 — Perfil do veterinário** (`/vet/perfil`)

![Perfil do veterinário](docs/img/15-vet-perfil.png)

## Prints do Swagger UI (todos os endpoints)

Documentação interativa em [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html),
gerada pelo Springdoc a partir das anotações `@Operation` e `@ApiResponses` dos controllers.

**Visão geral — os 12 grupos e todos os endpoints numa página só**

![Swagger UI completo](docs/img/16-swagger-visao-geral.png)

Cada grupo em detalhe:

**Autenticação** — login e os dois cadastros públicos

![Swagger Autenticação](docs/img/17-07-swagger-autenticacao.png)

**Pets**

![Swagger Pets](docs/img/17-10-swagger-pets.png)

**Consultas**

![Swagger Consultas](docs/img/17-01-swagger-consultas.png)

**Diagnósticos**

![Swagger Diagnósticos](docs/img/17-09-swagger-diagnosticos.png)

**Exames**

![Swagger Exames](docs/img/17-11-swagger-exames.png)

**Pedidos médicos**

![Swagger Pedidos Médicos](docs/img/17-05-swagger-pedidos-medicos.png)

**Vacinas e tratamentos**

![Swagger Vacinas e Tratamentos](docs/img/17-12-swagger-vacinas-e-tratamentos.png)

**Leituras wearable**

![Swagger Leituras Wearable](docs/img/17-04-swagger-leituras-wearable.png)

**Lembretes**

![Swagger Lembretes](docs/img/17-02-swagger-lembretes.png)

**Veterinários**

![Swagger Veterinários](docs/img/17-03-swagger-veterinarios.png)

**Unidades veterinárias**

![Swagger Unidades Veterinárias](docs/img/17-08-swagger-unidades-veterinarias.png)

**Responsáveis** — inclui contatos e endereços aninhados

![Swagger Responsáveis](docs/img/17-06-swagger-responsaveis.png)


## Prints das chamadas (Postman) — sprints anteriores

Fluxo seguindo as regras de negócio da solução, na ordem em que as entidades dependem umas das
outras. **As capturas são da Sprint 2, anteriores ao Spring Security**: para reproduzi-las hoje,
autentique-se antes (ver [Acesso à aplicação](#acesso-à-aplicação)) e envie o cabeçalho
`Authorization: Bearer <token>` em todas as chamadas. As coleções estão em `docs/`
(`PetHub API.postman_collection.json` e `PetHub_Sprint3.postman_collection.json`).

### 01 - Criar unidade veterinaria - `POST /api/unidades`
<img width="363" height="571" alt="image" src="https://github.com/user-attachments/assets/584b69f9-941f-45be-badf-9bffca58c44f" />

### 02 - Criar veterinario - `POST /api/veterinarios`
<img width="376" height="557" alt="image" src="https://github.com/user-attachments/assets/e44c2f88-2d4c-4f64-b62f-1c540a289cea" />

### 03 - Criar responsavel - `POST /api/responsaveis`
<img width="410" height="505" alt="image" src="https://github.com/user-attachments/assets/32113249-fd3d-42cc-b5a2-01457aa3d179" />

### 04 - Adicionar contato do responsavel - `POST /api/responsaveis/{id}/contatos`
<img width="375" height="398" alt="image" src="https://github.com/user-attachments/assets/06355eef-2767-439e-bbf6-c97609fe8e78" />

### 05 - Adicionar endereco do responsavel - `POST /api/responsaveis/{id}/enderecos`
<img width="387" height="574" alt="image" src="https://github.com/user-attachments/assets/b9b2ec26-44a8-4894-acb1-d7ef0acaa112" />

### 06 - Criar pet - `POST /api/pets`
<img width="443" height="572" alt="image" src="https://github.com/user-attachments/assets/c6b7f33d-8694-4f31-acc5-8e710b475d52" />

### 07 - Criar consulta - `POST /api/consultas`
<img width="376" height="538" alt="image" src="https://github.com/user-attachments/assets/aa97af95-9398-4760-aa5c-0eade88b003e" />

### 08 - Criar diagnostico - `POST /api/diagnosticos`
<img width="408" height="902" alt="image" src="https://github.com/user-attachments/assets/18099359-a6e6-4e02-8d8c-6dea54bf9f60" />

### 09 - Criar exame - `POST /api/exames`
<img width="411" height="493" alt="image" src="https://github.com/user-attachments/assets/0bc54a79-ad0b-4728-9a49-4753e00ed110" />

### 10 - Criar pedido medico - `POST /api/pedidos-medicos`
<img width="412" height="519" alt="image" src="https://github.com/user-attachments/assets/94be250e-0e62-4181-bbde-705bf7025646" />

### 11 - Criar vacina/tratamento - `POST /api/vacinas-tratamentos`
<img width="435" height="564" alt="image" src="https://github.com/user-attachments/assets/040d7cf3-37c2-4d4c-8fff-72a16ec0ea72" />

### 12 - Criar leitura wearable - `POST /api/leituras-wearable`
<img width="356" height="506" alt="image" src="https://github.com/user-attachments/assets/20eee015-ca0c-4983-be4d-511dafbcafbe" />

### 13 - Criar lembrete - `POST /api/lembretes`
<img width="419" height="608" alt="image" src="https://github.com/user-attachments/assets/0eab360e-61bf-4e13-a560-0b8e48bc1f7a" />

### 14 - Atualizar status do lembrete - `PATCH /api/lembretes/1/status?status={status}`
<img width="440" height="547" alt="image" src="https://github.com/user-attachments/assets/2e197512-fd7c-4a60-b587-2ac3af0548ad" />

> Listagem para confirmação de informações

### Buscar Responsável por CPF - `GET /api/responsaveis/buscar?cpf={CPF}`
<img width="452" height="797" alt="image" src="https://github.com/user-attachments/assets/aac50b5a-ea0a-48cf-9e2e-ab84564ae394" />

### Buscar Responsável por ID - `GET /api/responsaveis/{id}`
<img width="449" height="767" alt="image" src="https://github.com/user-attachments/assets/8b6c9ba9-51cd-410e-865e-21f6bdd86af2" />

### Listando Unidades Vets junto com os Veterinarios registrados - `GET /api/unidades`
<img width="376" height="886" alt="image" src="https://github.com/user-attachments/assets/dc1b7172-d109-4b4a-a817-260dd4200321" />
