# Roteiro do vídeo — PetHub Java Advanced (Sprint 3)

**Duração alvo: 9 minutos. Teto do enunciado: 10.** A margem existe porque vídeo sempre estica na
hora de gravar, e passar de 10 minutos é risco desnecessário numa entrega que já está pronta.

O enunciado pede: *"Vídeo demonstração da aplicação funcionando com apresentação das principais
funcionalidades da aplicação web (máx. 10min.)"*. Ou seja: **não é para percorrer todos os
endpoints.** São 55 endpoints REST; a 10 segundos cada, seriam 9 minutos de nada. Os prints de
todas as chamadas já estão no README — é lá que mora a cobertura exaustiva. O vídeo é outra coisa.

---

## 1. O que o vídeo precisa provar

A rubrica tem quatro itens. O roteiro abaixo cobre os quatro **na mesma ordem em que eles valem
ponto**, para que quem avalia não precise caçar nada.

| Item da rubrica | Pontos | Onde aparece no vídeo |
|---|---|---|
| Frontend (camada de visualização) | 30 | Blocos 2, 3, 4, 6, 7, 8 |
| Flyway | 20 | Bloco 1 |
| Spring Security (2 perfis, rotas protegidas) | 30 | Blocos 2, 4, 8 |
| Funcionalidades completas (2 fluxos não-CRUD) | 20 | Blocos 5, 6, 9 |

E há quatro penalidades que o vídeo pode **provocar** se for mal gravado:

| Penalidade | Como o vídeo evita |
|---|---|
| Página que não carrega ou link quebrado (-5 cada) | Clicar em todos os links antes de gravar (§3) |
| Funcionalidade com comportamento inesperado (-5) | Nunca improvisar um caminho não ensaiado |
| Apresentação incompleta ou incoerente (-15) | Cobrir os quatro itens da rubrica, em voz alta |
| SOLID / DRY / Clean Code | Bloco 1 e §5: mostrar que a separação é intencional |

---

## 2. Três decisões que orientam este roteiro

**a) Narre na ordem da rubrica e diga o nome do item em voz alta.**
Frases como *"este é o item Flyway"* e *"aqui está o segundo tipo de usuário"* parecem bobas, mas
eliminam a chance de o avaliador não encontrar algo que você entregou. Quem corrige assiste a
dezenas de vídeos; facilitar a conferência é ponto ganho.

**b) Mostre o código junto do efeito, não num bloco morto no começo.**
Um tour de dois minutos por pastas antes de qualquer coisa rodar é a parte mais chata de qualquer
vídeo, e é onde a atenção morre. O bloco 1 dá a visão geral em 75 segundos; depois disso, cada
trecho de código aparece no instante em que o efeito dele acabou de acontecer na tela. É mais
convincente e prepara melhor a avaliação oral.

**c) Mostre o caminho do erro, não só o feliz.**
Qualquer um grava um formulário que salva. Mostrar o formulário **recusando** o que não pode —
horário ocupado, prontuário duplicado, rota de outro perfil — é o que separa "tem tela" de "tem
sistema". É também o que o professor costuma perguntar quando não vê.

---

## 3. Antes de gravar — checklist

### Ambiente

- [ ] `.env` preenchido (`DB_USER`, `DB_PASSWORD`, `JWT_SECRET` com 32+ caracteres)
- [ ] **Nenhuma outra instância rodando.** O pool do Oracle da FIAP está em
      `maximum-pool-size=2`; uma segunda aplicação conectada derruba a gravação no meio
- [ ] Aplicação percorrida de ponta a ponta **uma vez inteira** antes de apertar REC
- [ ] `./mvnw test` verde (46 testes em 11 arquivos) — precisa do banco acessível

### Dados semeados (faça isto **antes**, fora do vídeo)

Digitar cadastro no ar queima dois dos dez minutos. Deixe pronto:

- [ ] **Tutor A** — o seu login de demonstração, com **um pet já com histórico**: uma consulta
      `REALIZADA`, com diagnóstico e um pedido médico. Serve para a tela de detalhes não abrir vazia
- [ ] **Tutor B** — com um pet próprio. Existe só para provar o isolamento por posse no bloco 8.
      Anote o **id do pet do Tutor B**; você vai precisar dele no Swagger
- [ ] **Veterinário** — o login do vet
- [ ] Uma **consulta já agendada** num horário X, para o bloco 5 poder mostrar o conflito de
      agenda sendo recusado
- [ ] Senhas iguais e simples nos três logins, para digitar rápido sem errar

### Abas e janelas

- [ ] Aba 1: aplicação (`http://localhost:8080`), logada como **tutor**
- [ ] Aba 2: **janela anônima** — é aqui que o veterinário entra, sem derrubar a sessão da aba 1
- [ ] Aba 3: Swagger (`/swagger-ui.html`), já com o **Authorize** feito com o token do veterinário
- [ ] Aba 4: repositório no GitHub, no README
- [ ] IDE aberta, com os arquivos do §5 **já abertos em abas**, na ordem em que serão mostrados
- [ ] Terminal visível para o log de boot

### Gravação

- [ ] Resolução 1920x1080; **zoom da IDE aumentado** — código ilegível é código que não conta
- [ ] Áudio testado (30 segundos gravados e ouvidos)
- [ ] Notificações do sistema desligadas
- [ ] Nada de credencial na tela: o `.env` **não** deve aparecer aberto

---

## 4. O roteiro, bloco a bloco

As falas são sugestões — diga com as suas palavras. O que importa é não esquecer o conteúdo.

---

### Bloco 0 — Abertura (0:00 → 0:35)

**Tela:** README no GitHub.

> "Olá, meu nome é João Guilherme, RM566234, e este é o PetHub, entrega de Java Advanced da
> Sprint 3. O PetHub é uma plataforma de cuidado veterinário, e este repositório é a API do
> domínio clínico: clínicas, veterinários, pets, consultas, exames, diagnósticos, vacinas e as
> leituras do coleiro IoT. Java 21, Spring Boot e Oracle. Nos próximos minutos eu mostro os quatro
> itens da entrega: o frontend, o Flyway, o Spring Security com dois perfis, e dois fluxos
> completos de negócio."

**Por que este bloco existe:** dá o mapa. Quem sabe o que vai ver avalia melhor o que vê. Dizer os
quatro itens logo no começo é o enquadramento que protege os 15 pontos de "apresentação coerente
com a proposta".

---

### Bloco 1 — Arquitetura e Flyway (0:35 → 1:50)

**Tela:** IDE, árvore de pastas → depois o terminal com o boot.

Mostre a árvore `src/main/java/fiap/pethub/` e percorra as camadas **rápido**, uma frase cada:

> "A organização segue camadas. O `controller` recebe a requisição e não tem regra de negócio; o
> `service` é onde a regra mora; o `repository` fala com o banco; o `mapper` converte entidade em
> DTO via MapStruct; `entity` são as treze tabelas; `security` concentra autenticação e posse; e
> `exception` tem o handler global, que é o único lugar que transforma erro em resposta HTTP. São
> cerca de 7.500 linhas de Java, e a razão de cada classe ser pequena é deliberada — a separação
> aqui é decisão de projeto, não decoração."

Agora **Flyway — diga o nome do item**:

> "Segundo item da entrega: Flyway, vinte pontos."

1. Abra `src/main/resources/db/migration/V1__schema_inicial.sql` e role: 13 sequences, 13 tabelas,
   20 chaves estrangeiras
2. Abra `application.properties` e aponte com o cursor:
   - `spring.jpa.hibernate.ddl-auto=validate` — **este é o ponto**
   - `spring.flyway.baseline-on-migrate=true`
3. Suba a aplicação e mostre o log do Flyway no terminal

> "O detalhe que importa aqui é o `ddl-auto=validate`. Nas sprints anteriores estava `update`, e
> com `update` o Hibernate cria e altera tabela por conta própria — ele e o Flyway disputariam o
> schema. Com `validate`, o Hibernate só **confere** se as entidades batem com o que o Flyway
> aplicou. Se divergirem, a aplicação não sobe, e é exatamente esse o objetivo: a divergência
> aparece no boot, e não em produção. O `baseline-on-migrate` está aí porque o schema da FIAP já
> existia antes de o Flyway entrar, criado pelo `update` das sprints anteriores."

**Por que este bloco existe:** Flyway vale 20 pontos e é invisível numa demonstração de tela. Se
você não mostrar de propósito, ninguém vê. É também o bloco mais árido, e por isso vem cedo,
enquanto ainda há atenção.

---

### Bloco 2 — Cadastro e login do tutor (1:50 → 2:40)

**Tela:** navegador, `http://localhost:8080/`.

> "Terceiro item: Spring Security, trinta pontos, com dois tipos de usuário."

1. Digite só `localhost:8080` — mostre que a raiz redireciona para o login em vez de dar 404
2. `/registrar/tutor` → cadastre um tutor ao vivo (rápido; os dados já pensados)
3. **Deixe um campo inválido de propósito** — e-mail sem `@`, ou senha curta — e mostre a mensagem
   voltando presa ao campo
4. Corrija, salve, faça login, caia no dashboard do tutor

> "A validação acontece nos dois lados: o HTML tem `required` e `maxlength`, e o servidor valida de
> novo com Bean Validation. A do cliente é conveniência; a que vale é a do servidor, porque
> qualquer um desliga o JavaScript. A senha nunca é gravada como texto — vai para o banco em
> BCrypt. E repare que, ao logar, eu não escolhi para onde ir: quem decide a página inicial de cada
> perfil é uma classe só, a `PaginaInicial`, usada tanto aqui quanto na rota raiz. Se as duas
> discordassem, um perfil cairia na tela do outro."

---

### Bloco 3 — Tutor cadastra um pet (2:40 → 3:30)

1. "Cadastrar novo pet" → preencha → salve → o pet aparece no dashboard
2. Abra os detalhes do pet **que já tem histórico** (o semeado): consultas, diagnósticos e pedidos

> "O formulário do pet é o mesmo ao cadastrar e ao editar, e ele existe uma vez só: é um fragmento
> Thymeleaf, incluído nas duas telas. Antes eram duas cópias que precisavam ser mantidas idênticas
> na mão — acrescentar um campo significava lembrar de mexer em dois arquivos."

Aponte para `templates/fragments/campos.html` por **10 segundos**.

> "E um detalhe de segurança que não aparece na tela: o formulário não pergunta de quem é o pet. O
> dono é sempre o tutor autenticado. Se o CPF do dono viesse do formulário, um tutor poderia
> cadastrar um pet no nome de outro."

---

### Bloco 4 — Troca de perfil (3:30 → 4:05)

**Tela:** janela anônima.

1. Login como **veterinário**
2. Dashboard do vet: a agenda de consultas — visivelmente diferente do dashboard do tutor

> "Aqui está o segundo tipo de usuário. Mesma aplicação, mesmo login, telas diferentes — e a
> separação não é só visual."

Abra `config/SecurityConfig.java` e mostre as **duas cadeias de filtro**:

> "As regras de acesso estão num arquivo só, e em duas cadeias. A primeira, para `/api/**`, é
> stateless e autentica por token JWT — é a que o Swagger usa. A segunda é a do site, com login por
> formulário e sessão. Elas são separadas porque uma API e um site falham de formas diferentes: a
> API responde 401 em JSON, o site manda para a tela de login. Juntar as duas significaria um dos
> dois lados se comportando errado. E o motivo de tudo estar neste arquivo, em vez de espalhado em
> anotações pelos onze controllers, é que a pergunta 'quem pode fazer o quê' precisa ter uma
> resposta que se lê de cima a baixo."

Aponte as linhas: `ROTAS_CLINICAS` no GET aceita os dois perfis; nos demais métodos, só o vet.

> "A regra segue o domínio: o veterinário produz o prontuário, o responsável consulta."

---

### Bloco 5 — Fluxo 1: agendamento com agenda protegida (4:05 → 5:20)

> "Quarto item: fluxos completos, que não podem ser CRUD. Este é o primeiro."

1. "Nova consulta" → escolha o pet do Tutor A → **use o horário que já está ocupado**
2. Salve → a tela volta com o erro preso ao campo de data:
   *"Este veterinário já tem uma consulta marcada neste horário."*
3. Corrija o horário → salve → a consulta entra na agenda

> "Não é só um insert. Antes de gravar, o serviço verifica se este veterinário já tem consulta
> naquele horário. Sem essa checagem, a agenda aceita sobreposição e o conflito só aparece no dia
> da consulta, com dois tutores na recepção. Repare também que o erro voltou **preso ao campo** que
> precisa ser corrigido, e não como uma mensagem solta no topo da tela."

4. Agora a parte boa — **volte para a aba do tutor** e recarregue o dashboard: o contador de
   lembretes subiu
5. Abra "Lembretes"

> "E aqui está a segunda metade do fluxo: ninguém pediu este lembrete. Agendar a consulta criou o
> lembrete para o tutor automaticamente, na data do atendimento. O tutor não tem nenhum botão de
> 'criar lembrete' — eles nascem do que o veterinário faz."

Abra `service/ConsultaService.java`, método `create`, por 10 segundos: valida agenda → salva →
notifica. São três linhas, e é o que torna o fluxo legível.

---

### Bloco 6 — Fluxo 2: fechar o prontuário (5:20 → 6:35)

**Tela:** janela do vet, consulta recém-criada.

1. Abra a consulta → o formulário tem duas seções: **1. Diagnóstico** e **2. Pedido médico (opcional)**
2. **Provoque a validação cruzada primeiro**: escolha o tipo do pedido e **deixe a descrição
   vazia** → salve → *"Para solicitar um exame, preencha o tipo e a descrição."*
3. Complete e salve → volta para a agenda com a consulta agora **REALIZADA**
4. **Abra a mesma consulta de novo** → o formulário sumiu, com o aviso de que o prontuário já foi
   registrado

> "Este é o fluxo mais interessante do sistema. Fechar uma consulta são três escritas que só fazem
> sentido juntas: grava o diagnóstico, emite o pedido médico **se** houver um, e dá a consulta por
> realizada. As três estão na mesma transação, num serviço próprio, o `ProntuarioService` — ou
> entram as três, ou não entra nenhuma. Isso já esteve no controller, e lá uma falha no meio
> deixava o diagnóstico gravado e a consulta ainda em aberto."

> "E o formulário some depois de salvo porque o serviço recusa um segundo prontuário na mesma
> consulta. Sem essa guarda, um F5 depois de salvar empilharia um segundo diagnóstico."

Abra `service/ProntuarioService.java` — 20 segundos, é o trecho mais forte do vídeo. Mostre
`registrar` e `recusarSeJaFechada`.

---

### Bloco 7 — O ciclo fecha no tutor (6:35 → 7:10)

**Tela:** aba do tutor.

1. Abra os detalhes do pet → o diagnóstico e o pedido médico que o veterinário acabou de emitir
   **já estão lá**
2. Abra "Lembretes" → agora há também o lembrete do pedido médico

> "O tutor não fez nada, e o histórico do pet dele mudou. É o mesmo banco e os mesmos registros,
> vistos pelos dois lados da aplicação. O tutor lê o prontuário; ele não o edita."

**Por que este bloco existe:** é o momento em que o sistema deixa de parecer duas telas separadas e
passa a parecer um sistema. Convence mais que qualquer explicação de arquitetura.

---

### Bloco 8 — Proteção de rotas e de registros (7:10 → 8:00)

Ainda logado como **tutor**:

1. Digite `/vet/dashboard` na barra de endereços → **403**, não a tela do vet
2. No **Swagger**, sem o Authorize, chame qualquer endpoint → **401**, com a mensagem em JSON
   *"Autenticação necessária"*
3. Autorize com o **token do tutor** e tente `POST /api/consultas` → **403**
   *"Seu perfil não tem permissão para esta operação"*
4. Ainda com o token do tutor, faça `GET /api/pets/{id do pet do Tutor B}` → **404**

O item 4 é o mais interessante do bloco. Pare nele:

> "Repare que este último não deu 403, deu 404. É de propósito. Um 403 confirmaria que o id existe,
> e permitiria a um tutor varrer os ids do banco descobrindo quantos pets a clínica tem. A mensagem
> é deliberadamente idêntica à de recurso inexistente: a API não confirma nem nega a existência de
> um recurso alheio."

Abra `security/EscopoDoUsuario.java`:

> "Isso é a segunda camada de proteção. A primeira é por rota, no `SecurityConfig`, e responde à
> pergunta 'este perfil pode chamar esta rota'. Esta aqui responde a outra: 'este usuário pode ver
> **este registro**'. Ela é o único ponto do sistema que lê o contexto do Spring Security — os
> serviços dependem dela, e não do framework, o que deixa a regra de posse testável sem subir
> contexto web."

Se houver fôlego, o método `chaveDeCache()` — 15 segundos, e é o detalhe que mais impressiona:

> "E uma consequência que não é óbvia: a chave do cache inclui o escopo do usuário. Sem isso, um
> pet carregado por um tutor seria devolvido a outro num acerto de cache, porque o `@Cacheable`
> curto-circuita o método e a verificação de posse nunca rodaria. O cache furaria a segurança
> justamente quando estivesse funcionando bem."

---

### Bloco 9 — O que a tela não mostra: wearable (8:00 → 8:45)

**Tela:** Swagger, autorizado como **veterinário** (gravar leitura é rota de escrita: perfil VET).

> "Nem toda funcionalidade tem tela, e esta é uma delas: as leituras do coleiro IoT chegam por API,
> vindas do dispositivo, não de um formulário."

`POST /api/leituras-wearable` com o corpo:

```json
{
  "petId": 1,
  "timestamp": "2026-09-12T20:30:00",
  "consumoMlRegistrado": 40,
  "metaDiariaML": 300
}
```

Ajuste `petId` e a data. Mostre a resposta: `percentualMeta` em torno de 13, `alertaGerado: true`,
`tipoAlerta: "DESIDRATACAO_CRITICA"`.

> "A leitura chegou com 40ml contra uma meta de 300, às oito e meia da noite. O sistema calculou o
> percentual, viu que é fim do dia e classificou como desidratação crítica. O horário importa: 13%
> da meta às nove da manhã é normal; às oito e meia da noite é risco. E todo dia às 21h uma rotina
> agendada apura o consumo de cada pet e cria o lembrete para o tutor — é o mesmo tipo de lembrete
> que vimos na tela dele, só que nascido de um sensor em vez de uma consulta."

Feche com `GET /api/leituras-wearable?apenasAlertas=true`.

> "E esta é a razão de eu estar mostrando esta parte no Swagger: a aplicação web tem tela para o
> que tutor e veterinário usam no dia a dia, e a API cobre o que é máquina falando com máquina."

**Atenção ao gravar:** o consumo é **acumulado** por dia. Se você repetir o POST, o percentual sobe
e o alerta muda de classificação. Faça uma vez só, ou refaça o cálculo antes de narrar.

---

### Bloco 10 — Fechamento (8:45 → 9:15)

1. Terminal: `./mvnw test` verde (ou o resultado já rodado, na tela)
2. Volte ao README no GitHub e role rápido pelo sumário

> "Para fechar: 46 testes automatizados, cobrindo a regra de posse, o agendamento, o prontuário e a
> emissão de token. O repositório é público, e o README tem instalação, execução, a tabela de
> permissões dos dois perfis, os fluxos e os prints de todas as chamadas da API. Recapitulando os
> quatro itens: frontend em Thymeleaf com as telas dos dois perfis; Flyway versionando o schema com
> `ddl-auto=validate`; Spring Security com dois perfis, proteção por rota e por posse do registro;
> e os fluxos de agendamento e de fechamento de prontuário, que não são CRUD. Obrigado."

---

## 5. Trechos de código a mostrar — resumo

Deixe estes arquivos **abertos em abas da IDE**, nesta ordem. Nenhum deve ficar mais de 20 segundos
na tela.

| # | Arquivo | O que apontar | Bloco |
|---|---|---|---|
| 1 | `db/migration/V1__schema_inicial.sql` | 13 tabelas, 20 FKs | 1 |
| 2 | `application.properties` | `ddl-auto=validate`, `baseline-on-migrate` | 1 |
| 3 | `security/PaginaInicial.java` | regra única da página inicial de cada perfil | 2 |
| 4 | `templates/fragments/campos.html` | fragmento reaproveitado nas duas telas (DRY) | 3 |
| 5 | `config/SecurityConfig.java` | as duas cadeias; `ROTAS_CLINICAS` por método HTTP | 4 |
| 6 | `service/ConsultaService.java` | `create`: valida agenda → salva → notifica | 5 |
| 7 | `service/ProntuarioService.java` | `registrar` e `recusarSeJaFechada` | 6 |
| 8 | `security/EscopoDoUsuario.java` | `exigirPosse` e `chaveDeCache` | 8 |

Se precisar cortar tempo, os dois que **não** podem sair são o **7** e o **8**: são as duas
respostas mais prováveis da avaliação oral.

---

## 6. Perguntas prováveis na avaliação oral

O que você afirmar no vídeo vira pergunta em sala. As respostas abaixo saem do próprio código —
confira cada uma antes de gravar e **não fale de nada que você não consiga sustentar**.

**"Por que duas cadeias de filtro no Spring Security?"**
Porque API e site falham de formas diferentes. A API é stateless, autentica por JWT e responde 401
em JSON; o site tem sessão, login por formulário e redireciona para a tela de login. Uma cadeia só
obrigaria um dos dois a se comportar errado.

**"Por que 404 e não 403 quando o recurso é de outro usuário?"**
Porque 403 confirma que o id existe. Com 403, um tutor autenticado consegue varrer ids e descobrir
o tamanho da base. A mensagem é igual à de recurso inexistente de propósito.

**"Por que o `ProntuarioService` existe, se já há `DiagnosticoService` e `PedidoMedicoService`?"**
Porque fechar uma consulta é um fluxo, não um CRUD: três escritas que só fazem sentido juntas, na
mesma transação. Isso estava no controller, e lá uma falha no meio deixava o diagnóstico gravado
com a consulta ainda em aberto.

**"O que o `@Transactional` está protegendo ali, exatamente?"**
Se o pedido médico falhar depois de o diagnóstico ter sido gravado, a transação desfaz o
diagnóstico também. Sem ela, a consulta ficaria com diagnóstico e sem o exame que o veterinário
pediu, e o status não refletiria nenhum dos dois.

**"Por que a chave do cache inclui o usuário?"**
Porque o `@Cacheable` curto-circuita o método: num acerto de cache, a verificação de posse não
roda. Sem o escopo na chave, o registro carregado por um tutor seria servido a outro.

**"Por que `ddl-auto=validate` e não `update`?"**
Com `update`, Hibernate e Flyway disputam o schema e o resultado é imprevisível. Com `validate`, o
Flyway é o dono e o Hibernate só confere. Divergência derruba o boot, que é onde se quer descobrir.

**"Por que o pool do banco está em 2?"**
O pool da FIAP é limitado, então fica no mínimo viável — mas 1 não basta: no boot o Flyway segura
uma conexão para aplicar a migration e pede outra para gravar o histórico. Com 1, ele espera os 30
segundos do timeout e a aplicação não sobe.

**"Onde você usou IA e o que revisou?"**
Responda com honestidade e com especificidade — é pergunta obrigatória da disciplina, e a resposta
vaga é pior que a resposta afirmativa. Diga o que pediu, o que aceitou, o que reescreveu e como
conferiu.

---

## 7. O que não fazer

- **Não passe endpoint por endpoint no Swagger.** O enunciado pede as principais funcionalidades;
  os prints exaustivos já estão no README
- **Não improvise um caminho não ensaiado.** Uma tela que quebra ao vivo é -5, e o vídeo inteiro
  fica com cara de não testado
- **Não leia código linha a linha.** Aponte o trecho, diga o porquê em uma frase, siga
- **Não explique o que você não consegue defender na oral.** Se um trecho não é seu de verdade, ele
  não deve virar o destaque do vídeo
- **Não deixe o `.env`, o token inteiro ou a senha do Oracle aparecerem na tela**
- **Não peça desculpa por nada no vídeo.** "Isso aqui ficou meio feio" planta uma dúvida que não
  existiria

---

## 8. Plano B

- **O Oracle da FIAP cai ou fica lento:** não tente gravar assim. Grave em outro horário; se não
  der, tenha um `DB_URL` alternativo apontando para um Oracle local em container
- **Um bloco quebra na gravação:** corte e regrave só aquele bloco. O roteiro é modular de
  propósito — os blocos não dependem de continuidade de tela, só de dados
- **Passou de 10 minutos:** corte nesta ordem — bloco 9 (wearable) para 25s, depois o bloco 3 (pet)
  para 30s, depois o tour de pastas do bloco 1. **Nunca corte** os blocos 1 (Flyway), 5 e 6
  (fluxos) e 8 (segurança): são 70 dos 100 pontos

---

## 9. Checklist final, antes de publicar

- [ ] Duração ≤ 10 minutos
- [ ] Os quatro itens da rubrica foram **nomeados em voz alta**
- [ ] Os dois perfis apareceram, com telas diferentes
- [ ] Pelo menos dois fluxos não-CRUD demonstrados de ponta a ponta
- [ ] Pelo menos uma validação **recusando** entrada inválida
- [ ] Pelo menos uma proteção de rota sendo negada
- [ ] Flyway apareceu: migration, `validate` e log do boot
- [ ] Nenhuma credencial visível em nenhum quadro
- [ ] Áudio audível do começo ao fim
- [ ] Vídeo assistido inteiro, uma vez, antes de enviar
- [ ] Repositório **público** e README atualizado
