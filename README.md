# PetHub API (serviço clínico, Java)

Uma solução completa para o cuidado veterinário, conectando pets, responsáveis e clínicas em uma jornada inteligente: do agendamento de consultas ao controle de vacinas, exames, diagnósticos e lembretes, tudo em um só lugar.

Projeto entregue para a disciplina **Java Advanced** (FIAP, Challenge 2026, 2º semestre, **Sprint 4**).
Este repositório é o **serviço clínico** do PetHub, um dos dois microsserviços da solução: o Java cuida do
veterinário, dos pets, das consultas, do prontuário e da IA; o serviço **.NET (Vitalis)** cuida do responsável
e dos lembretes. Veja a [arquitetura de microsserviços](#arquitetura-de-microsserviços).

## Entrega

| Artefato | Link |
|---|---|
| Repositório GitHub (Java) | https://github.com/vitalis-sa/pethub-java |
| Repositório GitHub (.NET) | https://github.com/vitalis-sa/PetHub-.NET |
| Repositório GitHub (aplicativo mobile) | https://github.com/FIAP-MOBILE-2026-Fevereiro/2tdspv-challenge-vitalis |
| Vídeo de apresentação (Sprint 4) | https://youtu.be/fIsw9z4U3s8 |
| Aplicação online: serviço Java (clínico) | https://pethub-java.cloud.run |
| Swagger do Java no ar | https://pethub-java.cloud.run/swagger-ui.html |
| Aplicação online: serviço .NET (Vitalis, responsável) | https://pethub-dotnet.cloud.run |
| Swagger do .NET no ar | https://pethub-dotnet.cloud.run/swagger |
| Coleção Postman para testar o fluxo | [`docs/PetHub_Sprint4.postman_collection.json`](docs/PetHub_Sprint4.postman_collection.json) |

## Grupo

| Nome | RM |
|---|---|
| Ana Flávia Camelo | RM561489 |
| Gustavo Kenji Terada | RM562745 |
| João Guilherme Carvalho Novaes | RM566234 |
| Pedro Chasci Puga | RM565154 |
| Lucas Figueiredo Vieira | RM561342 |

### Colaboração por disciplina

| Disciplina | Quem trabalhou |
|---|---|
| Java | Ana Flávia, João Guilherme e Lucas |
| Database | Lucas |
| IoT | Ana Flávia |
| .NET | Pedro e Ana Flávia |
| Mobile | Gustavo, Ana Flávia e João |
| QA | Gustavo, Pedro e Lucas |

## Tecnologias

| Tecnologia | Descrição |
|---|---|
| Java 21 | Linguagem principal do projeto, usando recursos modernos como `var`, text blocks e `switch` de expressão |
| Spring Boot 4.0.6 | Framework base que auto-configura o servidor embutido (Tomcat), gerencia beans e simplifica a criação da API REST |
| Maven | Gerenciador de dependências e build do projeto; responsável por baixar bibliotecas e compilar o código |
| Spring Data JPA | Abstrai o acesso ao banco de dados, gerando automaticamente as queries SQL a partir de interfaces como JpaRepository |
| Oracle Database (FIAP) | Banco de dados relacional utilizado para persistir os registros de pets, consultas, exames e outros dados da clínica |
| Flyway | Controle de versão do schema: aplica as migrations de `db/migration` no boot e registra o que já rodou na tabela `flyway_schema_history` |
| Spring Security | Autenticação e autorização da API: filtro JWT stateless, regras de acesso por rota e perfil, e BCrypt para o hash das senhas |
| JJWT (jjwt-api/impl/jackson) | Emissão e verificação dos tokens JWT assinados em HS256 |
| Caffeine Cache | Cache in-memory com expiração de 10 minutos e limite de 500 entradas para otimizar leituras frequentes |
| Lombok | Elimina código repetitivo (boilerplate) gerando automaticamente getters, setters, construtores e toString via anotações como @Data, @RequiredArgsConstructor |
| MapStruct | Gera o código de conversão entre entidades e DTOs (entrada/saída) sem precisar escrever mapeamentos manualmente |
| Spring Validation | Valida os dados recebidos nas requisições com anotações como @NotBlank, @NotNull e @Size, retornando erros 400 automaticamente quando inválidos |
| Springdoc OpenAPI | Gera documentação automaticamente e fornece interface Swagger UI para testar os endpoints |
| Spring Scheduling | Rotina diária que reenvia os e-mails de confirmação que falharam (`@Scheduled`) |
| Spring AMQP + RabbitMQ (CloudAMQP) | O Java **produz** a mensagem do lembrete; o serviço .NET consome. A fila é durável e a mensagem só sai depois do commit |
| Brevo (API HTTP) | E-mail de confirmação da consulta, enviado direto no fluxo normal, com status `ENVIADO`/`FALHOU` |
| Google Cloud Storage | Exames (PDF) em `exames/` e fotos de pets em `fotos/` no bucket `vitalis-docs`: o Java só assina URLs temporárias, o app envia/baixa direto |
| Groq (API REST, compatível com a da OpenAI) | Anamnese, receita e sugestão a partir da transcrição da consulta; resumo do texto de exames |
| RestClient | Cliente HTTP do Spring usado para Groq, Brevo e para o serviço .NET de responsáveis |

## Arquitetura de microsserviços

A solução deixou de ser um serviço único. Agora são **dois microsserviços**, cada um dono do seu domínio e
do seu código, mais o aplicativo móvel que consome os dois:

| Serviço | Tecnologia | É dono de | Onde roda |
|---|---|---|---|
| **pethub-java** (este repositório) | Java 21, Spring Boot | Veterinário, pets, consultas, prontuário (diagnósticos, exames, pedidos médicos, vacinas e tratamentos), unidades e IA | Cloud Run: https://pethub-java.cloud.run |
| **Vitalis** | .NET 10 | Responsável (cadastro e login), endereços, contatos e lembretes | Cloud Run: https://pethub-dotnet.cloud.run |
| **Aplicativo** | Expo / React Native | Interface do veterinário e do responsável | Fala com os dois serviços |

Cada serviço cria **só as próprias tabelas**, então a ordem de subida não importa e nenhuma chave estrangeira
cruza os dois domínios.

```mermaid
flowchart LR
    App[Aplicativo Expo]
    Net[Vitalis .NET<br/>responsável e lembretes]
    Java[PetHub Java<br/>veterinário e clínica]
    Rabbit[(RabbitMQ<br/>CloudAMQP)]
    Oracle[(Oracle FIAP)]
    Bucket[(Cloud Storage)]
    Groq[Groq IA]
    Brevo[Brevo e-mail]

    App -->|login e cadastro do responsável| Net
    App -->|login do veterinário| Java
    App -->|responsável, pelo gateway /api/clinico| Net
    Net -->|JWT de serviço| Java
    Java -->|JWT de serviço: dados do responsável| Net
    Java -->|publica lembretes| Rabbit
    Rabbit -->|consome| Net
    Java --> Oracle
    Net --> Oracle
    Java -->|URLs assinadas| Bucket
    App -->|envia e baixa arquivos| Bucket
    Java --> Groq
    Java --> Brevo
```

### Como os serviços conversam

| Fluxo | Mecanismo | Detalhe |
|---|---|---|
| Autenticação entre serviços | JWT HS256 de 60 s no cabeçalho `X-Service-Authorization` | Emissor e público próprios de cada lado; o mesmo segredo nos dois (`SERVICE_JWT_SECRET` no Java, `ServicoAuth__Segredo` no .NET) |
| Responsável usa a parte clínica | Gateway `/api/clinico/**` do .NET | O .NET valida o token do responsável e repassa ao Java dizendo de quem é o pedido |
| Dados do responsável (nome, e-mail, CPF) | Java chama o .NET | Para criar pet e para enviar o e-mail de confirmação |
| Lembretes | Fila RabbitMQ | O Java publica, só depois do commit; o .NET consome e cria o lembrete |
| E-mail de confirmação da consulta | Brevo (HTTP) | Direto, sem fila; falha fica registrada e é reenviada às 6h |

### Integração com as outras disciplinas

| Disciplina | Quem | Onde aparece |
|---|---|---|
| Java | Ana Flávia, João Guilherme, Lucas | Este repositório: API REST, segurança, mensageria, e-mail, IA e armazenamento |
| Database | Lucas | Oracle da FIAP, migration `V1__schema_inicial.sql` (10 tabelas, 10 sequences, 16 chaves estrangeiras) |
| .NET | Pedro, Ana Flávia | Vitalis: responsável, endereço, contato, lembrete, consumidor da fila e gateway para o Java |
| Mobile | Gustavo, Ana Flávia, João | Aplicativo Expo que consome os dois serviços |
| QA | Gustavo, Pedro, Lucas | Testes automatizados deste repositório (`src/test`) e a [coleção Postman](#postman-fluxo-para-testar) |
| IoT | Ana Flávia | **(preencher: como o IoT se conecta ao PetHub)** |

## Interface

Este repositório é só a **API**: não há telas servidas pelo Java. As telas do PetHub agora são refletidas no
**aplicativo móvel**, como o [vídeo](https://youtu.be/fIsw9z4U3s8) explica. O veterinário entra pelo Java; o
responsável entra pelo .NET e usa a parte clínica pelo gateway.

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
| `SERVICE_JWT_SECRET` | sim, para a comunicação com o .NET | Segredo (mín. 32 caracteres) que assina o JWT de serviço (`X-Service-Authorization`), igual ao `ServicoAuth__Segredo` do .NET. Gere com `openssl rand -base64 48`. Vazio = nenhuma chamada de serviço é aceita nem emitida |
| `RESPONSAVEIS_URL` | sim, para criar pet e enviar e-mail | URL do serviço .NET de responsáveis |
| `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`, `RABBIT_PASSWORD`, `RABBIT_VHOST`, `RABBIT_SSL` | sim, para os lembretes | Dados da instância no CloudAMQP (porta 5671 com `RABBIT_SSL=true`). Sem RabbitMQ a consulta é gravada, mas o lembrete não sai |
| `BREVO_API_KEY`, `BREVO_REMETENTE_EMAIL` | sim, para o e-mail | Chave da Brevo e remetente verificado na conta. Sem eles o envio falha e fica marcado para o reenvio diário |
| `GROQ_API_KEY` (e `GROQ_MODEL`, opcional) | sim, para a IA | Chave do Groq e id do modelo (padrão `openai/gpt-oss-120b`). Sem a chave, as rotas de IA respondem `503`. |
| `GCS_BUCKET`, `GCP_PROJECT_ID`, `GOOGLE_APPLICATION_CREDENTIALS` | para exames e fotos | Bucket e credenciais do Cloud Storage (ver "Arquivos") |

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

Ao subir, a aplicação:

1. aplica as migrations do Flyway no schema (ver [Migrations](#migrations-flyway));
2. valida se as entidades JPA batem com as tabelas (`ddl-auto=validate`);
3. sobe em `http://localhost:8080`.

| Recurso | URL |
|---|---|
| API REST | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

## Acesso à aplicação

O PetHub Java é uma API REST consumida pelo aplicativo móvel e pelo serviço .NET. Há **duas formas de entrar**:

| Quem | Como entra | O que o Java enxerga |
|---|---|---|
| Veterinário | `POST /api/auth/login` devolve um JWT, enviado em `Authorization: Bearer` | o veterinário logado |
| Responsável | Autentica no serviço **.NET**, que chama o Java com `X-Service-Authorization: Bearer <jwt>` (JWT HS256 de 60 s assinado pelo .NET; o `sub` é quem pede) | o responsável daquele id, com as mesmas regras de posse |

O cadastro e o login de responsáveis, assim como os lembretes, **pertencem ao .NET**. O Java guarda
apenas o `responsavelId` dentro do pet.

Em um banco limpo não existe usuário pré-cadastrado: crie o veterinário pela API (veja
[Pela API](#pela-api)) ou rode a [coleção Postman](#postman-fluxo-para-testar), que cria tudo o que o fluxo precisa.

### Pela API

**Toda rota `/api/**` fora de `/api/auth/**` exige autenticação** (JWT do veterinário ou a credencial
de serviço do .NET). Sem ela, a resposta é `401`.

### 1. Criar uma conta

Não existe usuário pré-cadastrado — o primeiro acesso é o cadastro público do veterinário:

```bash
curl -X POST http://localhost:8080/api/auth/registrar/veterinario \
  -H "Content-Type: application/json" \
  -d '{"nome":"Dra. Ana","crmv":"SP-12345","email":"ana@pethub.com","senha":"senha123","especialidade":"Clínica geral","ativo":true}'
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
| `403` | Autenticado, mas o perfil não tem permissão para a operação | Use uma conta de veterinário |
| `404` | Recurso inexistente **ou** de outro responsável | Confira o id; um responsável não enxerga dado alheio |
| `409` | Regra de negócio recusou a operação | Leia a mensagem: agenda ocupada, prontuário já registrado ou e-mail em uso |
| `503` | IA, serviço de responsáveis ou outra dependência externa fora do ar ou não configurada | Confira as variáveis de ambiente e tente de novo |

## Perfis e permissões

São dois tipos de usuário, e a diferença entre eles segue o domínio: **o veterinário produz o
prontuário, o responsável o consulta** — e também agenda consultas e cadastra os próprios pets.

| | `VETERINARIO` | `RESPONSAVEL` |
|---|---|---|
| Como entra | `POST /api/auth/registrar/veterinario` e login com JWT | pelo .NET, com JWT de serviço (`sub` = responsável) |
| Prontuário (consultas, diagnósticos, exames, pedidos médicos, vacinas) | cria, edita e apaga | apenas lê |
| Agendar consulta (`POST /api/consultas`) | qualquer pet; pode lançar consulta já realizada | só para os próprios pets, e sempre como `AGENDADA` |
| Cadastrar pet (`POST /api/pets`) | para o responsável do CPF informado | sempre para si mesmo |
| Editar e apagar pets, veterinários e unidades | sim | não |
| IA (análise da consulta, resumo de exame) | gera | apenas lê a análise |
| Alcance das listagens | todos os registros da clínica | somente os pets sob sua responsabilidade |


## Fluxos completos (não-CRUD)

A rubrica pede **dois fluxos completos que não sejam CRUD**. Os dois primeiros abaixo são esses:
cada um tem regra de negócio própria, recusa o que não faz sentido e envolve mais de uma
entidade. Os demais rodam sozinhos, sem ninguém chamar um endpoint de lembrete.

### 1. Agendamento de consulta com validação de agenda

Marcar uma consulta não é gravar uma linha. Em
[`ConsultaService.create`](src/main/java/fiap/pethub/service/ConsultaService.java) o agendamento
confere a posse do pet (o responsável só agenda para os próprios), passa por duas guardas e só então avisa o responsável:

| Guarda | Regra | Se violar |
|---|---|---|
| Agenda livre | O mesmo veterinário não atende dois pets no mesmo horário. Consulta cancelada não ocupa agenda | `409` — "Este veterinário já tem uma consulta marcada neste horário." |
| Horário futuro | Só vale para o que está sendo **agendado**; lançar uma consulta já `REALIZADA` no passado continua permitido, porque registrar o que aconteceu é legítimo | `409` — "Não é possível agendar uma consulta para uma data que já passou." |

Passadas as guardas, a consulta é salva. **Só depois do commit** saem os dois avisos, nesta ordem de
responsabilidade:

1. o **lembrete**, publicado na fila do RabbitMQ para o .NET criar (o Java não consome);
2. o **e-mail de confirmação**, enviado direto pela Brevo, sem fila. Se falhar, a consulta continua
   gravada, o e-mail fica marcado `FALHOU` em `tb_email_consulta` e o job das 6h tenta de novo as
   falhas do dia anterior.

Consulta lançada como `REALIZADA` ou `CANCELADA` não gera lembrete nem e-mail.

### 2. Atendimento com IA e fechamento da consulta

O veterinário atende, o aplicativo transcreve a conversa e manda **só o texto** ao Java. O fluxo tem três
passos, e nada é gravado antes da aprovação:

| Passo | Endpoint | O que acontece |
|---|---|---|
| 1. Rascunho | `POST /api/consultas/{id}/analise-ia` | `AnaliseConsultaService.gerarRascunho` chama o Groq e devolve anamnese, medicamentos, exames e sugestão. Consulta cancelada é recusada (`409`) |
| 2. Aprovação | `PUT /api/consultas/{id}/analise-ia` | `AnaliseConsultaService.aprovar` grava a análise, `ProntuarioDaConsulta.refletir` leva medicamentos para Tratamentos e exames para Exames, e a consulta passa a `REALIZADA` |
| 3. Receita | `GET /api/consultas/{id}/receita` | Veterinário e responsável dono do pet leem a receita aprovada |

Regras do fluxo:

- salvar de novo não duplica: itens com o mesmo nome são atualizados;
- medicamento tirado da receita sai de Tratamentos e as doses pendentes dele são canceladas no .NET;
- cada medicamento com `vezesPorDia` e `duracaoDias` gera a agenda de doses, e o .NET cria um lembrete por dose;
- consulta já realizada ou cancelada não aceita nova análise;
- a chamada ao modelo não segura transação do banco (o pool tem só 2 conexões).

### 3. Lembretes automáticos (pelo .NET)

O Java só **produz** a mensagem; quem cria o lembrete é o .NET, ao consumir a fila. Estas situações publicam,
sempre depois de gravar:

| Origem | Tipo do lembrete | Data |
|---|---|---|
| Consulta agendada ou remarcada | `CONSULTA` | dia da consulta |
| Vacina ou tratamento com `proximaDose` | `VACINA` | `proximaDose` |
| Pedido médico com `dataLimite` | `EXAME` ou `MEDICAMENTO`, conforme o pedido | `dataLimite` |
| Receita aprovada com `vezesPorDia` e `duracaoDias` | `MEDICAMENTO` (uma dose por horário) | a partir do horário da aprovação |
| Medicamento removido da receita | cancelamento das doses ainda não liberadas | sem data |

Detalhes do contrato em [Integração com o serviço .NET](#integração-com-o-serviço-net).

## Endpoints

Legenda da coluna **Acesso**: `público` dispensa token; `ambos` aceita os dois perfis; `VET` exige
o perfil `VETERINARIO`.

### Autenticação

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | /api/auth/login | Autenticar veterinário e receber o token JWT | público |
| POST | /api/auth/registrar/veterinario | Cadastrar veterinário e já receber o token | público |

### Pets e prontuário

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | /api/pets | Listar todos os pets (paginado) | ambos |
| GET | /api/pets/{id} | Buscar pet por ID | ambos |
| GET | /api/pets/responsavel?cpf= | Buscar pets por CPF do responsável | ambos |
| POST | /api/pets | Criar novo pet (responsável cadastra para si) | ambos |
| PUT | /api/pets/{id} | Atualizar pet por ID | VET |
| DELETE | /api/pets/{id} | Deletar pet por ID | VET |
| DELETE | /api/pets/responsavel/{responsavelId} | Apaga todos os pets do responsável com o histórico (o .NET chama antes de excluir o responsável) | ambos |
| GET | /api/consultas | Listar todas as consultas (paginado) | ambos |
| GET | /api/consultas/{id} | Buscar consulta por ID | ambos |
| POST | /api/consultas | Agendar consulta (responsável só para os próprios pets) | ambos |
| PUT | /api/consultas/{id} | Atualizar consulta | VET |
| DELETE | /api/consultas/{id} | Deletar consulta | VET |
| GET | /api/exames | Listar todos os exames (paginado) | ambos |
| GET | /api/exames/{id} | Buscar exame por ID | ambos |
| POST | /api/exames | Criar novo exame | VET |
| PUT | /api/exames/{id} | Atualizar exame | VET |
| DELETE | /api/exames/{id} | Deletar exame | VET |
| POST | /api/exames/{id}/resumo-ia | Resumir o texto de um exame com IA e gravar em `resumoIa` | VET |
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

### Clínica

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

## Inteligência Artificial (Groq)

O aplicativo converte a voz em texto com o reconhecimento do próprio aparelho (no navegador, o do navegador) e **nenhum áudio chega à API**; o PDF do exame também vira texto no aplicativo. A análise em si recebe somente texto e chama o Groq pela API REST (`/openai/v1/chat/completions`, modo JSON).

| Método | Rota | Descrição | Perfil |
|--------|------|-----------|--------|
| POST | /api/consultas/{id}/analise-ia | Gera o **rascunho** de anamnese, receita e sugestão a partir da transcrição (não grava nada) | VET |
| PUT | /api/consultas/{id}/analise-ia | Salva a anamnese e a receita que o veterinário aceitou ou corrigiu (substitui a anterior) | VET |
| GET | /api/consultas/{id}/analise-ia | Busca a anamnese e a receita salvas | VET |
| GET | /api/consultas/{id}/receita | Receita aprovada; o PDF é montado no aplicativo | VET e responsável do pet |
| POST | /api/exames/{id}/resumo-ia | Resume o texto do exame e grava em `resumoIa` | VET |
| PATCH | /api/consultas/{id}/remarcar | Muda a data/hora de uma consulta agendada (futuro, agenda livre); o lembrete no .NET é atualizado | VET e responsável do pet |
| PUT | /api/consultas/{id}/queixas | Registra as queixas do responsável antes da consulta | VET e responsável do pet |

- Toda a análise é **exclusiva do veterinário**. O rascunho não é gravado; só o que ele aceitou ou corrigiu (`PUT`: anamnese, medicamentos, exames e a sugestão) vai para o banco, reflete em Tratamentos e Exames do pet e marca a consulta como **realizada** (depois disso ela não pode ser atendida de novo). A sugestão nunca é diagnóstico.
- O responsável só enxerga a receita **depois** da aprovação, em `GET /api/consultas/{id}/receita`.
- **Onde fica o prompt:** as instruções estão em `src/main/resources/prompts/*.txt` e são ligadas ao formato da resposta em [`PromptIa`](src/main/java/fiap/pethub/ia/PromptIa.java). O texto que veio do app vai à parte, dentro de uma tag (`<transcricao>`), como material a processar. O [`GroqClient`](src/main/java/fiap/pethub/ia/GroqClient.java) só envia e converte a resposta.
- Variáveis: `GROQ_API_KEY` (obrigatória), `GROQ_MODEL`, `GROQ_URL_BASE`, `GROQ_MAX_TOKENS` e `GROQ_TIMEOUT_SEGUNDOS` (opcionais). Sem a chave, as rotas respondem 503.
- A chave vai no cabeçalho `x-goog-api-key` e nunca é registrada em log. Falhas do serviço de IA viram 503 sem repassar o corpo do erro.
- A chamada ao modelo não segura transação do banco (o pool tem apenas 2 conexões).
- Consulta cancelada não aceita análise.
- Receita em PDF: o aplicativo monta o PDF a partir do texto aprovado (`expo-print`). O veterinário imprime na hora; o responsável baixa depois. Não há biblioteca de PDF no Java.

## Arquivos (Google Cloud Storage)

Os bytes nunca passam pelo Java. O fluxo tem três passos, iguais para exames e fotos:

| Passo | Exame (PDF) | Foto do pet |
|-------|-------------|-------------|
| 1. Pedir a URL de envio | `POST /api/exames/{id}/arquivo/envio` (VET) | `POST /api/pets/{id}/foto/envio` (VET ou responsável dono) |
| 2. Enviar | `PUT` do arquivo na URL devolvida, com o mesmo `Content-Type` | idem |
| 3. Confirmar | `PUT /api/exames/{id}/arquivo` com o `caminho` | `PUT /api/pets/{id}/foto` com o `caminho` |
| Baixar | `GET /api/exames/{id}/arquivo` (VET e responsável dono) | `GET /api/pets/{id}/foto` |

- Tipos aceitos: `application/pdf` em `exames/{exameId}/`; `image/jpeg`, `image/png` e `image/webp` em `fotos/{petId}/`. O banco guarda só o caminho (`arquivo_resultado`, `foto_path`); as respostas trazem `temArquivo`/`temFoto`.
- O `caminho` confirmado precisa estar na subpasta do próprio registro, então ninguém aponta um exame para o arquivo de outro.
- Credenciais: o SDK usa as credenciais padrão do Google. Localmente, `GOOGLE_APPLICATION_CREDENTIALS` aponta para o JSON de uma conta de serviço com acesso ao bucket e permissão de assinar URLs (papel *Service Account Token Creator* nela mesma); no Cloud Run vale a conta de serviço do próprio serviço. Uma chave de API **não** serve para o Storage.
- **Modo local** (`ARMAZENAMENTO_MODO=local`): grava em `ARMAZENAMENTO_PASTA` (padrão `./arquivos-locais`, ignorada pelo git) e imita as URLs assinadas com HMAC em `/api/arquivos/**`. Serve para desenvolver sem conta do Google; `ARMAZENAMENTO_URL_PUBLICA` deve ser o endereço pelo qual o **app** alcança o Java (IP da máquina no celular, `http://10.0.2.2:8080` no emulador Android).
- Variáveis: `GCS_BUCKET` (padrão `vitalis-docs`), `GCP_PROJECT_ID` (padrão `vitalis-pethub`), `GCS_VALIDADE_URL_MIN` (padrão 15).
- O bucket precisa de CORS liberando `PUT`/`GET` e o cabeçalho `Content-Type` para o app web enviar direto.

## Integração com o serviço .NET

O .NET (Vitalis) é dono de responsável, endereço, contato e lembrete. O Java é dono de pet, consulta,
prontuário e IA. Cada serviço cria **só as próprias tabelas**, então a ordem de subida não importa e
nenhuma chave estrangeira cruza os bancos.

| Fluxo | Direção | Como |
|---|---|---|
| Responsável agenda ou cadastra pet | .NET → Java | JWT de serviço (`sub` = responsável); o Java aplica as regras de posse |
| Cadastro de responsável (CPF, nome, e-mail) | Java → .NET | `GET /api/responsavel/{id}` e `/api/responsavel/buscar?cpf=` com JWT de serviço emitido pelo Java |
| Lembrete | Java → fila → .NET | mensagem JSON no RabbitMQ |
| Exclusão de responsável | .NET → Java | `DELETE /api/pets/responsavel/{id}` com JWT de serviço cujo `sub` é o id; o .NET só apaga o responsável se o Java confirmar |

**Mensagem do lembrete**: exchange `pethub.lembretes.exchange` (direct, durável), routing key
`lembrete.criar`, fila `pethub.lembretes.queue` (durável). O corpo tem os campos do `CriarLembreteDto`
do .NET. Lembrete de um dia só (consulta, vacina, exame):

```json
{
  "responsavelId": 7, "petId": 5, "tipo": "CONSULTA", "dataAgendada": "2026-11-01",
  "mensagem": "Consulta agendada para Rex em 01/11/2026 às 14:30",
  "referenciaId": 100, "referenciaTipo": "Consulta"
}
```

Medicamento com agenda de doses (o .NET cria um lembrete por dose) e cancelamento de doses:

```json
{
  "responsavelId": 7, "petId": 5, "tipo": "MEDICAMENTO", "dataAgendada": "2026-11-01",
  "mensagem": "Ondansetrona 4 mg, comprimido", "referenciaId": 31, "referenciaTipo": "VacinaTratamento",
  "vezesPorDia": 2, "duracaoDias": 3, "inicio": "2026-11-01T09:00:00"
}
```

```json
{ "responsavelId": 7, "petId": 5, "tipo": "MEDICAMENTO", "mensagem": "Tratamento removido",
  "referenciaId": 31, "referenciaTipo": "VacinaTratamento", "cancelar": true }
```

`tipo` vai como texto (`CONSULTA`, `VACINA`, `EXAME`, `MEDICAMENTO`); o .NET deserializa enum por nome.
`referenciaTipo` + `referenciaId` identificam a origem, e o consumidor os usa para não criar o mesmo
lembrete duas vezes numa reentrega: se a mensagem chega de novo, o lembrete é atualizado.


## Deploy (Google Cloud Run)

O Java roda no **Cloud Run** (projeto `vitalis-pethub`, região `southamerica-east1`), ligado ao GitHub: cada
push na `main` dispara um build com o `Dockerfile` da raiz e publica uma nova revisão.

| Item | Valor |
|---|---|
| URL do serviço | https://pethub-java.cloud.run |
| Imagem | `Dockerfile` da raiz (copia `pom.xml` e `lombok.config`, empacota o jar e roda com `java -jar`) |
| Banco | Oracle da FIAP; no primeiro deploy o schema precisa estar limpo para o Flyway aplicar a `V1` |
| Arquivos | Bucket `vitalis-docs` (Cloud Storage) |
| Fila | RabbitMQ no CloudAMQP |

Variáveis de ambiente do serviço (os valores ficam nas configurações do Cloud Run, nunca no repositório):
`DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `SERVICE_JWT_SECRET`, `RESPONSAVEIS_URL`, `RABBIT_HOST`, `RABBIT_PORT`,
`RABBIT_USER`, `RABBIT_PASSWORD`, `RABBIT_VHOST`, `RABBIT_SSL`, `BREVO_API_KEY`, `BREVO_REMETENTE_EMAIL`,
`GROQ_API_KEY`, `GCS_BUCKET` e `GCP_PROJECT_ID`.

Para o bucket funcionar em produção:

1. A conta de serviço do Cloud Run precisa de acesso ao bucket e do papel **Service Account Token Creator** nela
   mesma, para assinar as URLs temporárias (a API *IAM Service Account Credentials* precisa estar ativada).
2. O bucket precisa de **CORS** liberando `PUT` e `GET` e o cabeçalho `Content-Type`, para o app web enviar direto.

`SERVICE_JWT_SECRET` deve ter o **mesmo valor** do `ServicoAuth__Segredo` do .NET.

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
│   ├── ServicoAuthenticationFilter.java → JWT de serviço do .NET (X-Service-Authorization)
│   ├── UsuarioDetailsService.java   → Carrega o veterinário pelo e-mail
│   ├── UsuarioAutenticado.java  → Principal com id e perfil
│   └── EscopoDoUsuario.java     → Regra de posse: o que o usuário pode enxergar
│
├── controller/                  → Controladores (endpoints REST)
│   ├── AuthController.java
│   ├── PetsController.java
│   ├── ConsultasController.java
│   ├── ExamesController.java
│   ├── DiagnosticosController.java
│   ├── VacinasTratamentosController.java
│   ├── PedidosMedicosController.java
│   ├── IaController.java
│   ├── VeterinariosController.java
│   ├── UnidadesVeterinarioController.java
│
├── dto/                         → Data Transfer Objects
│   ├── request/                 → DTOs de entrada (validação)
│   │   ├── LoginRequest.java
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
│   ├── AnaliseConsulta.java
│   ├── EmailConsulta.java
│   ├── Veterinario.java
│   └── UnidadeVeterinario.java
│
├── enums/                       → Enumerações
│   ├── Perfil.java              → VETERINARIO e RESPONSAVEL
│   ├── StatusConsulta.java
│   ├── StatusEmail.java
│   ├── StatusPedidoMedico.java
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
├── ia/                          → Groq: prompts, cliente HTTP e serviços de IA
├── messaging/                   → Produtor RabbitMQ (fila de lembretes)
├── email/                       → Confirmação por e-mail (Brevo) e reenvio diário
├── integracao/                  → Cliente do serviço .NET de responsáveis e JWT de serviço
├── armazenamento/               → URLs assinadas do Cloud Storage (ou disco, em modo local)
│
├── service/                     → Lógica de negócio
│   ├── AuthService.java
│   ├── ConsultaService.java     → Agendamento e suas guardas de agenda
│   ├── RemocaoEmCascata.java    → Remoção coerente dos dependentes
│   ├── PetService.java
│   └── ...
│
└── resources/
    ├── prompts/                 → Instruções das funções de IA (texto puro)
    ├── application.properties   → Configurações da aplicação
    ├── db/migration/            → Migrations do Flyway
    │   └── V1__schema_inicial.sql
```

## Requisitos Técnicos Atendidos

### Sprint 4

| Requisito do PDF | Como é atendido | Onde ver |
|---|---|---|
| Aplicação rodando online (deploy) | Java e .NET no Cloud Run | [Entrega](#entrega) e [Deploy](#deploy-google-cloud-run) |
| Principais fluxos do sistema | Agendamento, atendimento com IA, lembretes, e-mail e arquivos | [Fluxos completos](#fluxos-completos-não-crud) e a [coleção Postman](#postman-fluxo-para-testar) |
| Conceitos da disciplina no app criado | Segurança entre serviços, mensageria, e-mail, integração com IA, armazenamento, cache e Flyway | Seções do próprio README |
| Narrativa e decisões de design | Microsserviços por domínio, fila para o que cruza serviços, e-mail direto com reenvio | [Arquitetura de microsserviços](#arquitetura-de-microsserviços) |
| Integração multidisciplinar | Database, .NET, Mobile, QA e IoT | [Integração com as outras disciplinas](#integração-com-as-outras-disciplinas) |
| Todos os membros no vídeo | Vídeo de apresentação | https://youtu.be/fIsw9z4U3s8 |
| Repositório com README completo, link da aplicação e vídeo | Este arquivo | [Entrega](#entrega) |

### Sprint 3

| Requisito | Status | Evidência |
|---|---|---|
| Flyway para versionamento do banco | ✅ | `V1__schema_inicial.sql` com 10 tabelas, 10 sequences e 16 FKs; `ddl-auto=validate` |
| Spring Security — dois tipos de usuário | ✅ | Perfis `VETERINARIO` (JWT, senha em BCrypt) e `RESPONSAVEL` (credencial de serviço do .NET) |
| Spring Security — rotas protegidas por perfil | ✅ | `SecurityConfig` por rota e método, mais posse por recurso em `EscopoDoUsuario` |
| Dois fluxos completos além do CRUD | ✅ | [Agendamento com validação de agenda](#1-agendamento-de-consulta-com-validação-de-agenda) (`ConsultaService`) e [atendimento com IA e fechamento da consulta](#2-atendimento-com-ia-e-fechamento-da-consulta) (`AnaliseConsultaService`), mais lembretes automáticos por mensageria |
| Validações nos dados de entrada | ✅ | Bean Validation nos DTOs (erro 400 por campo) e regras de negócio em serviço (erro 409) |

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

## Evidências do deploy e das integrações

### Serviços no Google Cloud Run

<img width="1916" height="892" alt="image" src="https://github.com/user-attachments/assets/086c1fe7-45b6-46e7-89b9-1eae89f1f390" />


<img width="1919" height="925" alt="image" src="https://github.com/user-attachments/assets/0074b1f4-9f41-4352-9309-ecf0df7c4554" />


### Fila RabbitMQ (CloudAMQP)

<img width="1695" height="362" alt="image" src="https://github.com/user-attachments/assets/1e749126-a3e6-489d-b83d-7e12d08e12fe" />

<img width="1404" height="54" alt="image" src="https://github.com/user-attachments/assets/41814c59-13b0-4ea6-8683-1f1686ab1201" />

### E-mail de confirmação (Brevo)

<img width="797" height="408" alt="image" src="https://github.com/user-attachments/assets/be482434-43e6-4fd6-9033-94ad76528381" />

## Prints do Swagger UI (todos os endpoints)

Documentação interativa em [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html),
gerada pelo Springdoc a partir das anotações `@Operation` e `@ApiResponses` dos controllers.

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

**Veterinários**

![Swagger Veterinários](docs/img/17-03-swagger-veterinarios.png)

**Unidades veterinárias**

![Swagger Unidades Veterinárias](docs/img/17-08-swagger-unidades-veterinarias.png)

